package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.FolderEntity
import com.example.data.PriceListDatabase
import com.example.data.PriceListRepository
import com.example.data.ProductEntity
import com.example.data.BinFolder
import com.example.data.BinProduct
import com.example.ui.translation.AppLanguage
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import org.json.JSONArray

enum class Screen {
    LOGIN,
    FOLDERS,
    PRODUCTS,
    TRASH
}

enum class ViewMode {
    CARD,
    TABLE
}

enum class ProductSortOption {
    NAME_ASC,
    NAME_DESC,
    VALUE_HIGH_TO_LOW,
    VALUE_LOW_TO_HIGH,
    DATE_PURCHASED_NEWEST,
    DATE_PURCHASED_OLDEST,
    DATE_ADDED_NEWEST,
    DATE_ADDED_OLDEST
}

data class ExtractedProduct(
    val id: Long = System.nanoTime(),
    var name: String,
    var description: String,
    var costPrice: Double,
    var sellingPrice: Double,
    var wholesalePrice: Double,
    var boughtFrom: String,
    var priceUnit: String = "piece"
)

class PriceListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application.applicationContext,
        PriceListDatabase::class.java,
        "price_list_database"
    ).fallbackToDestructiveMigration().build()

    val repository = PriceListRepository(db.priceListDao)

    // Language state
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    // Navigation and screen states
    var currentScreen by mutableStateOf(Screen.FOLDERS)

    // Backing flows for combining
    private val selectedFolderState = MutableStateFlow<FolderEntity?>(null)
    private val searchQueryState = MutableStateFlow("")

    private var _selectedFolder by mutableStateOf<FolderEntity?>(null)
    var selectedFolder: FolderEntity?
        get() = _selectedFolder
        set(value) {
            _selectedFolder = value
            selectedFolderState.value = value
        }

    private var _searchQuery by mutableStateOf("")
    var searchQuery: String
        get() = _searchQuery
        set(value) {
            _searchQuery = value
            searchQueryState.value = value
        }

    private var _isSearchExpanded by mutableStateOf(false)
    var isSearchExpanded: Boolean
        get() = _isSearchExpanded
        set(value) {
            _isSearchExpanded = value
            if (!value) {
                searchQuery = "" // Clear search queries on close
            }
        }

    // View layout option (Card vs Spreadsheet row/columns)
    var viewMode by mutableStateOf(ViewMode.TABLE)

    // Product Sorting States
    private val sortOptionState = MutableStateFlow(ProductSortOption.DATE_ADDED_NEWEST)
    var appProductSortOption by mutableStateOf(ProductSortOption.DATE_ADDED_NEWEST)
        private set

    // Passcode lock security
    var isLoggedIn by mutableStateOf(true)
    var enteredPasscode by mutableStateOf("")
    var loginError by mutableStateOf(false)
    var savedPasscode by mutableStateOf("1234") // Default gate

    // Cloud & Multi-Device Sync properties
    var loginMethod by mutableStateOf("pin") // "pin" or "cloud"
    var cloudUsername by mutableStateOf("")
    var cloudShopId by mutableStateOf("")
    var isCloudSyncing by mutableStateOf(false)
    var cloudSyncMessage by mutableStateOf("")
    var lastLocalWriteTime by mutableStateOf(0L)

    var linkedDevices by mutableStateOf<List<String>>(emptyList())

    // Phone Authentication state variables
    var userPhoneNumber by mutableStateOf("")
    var isPhoneConnected by mutableStateOf(false)

    // Phone login wizard states
    var phoneLoginStep by mutableStateOf(1) // 1 = Phone Number, 2 = OTP, 3 = Store Name
    var enteredPhoneInput by mutableStateOf("")
    var enteredOtpInput by mutableStateOf("")
    var enteredStoreNameInput by mutableStateOf("")
    var generatedPhoneOtp by mutableStateOf("")
    var phoneLoginError by mutableStateOf("")
    var isSendingSmsOtp by mutableStateOf(false)
    var showIncomingSmsNotification by mutableStateOf(false)

    // Active voice compilation field target selection (e.g. "name", "price")
    var activeVoiceTargetField by mutableStateOf("name")

    // AI Bill/Invoice Scanner Properties
    var isScanningInvoice by mutableStateOf(false)
    var scannerErrorMessage by mutableStateOf<String?>(null)
    var scannedInvoiceText by mutableStateOf("")
    var scannedInvoiceSupplier by mutableStateOf("")
    var extractedProducts by mutableStateOf<List<ExtractedProduct>>(emptyList())

    // UI Flows
    val foldersFlow: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProductsFlow: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val binFoldersFlow: StateFlow<List<BinFolder>> = repository.allBinFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val binProductsFlow: StateFlow<List<BinProduct>> = repository.allBinProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products list state filtered locally/globally (including subfolders recursively)
    val filteredProductsFlow: Flow<List<ProductEntity>> = combine(
        selectedFolderState,
        searchQueryState,
        allProductsFlow,
        foldersFlow,
        sortOptionState
    ) { folder, query, allProducts, folders, sortOption ->
        val folderProducts = if (folder != null) {
            val nestedFolderIds = getDescendantFolderIds(folder.id, folders)
            allProducts.filter { it.folderId in nestedFolderIds }
        } else {
            allProducts
        }
        val filtered = if (query.isBlank()) {
            folderProducts
        } else {
            folderProducts.filter { isProductMatch(it, query) }
        }
        
        when (sortOption) {
            ProductSortOption.NAME_ASC -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            ProductSortOption.NAME_DESC -> filtered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            ProductSortOption.VALUE_HIGH_TO_LOW -> filtered.sortedByDescending { it.sellingPrice }
            ProductSortOption.VALUE_LOW_TO_HIGH -> filtered.sortedBy { it.sellingPrice }
            ProductSortOption.DATE_PURCHASED_NEWEST -> filtered.sortedByDescending { parseDateString(it.boughtFrom) }
            ProductSortOption.DATE_PURCHASED_OLDEST -> filtered.sortedBy { parseDateString(it.boughtFrom) }
            ProductSortOption.DATE_ADDED_NEWEST -> filtered.sortedByDescending { it.createdAt }
            ProductSortOption.DATE_ADDED_OLDEST -> filtered.sortedBy { it.createdAt }
        }
    }

    private fun parseDateString(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        try {
            val clean = dateStr.trim()
            // YYYY-MM-DD
            val ymdPattern = java.util.regex.Pattern.compile("^(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})")
            val ymdMatcher = ymdPattern.matcher(clean)
            if (ymdMatcher.find()) {
                val year = ymdMatcher.group(1).toInt()
                val month = ymdMatcher.group(2).toInt() - 1
                val day = ymdMatcher.group(3).toInt()
                val cal = java.util.Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                return cal.timeInMillis
            }

            // DD-MM-YYYY
            val dmyPattern = java.util.regex.Pattern.compile("^(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})")
            val dmyMatcher = dmyPattern.matcher(clean)
            if (dmyMatcher.find()) {
                val day = dmyMatcher.group(1).toInt()
                val month = dmyMatcher.group(2).toInt() - 1
                val year = dmyMatcher.group(3).toInt()
                val cal = java.util.Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                return cal.timeInMillis
            }
        } catch (e: Exception) {
            // ignore
        }
        return 0L
    }

    private fun getDescendantFolderIds(folderId: Long, folders: List<FolderEntity>): Set<Long> {
        val result = mutableSetOf(folderId)
        val queue = ArrayDeque<Long>()
        queue.add(folderId)
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val children = folders.filter { it.parentId == currentId }.map { it.id }
            for (childId in children) {
                if (result.add(childId)) {
                    queue.add(childId)
                }
            }
        }
        return result
    }

    fun isProductMatch(product: ProductEntity, query: String): Boolean {
        if (query.isBlank()) return true
        val queryTerms = query.lowercase().trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (queryTerms.isEmpty()) return true

        val prodName = product.name.lowercase()
        val prodDesc = product.description.lowercase()
        val prodSupplier = product.boughtFrom.lowercase()

        return queryTerms.all { term ->
            if (prodName.contains(term) || prodDesc.contains(term) || prodSupplier.contains(term)) {
                return@all true
            }
            if (term.length >= 2) {
                val words = (prodName.split("[\\s_\\-\\.\\,\\(\\)]+".toRegex()) + 
                             prodDesc.split("[\\s_\\-\\.\\,\\(\\)]+".toRegex()) + 
                             prodSupplier.split("[\\s_\\-\\.\\,\\(\\)]+".toRegex())).filter { it.length >= 2 }
                val hasFuzzyMatch = words.any { word ->
                    val dist = levenshteinDistance(word, term)
                    val maxAllowedDist = if (term.length > 5) 2 else 1
                    dist <= maxAllowedDist
                }
                if (hasFuzzyMatch) return@all true
            }
            false
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                if (s1[i - 1] == s2[j - 1]) {
                    dp[j] = prev
                } else {
                    dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + 1)
                }
                prev = temp
            }
        }
        return dp[s2.length]
    }

    // Input form states for adding/editing a product
    var productNameInput by mutableStateOf("")
    var productDescriptionInput by mutableStateOf("")
    var productCostPriceInput by mutableStateOf("")
    var productSellingPriceInput by mutableStateOf("")
    var productWholesalePriceInput by mutableStateOf("")
    var productSupplierInput by mutableStateOf("")
    var productPriceUnitInput by mutableStateOf("piece")

    // Adding/editing categories folder inputs
    var folderNameInput by mutableStateOf("")

    // Modal Control States
    var showFolderDialog by mutableStateOf(false)
    var folderParentId by mutableStateOf<Long?>(null)
    var editingFolder by mutableStateOf<FolderEntity?>(null)
    var showFolderOptionsFor by mutableStateOf<FolderEntity?>(null)
    var showProductDialog by mutableStateOf(false)
    var showSuccessDialog by mutableStateOf(false)
    var successDialogMessage by mutableStateOf("")
    var editingProduct by mutableStateOf<ProductEntity?>(null)
    var activeProductForOptions by mutableStateOf<ProductEntity?>(null)
    var showDeleteFolderConfirm by mutableStateOf<FolderEntity?>(null)
    var showDeleteProductConfirm by mutableStateOf<ProductEntity?>(null)

    // Voice typing feedback
    var voiceError by mutableStateOf<String?>(null)
    var isVoiceListening by mutableStateOf(false)
    var showWelcomeUser by mutableStateOf(false)

    // --- CUSTOMIZED SETTINGS STATES ---
    var isBinAutoDeleteEnabled by mutableStateOf(true)
    var isAudioGuideEnabled by mutableStateOf(true)
    var showCostAndMargins by mutableStateOf(true)
    var isFolderAutoNumberingEnabled by mutableStateOf(true)
    var showSupplierInfo by mutableStateOf(true)
    var defaultTaxRate by mutableStateOf(0f)
    var customShopName by mutableStateOf("")
    var customShopTagline by mutableStateOf("")
    var appThemeMode by mutableStateOf("SYSTEM") // SYSTEM, LIGHT, DARK
    var appThemePalette by mutableStateOf("SAGE") // SAGE, BLUE, CRIMSON, TEAL, GOLDEN
    var fontSizeScale by mutableStateOf(1.0f) // 0.85f (Small), 1.0f (Normal), 1.15f (Large), 1.3f (Extra Large)
    var appVersionName by mutableStateOf("1.4.2")

    // --- CUSTOM FIREBASE CONFIG STATES ---
    var firebaseApiKeyInput by mutableStateOf("")
    var firebaseProjectIdInput by mutableStateOf("")
    var firebaseAppIdInput by mutableStateOf("")
    var showFirebaseConfigDialog by mutableStateOf(false)

    init {
        // Retrieve language or lock presets
        val prefs = application.getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("selected_lang", "ENGLISH") ?: "ENGLISH"
        currentLanguage = if (savedLang == "HINDI") AppLanguage.HINDI else AppLanguage.ENGLISH
        savedPasscode = prefs.getString("security_passcode", "1234") ?: "1234"
        
        loginMethod = prefs.getString("login_method", "pin") ?: "pin"
        cloudUsername = prefs.getString("cloud_username", "") ?: ""
        cloudShopId = prefs.getString("cloud_shop_id", "") ?: ""
        
        userPhoneNumber = prefs.getString("user_phone_number", "") ?: ""
        isPhoneConnected = prefs.getBoolean("is_phone_connected", false)

        // Retrieve custom Firebase settings
        firebaseApiKeyInput = prefs.getString("firebase_api_key", "") ?: ""
        firebaseProjectIdInput = prefs.getString("firebase_project_id", "") ?: ""
        firebaseAppIdInput = prefs.getString("firebase_app_id", "") ?: ""

        // Retrieve customized settings
        showCostAndMargins = prefs.getBoolean("settings_show_cost_margins", true)
        isFolderAutoNumberingEnabled = prefs.getBoolean("settings_folder_auto_numbering", true)
        showSupplierInfo = prefs.getBoolean("settings_show_supplier", true)
        defaultTaxRate = prefs.getFloat("settings_tax_rate", 0f)
        customShopName = prefs.getString("settings_shop_name", "") ?: ""
        customShopTagline = prefs.getString("settings_shop_tagline", "") ?: ""
        appThemeMode = prefs.getString("settings_theme_mode", "SYSTEM") ?: "SYSTEM"
        appThemePalette = prefs.getString("settings_theme_palette", "SAGE") ?: "SAGE"
        fontSizeScale = prefs.getFloat("settings_font_size_scale", 1.0f)
        appVersionName = prefs.getString("settings_app_version_name", "1.4.2") ?: "1.4.2"
        isBinAutoDeleteEnabled = prefs.getBoolean("settings_bin_auto_delete", true)
        if (isBinAutoDeleteEnabled) {
            checkAndPerformBinAutoDelete()
        }

        val savedSort = prefs.getString("settings_product_sort_option", ProductSortOption.DATE_ADDED_NEWEST.name) ?: ProductSortOption.DATE_ADDED_NEWEST.name
        val loadedSort = try { ProductSortOption.valueOf(savedSort) } catch(e: Exception) { ProductSortOption.DATE_ADDED_NEWEST }
        appProductSortOption = loadedSort
        sortOptionState.value = loadedSort

        val savedDevicesStr = prefs.getString("settings_linked_devices", "") ?: ""
        linkedDevices = if (savedDevicesStr.isBlank()) {
            emptyList()
        } else {
            savedDevicesStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
        
        val isProfileSet = prefs.getBoolean("is_profile_created", false) || (customShopName.isNotBlank() && cloudUsername.isNotBlank())
        if (isProfileSet) {
            isLoggedIn = true
            currentScreen = Screen.FOLDERS
            syncFromCloud()
        } else {
            isLoggedIn = false
            currentScreen = Screen.LOGIN
        }

        // Periodic cloud auto-scanning and sync (every 15 seconds) to keep multi-devices mirrored
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000)
                if (cloudShopId.isNotBlank() && isLoggedIn) {
                    val timeSinceLocalWrite = System.currentTimeMillis() - lastLocalWriteTime
                    if (timeSinceLocalWrite > 25_000) {
                        syncFromCloud()
                    }
                }
            }
        }
    }

    fun saveCloudCredentials() {
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("login_method", loginMethod)
            putString("cloud_username", cloudUsername)
            putString("cloud_shop_id", cloudShopId)
            apply()
        }
    }

    fun savePhoneCredentials() {
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_phone_number", userPhoneNumber)
            putBoolean("is_phone_connected", isPhoneConnected)
            putString("settings_shop_name", customShopName)
            apply()
        }
    }

    fun handleProfileCreation(ownerName: String, storeName: String) {
        userPhoneNumber = "Owner Account"
        isPhoneConnected = true
        customShopName = storeName
        cloudUsername = ownerName
        
        loginMethod = "cloud"
        val cleanName = storeName.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        val randomSuffix = (1000..9999).random().toString()
        cloudShopId = "s_${cleanName}_$randomSuffix"
        
        saveCloudCredentials()
        savePhoneCredentials()
        
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_profile_created", true).apply()
        
        isLoggedIn = true
        currentScreen = Screen.FOLDERS
        showWelcomeUser = true
        
        syncFromCloud()
    }

    private suspend fun sendRealSmsViaApi(phoneNumber: String, code: String): Pair<Boolean, String> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val client = OkHttpClient()
            val language = currentLanguage

            val twilioSid = try { BuildConfig.TWILIO_ACCOUNT_SID } catch (e: Exception) { "" }
            val twilioToken = try { BuildConfig.TWILIO_AUTH_TOKEN } catch (e: Exception) { "" }
            val twilioPhone = try { BuildConfig.TWILIO_PHONE_NUMBER } catch (e: Exception) { "" }

            val hasTwilio = twilioSid.isNotBlank() && twilioSid != "YOUR_TWILIO_ACCOUNT_SID" &&
                            twilioToken.isNotBlank() && twilioToken != "YOUR_TWILIO_AUTH_TOKEN" &&
                            twilioPhone.isNotBlank() && twilioPhone != "YOUR_TWILIO_PHONE_NUMBER"

            val msg = if (language == AppLanguage.HINDI) {
                "आपकी कीमत सूची सत्यापन ओटीपी है: $code"
            } else {
                "Your Price List Verification OTP is: $code"
            }

            if (hasTwilio) {
                val twilioUrl = "https://api.twilio.com/2010-04-01/Accounts/$twilioSid/Messages.json"
                val basicAuth = okhttp3.Credentials.basic(twilioSid, twilioToken)
                
                var formattedPhone = phoneNumber.trim()
                if (!formattedPhone.startsWith("+")) {
                    if (formattedPhone.length == 10) {
                        formattedPhone = "+91$formattedPhone"
                    } else {
                        formattedPhone = "+$formattedPhone"
                    }
                }

                val formBody = okhttp3.FormBody.Builder()
                    .add("To", formattedPhone)
                    .add("From", twilioPhone)
                    .add("Body", msg)
                    .build()

                val request = Request.Builder()
                    .url(twilioUrl)
                    .addHeader("Authorization", basicAuth)
                    .post(formBody)
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            Pair(true, "OTP sent successfully via Twilio!")
                        } else {
                            val errorMessage = try {
                                JSONObject(bodyStr).optString("message", "Twilio API error")
                            } catch (e: Exception) {
                                "Twilio error (Code ${resp.code})"
                            }
                            Pair(false, errorMessage)
                        }
                    }
                } catch (e: Exception) {
                    Pair(false, e.localizedMessage ?: "Twilio network error")
                }
            } else {
                val textbeltUrl = "https://textbelt.com/text"
                
                var formattedPhone = phoneNumber.trim()
                if (!formattedPhone.startsWith("+")) {
                    if (formattedPhone.length == 10) {
                        formattedPhone = "+91$formattedPhone"
                    } else {
                        formattedPhone = "+$formattedPhone"
                    }
                }

                val formBody = okhttp3.FormBody.Builder()
                    .add("phone", formattedPhone)
                    .add("message", msg)
                    .add("key", "textbelt")
                    .build()

                val request = Request.Builder()
                    .url(textbeltUrl)
                    .post(formBody)
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val json = JSONObject(bodyStr)
                            val success = json.optBoolean("success", false)
                            if (success) {
                                Pair(true, "OTP sent successfully via Textbelt!")
                            } else {
                                val errorMsg = json.optString("error", "Textbelt limit reached")
                                Pair(false, errorMsg)
                            }
                        } else {
                            Pair(false, "Textbelt server error (Code ${resp.code})")
                        }
                    }
                } catch (e: Exception) {
                    Pair(false, e.localizedMessage ?: "Textbelt network error")
                }
            }
        }
    }

    fun sendPhoneOtp(phoneNumber: String) {
        isSendingSmsOtp = true
        phoneLoginError = ""
        
        viewModelScope.launch {
            val code = (100000..999999).random().toString()
            generatedPhoneOtp = code
            
            val result = sendRealSmsViaApi(phoneNumber, code)
            val isRealSmsSent = result.first
            val apiMessage = result.second
            
            isSendingSmsOtp = false
            phoneLoginStep = 2 // Move to OTP input stage
            
            if (isRealSmsSent) {
                phoneLoginError = "" // Success, sent via real SMS gateway
            } else {
                // If SMS dispatch failed (e.g. daily quota reached or keys missing), we report why
                phoneLoginError = apiMessage
            }
            
            // Only show the floating notification banner as a helpful developer fallback
            // if the real SMS gateway failed or wasn't configured. This ensures that
            // for real numbers with API configurations, the OTP does NOT "mysteriously show up" on screen!
            showIncomingSmsNotification = !isRealSmsSent
        }
    }

    fun disconnectPhone() {
        userPhoneNumber = ""
        isPhoneConnected = false
        customShopName = ""
        
        loginMethod = "pin"
        cloudUsername = ""
        cloudShopId = ""
        
        saveCloudCredentials()
        savePhoneCredentials()
        
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_profile_created", false).apply()
        
        phoneLoginStep = 1
        enteredPhoneInput = ""
        enteredOtpInput = ""
        enteredStoreNameInput = ""
        
        isLoggedIn = false
        currentScreen = Screen.LOGIN
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_lang", currentLanguage.name).apply()
    }

    fun attemptLogin(): Boolean {
        if (enteredPasscode == savedPasscode) {
            isLoggedIn = true
            currentScreen = Screen.FOLDERS
            loginError = false
            enteredPasscode = ""
            return true
        } else {
            loginError = true
            enteredPasscode = ""
            return false
        }
    }

    fun bypassLogin() {
        isLoggedIn = true
        currentScreen = Screen.FOLDERS
    }

    fun resetPasscode(newPass: String) {
        if (newPass.length == 4) {
            savedPasscode = newPass
            val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("security_passcode", newPass).apply()
        }
    }

    fun toggleBypassSecurity(bypass: Boolean) {
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("security_bypass", bypass).apply()
        if (bypass) {
            isLoggedIn = true
        }
    }

    fun updateShowCostAndMargins(value: Boolean) {
        showCostAndMargins = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("settings_show_cost_margins", value).apply()
    }

    fun updateFolderAutoNumbering(value: Boolean) {
        isFolderAutoNumberingEnabled = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("settings_folder_auto_numbering", value).apply()
    }

    fun updateShowSupplierInfo(value: Boolean) {
        showSupplierInfo = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("settings_show_supplier", value).apply()
    }

    fun updateDefaultTaxRate(value: Float) {
        defaultTaxRate = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("settings_tax_rate", value).apply()
    }

    fun updateCustomShopName(value: String) {
        customShopName = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("settings_shop_name", value).apply()
    }

    fun updateCustomShopTagline(value: String) {
        customShopTagline = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("settings_shop_tagline", value).apply()
    }

    fun updateCloudUsername(value: String) {
        cloudUsername = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("cloud_username", value).apply()
    }

    fun updateAppThemeMode(value: String) {
        appThemeMode = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("settings_theme_mode", value).apply()
    }

    fun updateAppThemePalette(value: String) {
        appThemePalette = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("settings_theme_palette", value).apply()
    }

    fun updateFontSizeScale(value: Float) {
        fontSizeScale = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("settings_font_size_scale", value).apply()
    }

    fun updateAppVersionName(value: String) {
        appVersionName = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("settings_app_version_name", value).apply()
    }

    fun updateProductSortOption(value: ProductSortOption) {
        appProductSortOption = value
        sortOptionState.value = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("settings_product_sort_option", value.name).apply()
    }

    fun addLinkedDevice(device: String) {
        val current = linkedDevices.toMutableList()
        if (!current.contains(device)) {
            current.add(device)
            linkedDevices = current
            val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("settings_linked_devices", current.joinToString(",")).apply()
        }
    }

    fun removeLinkedDevice(device: String) {
        val current = linkedDevices.toMutableList()
        if (current.remove(device)) {
            linkedDevices = current
            val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("settings_linked_devices", current.joinToString(",")).apply()
        }
    }

    fun selectFolder(folder: FolderEntity?) {
        selectedFolder = folder
        currentScreen = if (folder != null) Screen.PRODUCTS else Screen.FOLDERS
        searchQuery = "" // Clear search queries on fold navigation
        isSearchExpanded = false
    }

    // CRUD FOLDERS
    fun createFolder() {
        if (folderNameInput.isNotBlank()) {
            viewModelScope.launch {
                lastLocalWriteTime = System.currentTimeMillis()
                val currEditing = editingFolder
                if (currEditing != null) {
                    repository.updateFolder(currEditing.copy(name = folderNameInput.trim()))
                    successDialogMessage = "Folder renamed."
                    showSuccessDialog = true
                } else {
                    repository.insertFolder(folderNameInput.trim(), folderParentId)
                }
                folderNameInput = ""
                showFolderDialog = false
                folderParentId = null
                editingFolder = null
                syncToCloudSilent()
            }
        }
    }

    fun deleteFolderCascade(folder: FolderEntity) {
        viewModelScope.launch {
            val allFoldersList = foldersFlow.value
            val allProductsList = allProductsFlow.value
            val descendantIds = getDescendantFolderIds(folder.id, allFoldersList)

            // Save folders to BinFolder
            allFoldersList.filter { it.id in descendantIds }.forEach { f ->
                repository.insertBinFolder(
                    BinFolder(
                        id = f.id,
                        name = f.name,
                        parentId = f.parentId,
                        deletedAt = System.currentTimeMillis()
                    )
                )
            }

            // Save products to BinProduct
            allProductsList.filter { it.folderId in descendantIds }.forEach { p ->
                repository.insertBinProduct(
                    BinProduct(
                        id = p.id,
                        folderId = p.folderId,
                        name = p.name,
                        description = p.description,
                        costPrice = p.costPrice,
                        sellingPrice = p.sellingPrice,
                        wholesalePrice = p.wholesalePrice,
                        boughtFrom = p.boughtFrom,
                        deletedAt = System.currentTimeMillis()
                    )
                )
            }

            repository.deleteFolder(folder)
            showDeleteFolderConfirm = null
            if (selectedFolder?.id == folder.id) {
                selectFolder(null)
            }
            syncToCloudSilent()
        }
    }

    fun restoreFolderCascade(binFolder: BinFolder) {
        viewModelScope.launch {
            val activeFolders = foldersFlow.value.toMutableList()
            val binFolders = binFoldersFlow.value
            val binProducts = binProductsFlow.value

            // 1. Resolve parentId of top folder
            val resolvedParentId = if (binFolder.parentId != null && activeFolders.any { it.id == binFolder.parentId }) {
                binFolder.parentId
            } else {
                null
            }

            val topFolder = FolderEntity(
                id = binFolder.id,
                name = binFolder.name,
                parentId = resolvedParentId
            )
            repository.restoreFolderRaw(topFolder)
            repository.deleteBinFolder(binFolder)
            activeFolders.add(topFolder)

            // 2. Recursively find child folders and products in bin
            val queue = ArrayDeque<Long>()
            queue.add(binFolder.id)

            while (queue.isNotEmpty()) {
                val currentParentId = queue.removeFirst()

                val children = binFolders.filter { it.parentId == currentParentId }
                for (childBin in children) {
                    val restoredChild = FolderEntity(
                        id = childBin.id,
                        name = childBin.name,
                        parentId = currentParentId
                    )
                    repository.restoreFolderRaw(restoredChild)
                    repository.deleteBinFolder(childBin)
                    activeFolders.add(restoredChild)
                    queue.add(childBin.id)
                }

                val products = binProducts.filter { it.folderId == currentParentId }
                for (prodBin in products) {
                    repository.restoreProductRaw(
                        ProductEntity(
                            id = prodBin.id,
                            folderId = prodBin.folderId,
                            name = prodBin.name,
                            description = prodBin.description,
                            costPrice = prodBin.costPrice,
                            sellingPrice = prodBin.sellingPrice,
                            wholesalePrice = prodBin.wholesalePrice,
                            boughtFrom = prodBin.boughtFrom
                        )
                    )
                    repository.deleteBinProduct(prodBin)
                }
            }
            syncToCloudSilent()
        }
    }

    // CRUD PRODUCTS
    fun openAddProduct() {
        editingProduct = null
        productNameInput = ""
        productDescriptionInput = ""
        productCostPriceInput = ""
        productSellingPriceInput = ""
        productWholesalePriceInput = ""
        productSupplierInput = ""
        productPriceUnitInput = "piece"
        showProductDialog = true
    }

    fun openEditProduct(product: ProductEntity) {
        editingProduct = product
        productNameInput = product.name
        productDescriptionInput = product.description
        productCostPriceInput = product.costPrice.toString()
        productSellingPriceInput = product.sellingPrice.toString()
        productWholesalePriceInput = product.wholesalePrice.toString()
        productSupplierInput = product.boughtFrom
        productPriceUnitInput = product.priceUnit
        showProductDialog = true
    }

    fun saveProduct() {
        val fId = selectedFolder?.id ?: return
        val cost = productCostPriceInput.toDoubleOrNull() ?: 0.0
        val sell = productSellingPriceInput.toDoubleOrNull() ?: 0.0
        val wholesale = productWholesalePriceInput.toDoubleOrNull() ?: 0.0
        val name = productNameInput.trim()
        val desc = productDescriptionInput.trim()
        val supplier = productSupplierInput.trim()

        if (name.isBlank()) return

        viewModelScope.launch {
            lastLocalWriteTime = System.currentTimeMillis()
            val currEditing = editingProduct
            if (currEditing != null) {
                repository.updateProduct(
                    currEditing.copy(
                        name = name,
                        description = desc,
                        costPrice = cost,
                        sellingPrice = sell,
                        wholesalePrice = wholesale,
                        boughtFrom = supplier,
                        priceUnit = productPriceUnitInput
                    )
                )
                successDialogMessage = "Item updated."
            } else {
                repository.insertProduct(
                    folderId = fId,
                    name = name,
                    description = desc,
                    costPrice = cost,
                    sellingPrice = sell,
                    wholesalePrice = wholesale,
                    boughtFrom = supplier,
                    priceUnit = productPriceUnitInput
                )
                successDialogMessage = "Item added."
            }
            showSuccessDialog = true
            showProductDialog = false
            editingProduct = null
            syncToCloudSilent()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            lastLocalWriteTime = System.currentTimeMillis()
            repository.insertBinProduct(
                BinProduct(
                    id = product.id,
                    folderId = product.folderId,
                    name = product.name,
                    description = product.description,
                    costPrice = product.costPrice,
                    sellingPrice = product.sellingPrice,
                    wholesalePrice = product.wholesalePrice,
                    boughtFrom = product.boughtFrom,
                    priceUnit = product.priceUnit,
                    deletedAt = System.currentTimeMillis()
                )
            )
            repository.deleteProduct(product)
            syncToCloudSilent()
        }
    }

    fun restoreProduct(binProduct: BinProduct) {
        viewModelScope.launch {
            val activeFolders = foldersFlow.value
            var folderId = binProduct.folderId
            val folderExists = activeFolders.any { it.id == folderId }

            if (!folderExists) {
                val binFolder = binFoldersFlow.value.firstOrNull { it.id == folderId }
                if (binFolder != null) {
                    restoreFolderCascade(binFolder)
                } else {
                    val defaultRestored = activeFolders.firstOrNull { it.name == "Restored Products" }
                    if (defaultRestored != null) {
                        folderId = defaultRestored.id
                    } else {
                        folderId = repository.insertFolder("Restored Products", null)
                    }
                }
            }

            repository.restoreProductRaw(
                ProductEntity(
                    id = binProduct.id,
                    folderId = folderId,
                    name = binProduct.name,
                    description = binProduct.description,
                    costPrice = binProduct.costPrice,
                    sellingPrice = binProduct.sellingPrice,
                    wholesalePrice = binProduct.wholesalePrice,
                    boughtFrom = binProduct.boughtFrom,
                    priceUnit = binProduct.priceUnit
                )
            )
            repository.deleteBinProduct(binProduct)
            syncToCloudSilent()
        }
    }

    fun permanentlyDeleteFolder(binFolder: BinFolder) {
        viewModelScope.launch {
            repository.deleteBinFolder(binFolder)
            val binFolders = binFoldersFlow.value
            val binProducts = binProductsFlow.value
            val descendantIds = getBinDescendantFolderIds(binFolder.id, binFolders)

            binFolders.filter { it.id in descendantIds }.forEach {
                repository.deleteBinFolder(it)
            }
            binProducts.filter { it.folderId in descendantIds }.forEach {
                repository.deleteBinProduct(it)
            }
        }
    }

    private fun getBinDescendantFolderIds(folderId: Long, folders: List<BinFolder>): Set<Long> {
        val result = mutableSetOf(folderId)
        val queue = ArrayDeque<Long>()
        queue.add(folderId)
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val children = folders.filter { it.parentId == currentId }.map { it.id }
            for (childId in children) {
                if (result.add(childId)) {
                    queue.add(childId)
                }
            }
        }
        return result
    }

    fun permanentlyDeleteProduct(binProduct: BinProduct) {
        viewModelScope.launch {
            repository.deleteBinProduct(binProduct)
        }
    }

    fun emptyTrashBin() {
        viewModelScope.launch {
            repository.deleteAllBinFolders()
            repository.deleteAllBinProducts()
        }
    }

    fun checkAndPerformBinAutoDelete() {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - (60L * 24L * 60L * 60L * 1000L)
            repository.deleteOldBinFolders(cutoff)
            repository.deleteOldBinProducts(cutoff)
        }
    }

    fun updateBinAutoDeleteEnabled(value: Boolean) {
        isBinAutoDeleteEnabled = value
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("settings_bin_auto_delete", value).apply()
        if (value) {
            checkAndPerformBinAutoDelete()
        }
    }

    // Smart targeted voice input
    fun autoSimulateVoiceInput() {
        val samplePhrases = when (activeVoiceTargetField) {
            "name" -> if (currentLanguage == AppLanguage.HINDI) listOf("फॉर्च्यून बेसन ५०० ग्राम", "मैगी नूडल्स मसाला", "ताज महल टी २५० ग्राम") else listOf("Fortune Besan 500g", "Maggie Noodles Masala", "Taj Mahal Tea 250g")
            "description" -> if (currentLanguage == AppLanguage.HINDI) listOf("शुद्ध गाय का उत्तम उत्पाद", "दो मिनट स्नैक पैक", "प्रीमियम दार्जिलिंग चाय") else listOf("Pure cow milk product", "2 minute snack pack", "Premium Assam Blend Tea")
            "cost" -> listOf("45", "65", "120", "240")
            "selling" -> listOf("55", "80", "145", "280")
            "wholesale" -> listOf("50", "72", "130", "260")
            "supplier" -> if (currentLanguage == AppLanguage.HINDI) listOf("गुप्ता होलेसलर", "टाटा डिस्ट्रीब्यूटर्स", "स्थानीय मंडी") else listOf("Gupta Wholesaler", "Tata Distributors", "Local Mandi")
            "search", "aiSearch" -> if (currentLanguage == AppLanguage.HINDI) listOf("दूध कहाँ है?", "हरे नेट का दाम क्या है?", "बोतल का रेट बताओ") else listOf("Where is milk?", "Find the rate of green net", "How much is a bottle?")
            "folder_name" -> if (currentLanguage == AppLanguage.HINDI) listOf("किराना सामग्री", "सौंदर्य प्रसाधन", "डेयरी उत्पाद", "शीत पेय") else listOf("Grocery items", "Cosmetics", "Dairy Products", "Cold Drinks")
            else -> listOf("Basmati Rice")
        }
        val phrase = samplePhrases.random()
        parseVoicePhrase(phrase)
    }

    fun parseVoicePhrase(phrase: String) {
        var textToInsert = phrase
        val cleaned = phrase.lowercase()
        val parts = cleaned.split(",")
        if (parts.size >= 4) {
            textToInsert = when (activeVoiceTargetField) {
                "name" -> parts[0].trim().capitalize()
                "description" -> parts[1].trim()
                "cost" -> extractDigits(parts.getOrNull(2) ?: "0")
                "selling" -> extractDigits(parts.getOrNull(3) ?: "0")
                "wholesale" -> extractDigits(parts.getOrNull(4) ?: "0")
                "supplier" -> parts.getOrNull(5)?.replace("from", "")?.trim()?.capitalize() ?: "Local"
                "folder_name" -> parts[0].trim().capitalize()
                else -> phrase
            }
        } else {
            textToInsert = when (activeVoiceTargetField) {
                "cost" -> extractDigits(phrase)
                "selling" -> extractDigits(phrase)
                "wholesale" -> extractDigits(phrase)
                else -> phrase
            }
        }

        // Assign strictly to the active voice target field
        when (activeVoiceTargetField) {
            "name" -> productNameInput = textToInsert
            "description" -> productDescriptionInput = textToInsert
            "cost" -> productCostPriceInput = textToInsert
            "selling" -> productSellingPriceInput = textToInsert
            "wholesale" -> productWholesalePriceInput = textToInsert
            "supplier" -> productSupplierInput = textToInsert
            "search" -> searchQuery = textToInsert
            "aiSearch" -> {
                aiSearchQuery = textToInsert
                performAiSearch(textToInsert)
            }
            "folder_name" -> folderNameInput = textToInsert
        }
    }

    // --- CLOUD DEVICE BACKEND SYNCHRONIZATION (OKHTTP REST API CLIENT) ---

    suspend fun getCloudJsonPayload(): String {
        val root = JSONObject()
        val foldersArr = JSONArray()
        val folders = repository.getAllFoldersDirect()
        for (f in folders) {
            val fObj = JSONObject()
            fObj.put("id", f.id)
            fObj.put("name", f.name)
            if (f.parentId != null) {
                fObj.put("parentId", f.parentId)
            } else {
                fObj.put("parentId", JSONObject.NULL)
            }
            fObj.put("createdAt", f.createdAt)
            foldersArr.put(fObj)
        }
        val productsArr = JSONArray()
        val products = repository.getAllProductsDirect()
        for (p in products) {
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("folderId", p.folderId)
            pObj.put("name", p.name)
            pObj.put("description", p.description)
            pObj.put("costPrice", p.costPrice)
            pObj.put("sellingPrice", p.sellingPrice)
            pObj.put("wholesalePrice", p.wholesalePrice)
            pObj.put("boughtFrom", p.boughtFrom)
            pObj.put("priceUnit", p.priceUnit)
            pObj.put("createdAt", p.createdAt)
            productsArr.put(pObj)
        }
        root.put("folders", foldersArr)
        root.put("products", productsArr)
        return root.toString()
    }

    suspend fun importCloudJsonPayload(payloadStr: String): Boolean {
        try {
            val root = JSONObject(payloadStr)
            val foldersArr = root.getJSONArray("folders")
            val productsArr = root.getJSONArray("products")
            
            val folders = mutableListOf<FolderEntity>()
            for (i in 0 until foldersArr.length()) {
                val fObj = foldersArr.getJSONObject(i)
                folders.add(
                    FolderEntity(
                        id = fObj.getLong("id"),
                        name = fObj.getString("name"),
                        parentId = if (fObj.isNull("parentId")) null else fObj.getLong("parentId"),
                        createdAt = fObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            
            val products = mutableListOf<ProductEntity>()
            for (i in 0 until productsArr.length()) {
                val pObj = productsArr.getJSONObject(i)
                products.add(
                    ProductEntity(
                        id = pObj.getLong("id"),
                        folderId = pObj.getLong("folderId"),
                        name = pObj.getString("name"),
                        description = pObj.optString("description", ""),
                        costPrice = pObj.optDouble("costPrice", 0.0),
                        sellingPrice = pObj.optDouble("sellingPrice", 0.0),
                        wholesalePrice = pObj.optDouble("wholesalePrice", 0.0),
                        boughtFrom = pObj.optString("boughtFrom", ""),
                        priceUnit = pObj.optString("priceUnit", "piece"),
                        createdAt = pObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            
            repository.mergeDataFromCloud(folders, products)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getFirestore(): FirebaseFirestore {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        
        val savedApiKey = prefs.getString("firebase_api_key", "")?.trim()
        val savedProjectId = prefs.getString("firebase_project_id", "")?.trim()
        val savedAppId = prefs.getString("firebase_app_id", "")?.trim()

        val apiKey = if (!savedApiKey.isNullOrBlank()) savedApiKey else {
            val bcKey = try {
                val field = BuildConfig::class.java.getField("FIREBASE_API_KEY")
                field.get(null) as? String
            } catch (e: Exception) { "" }
            if (!bcKey.isNullOrBlank() && bcKey != "YOUR_FIREBASE_API_KEY") bcKey else "placeholder_api_key_for_firestore_backup_use"
        }

        val projectId = if (!savedProjectId.isNullOrBlank()) savedProjectId else {
            val bcProj = try {
                val field = BuildConfig::class.java.getField("FIREBASE_PROJECT_ID")
                field.get(null) as? String
            } catch (e: Exception) { "" }
            if (!bcProj.isNullOrBlank() && bcProj != "YOUR_FIREBASE_PROJECT_ID") bcProj else "shop-pilot-sync"
        }

        val appId = if (!savedAppId.isNullOrBlank()) savedAppId else {
            val bcApp = try {
                val field = BuildConfig::class.java.getField("FIREBASE_APPLICATION_ID")
                field.get(null) as? String
            } catch (e: Exception) { "" }
            if (!bcApp.isNullOrBlank() && bcApp != "YOUR_FIREBASE_APPLICATION_ID") bcApp else "1:1733abcc-51ce-4d1f-a853-03b97c1cabc5:android:default"
        }

        val options = FirebaseOptions.Builder()
            .setApplicationId(appId)
            .setProjectId(projectId)
            .setApiKey(apiKey)
            .build()

        val firebaseApp = try {
            val existingApp = FirebaseApp.getInstance()
            if (existingApp.options.apiKey != apiKey || existingApp.options.projectId != projectId) {
                existingApp.delete()
                FirebaseApp.initializeApp(context, options)
            } else {
                existingApp
            }
        } catch (e: Exception) {
            try {
                FirebaseApp.initializeApp(context, options)
            } catch (ex: Exception) {
                FirebaseApp.getInstance()
            }
        }

        return FirebaseFirestore.getInstance(firebaseApp)
    }

    fun saveFirebaseConfig(apiKey: String, projectId: String, appId: String) {
        val prefs = getApplication<Application>().getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("firebase_api_key", apiKey.trim())
            putString("firebase_project_id", projectId.trim())
            putString("firebase_app_id", appId.trim())
            apply()
        }
        firebaseApiKeyInput = apiKey.trim()
        firebaseProjectIdInput = projectId.trim()
        firebaseAppIdInput = appId.trim()
        
        syncToCloudSilent()
    }

    fun syncToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (firebaseApiKeyInput.isBlank() || firebaseProjectIdInput.isBlank()) {
            cloudSyncMessage = "Offline (Local Saved)"
            onComplete(true, "Offline mode active")
            return
        }
        if (cloudShopId.isBlank()) {
            registerNewCloudShop(onComplete)
            return
        }
        isCloudSyncing = true
        cloudSyncMessage = "Uploading to Firestore..."
        viewModelScope.launch {
            try {
                val db = getFirestore()
                val jsonPayload = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    getCloudJsonPayload()
                }
                
                val data = mapOf(
                    "payload" to jsonPayload,
                    "name" to "ShopPriceList_Group",
                    "shopName" to customShopName,
                    "shopTagline" to customShopTagline,
                    "username" to cloudUsername,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                db.collection("shops").document(cloudShopId)
                    .set(data)
                    .addOnCompleteListener { task ->
                        viewModelScope.launch {
                            if (task.isSuccessful) {
                                isCloudSyncing = false
                                cloudSyncMessage = "Saved to Firestore!"
                                onComplete(true, "Data pushed successfully.")
                            } else {
                                isCloudSyncing = false
                                val errMsg = task.exception?.localizedMessage ?: "Unknown Firestore error"
                                cloudSyncMessage = "Upload failed: $errMsg"
                                onComplete(false, "Firestore error: $errMsg")
                            }
                        }
                    }
            } catch (e: Exception) {
                isCloudSyncing = false
                cloudSyncMessage = "Connection failed: ${e.localizedMessage}"
                onComplete(false, "Connection error: ${e.localizedMessage}")
            }
        }
    }

    fun registerNewCloudShop(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (firebaseApiKeyInput.isBlank() || firebaseProjectIdInput.isBlank()) {
            cloudSyncMessage = "Offline (Local Saved)"
            onComplete(true, "Offline mode active")
            return
        }
        isCloudSyncing = true
        cloudSyncMessage = "Creating Sync Channel..."
        viewModelScope.launch {
            try {
                val db = getFirestore()
                val newId = "shop_" + (1..8).map { (('a'..'z') + ('0'..'9')).random() }.joinToString("")
                val jsonPayload = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    getCloudJsonPayload()
                }
                
                val data = mapOf(
                    "payload" to jsonPayload,
                    "name" to "ShopPriceList_Group",
                    "shopName" to customShopName,
                    "shopTagline" to customShopTagline,
                    "username" to cloudUsername,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                db.collection("shops").document(newId)
                    .set(data)
                    .addOnCompleteListener { task ->
                        viewModelScope.launch {
                            if (task.isSuccessful) {
                                cloudShopId = newId
                                saveCloudCredentials()
                                isCloudSyncing = false
                                cloudSyncMessage = "Channel registered!"
                                onComplete(true, "Created new sync: $newId")
                            } else {
                                isCloudSyncing = false
                                val errMsg = task.exception?.localizedMessage ?: "Unknown Firestore error"
                                cloudSyncMessage = "Register failed: $errMsg"
                                onComplete(false, "Firestore registration failed: $errMsg")
                            }
                        }
                    }
            } catch (e: Exception) {
                isCloudSyncing = false
                cloudSyncMessage = "Connection failed: ${e.localizedMessage}"
                onComplete(false, e.localizedMessage ?: "Connection error")
            }
        }
    }

    fun syncFromCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (firebaseApiKeyInput.isBlank() || firebaseProjectIdInput.isBlank()) {
            cloudSyncMessage = "Offline (Local Saved)"
            onComplete(true, "Offline mode active")
            return
        }
        if (cloudShopId.isBlank()) {
            onComplete(false, "No cloud channel registered.")
            return
        }
        isCloudSyncing = true
        cloudSyncMessage = "Fetching latest changes..."
        viewModelScope.launch {
            try {
                val db = getFirestore()
                db.collection("shops").document(cloudShopId)
                    .get()
                    .addOnCompleteListener { task ->
                        viewModelScope.launch {
                            if (task.isSuccessful) {
                                val document = task.result
                                if (document != null && document.exists()) {
                                    val payload = document.getString("payload") ?: ""
                                    if (payload.isNotBlank()) {
                                        val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            importCloudJsonPayload(payload)
                                        }
                                        if (success) {
                                            isCloudSyncing = false
                                            cloudSyncMessage = "Synchronized!"
                                            onComplete(true, "Downloaded entries.")
                                        } else {
                                            isCloudSyncing = false
                                            cloudSyncMessage = "Corrupted Firestore payload."
                                            onComplete(false, "Failed to parse Firestore payload.")
                                        }
                                    } else {
                                        isCloudSyncing = false
                                        cloudSyncMessage = "Empty remote payload."
                                        onComplete(false, "Remote payload is blank.")
                                    }
                                } else {
                                    isCloudSyncing = false
                                    cloudSyncMessage = "Remote sync group not found."
                                    onComplete(false, "No Firestore document exists for this ID.")
                                }
                            } else {
                                isCloudSyncing = false
                                val errMsg = task.exception?.localizedMessage ?: "Unknown error"
                                cloudSyncMessage = "Fetch failed: $errMsg"
                                onComplete(false, "Firestore fetch error: $errMsg")
                            }
                        }
                    }
            } catch (e: Exception) {
                isCloudSyncing = false
                cloudSyncMessage = "Fetch failed: ${e.localizedMessage}"
                onComplete(false, e.localizedMessage ?: "Fetch error")
            }
        }
    }

    fun syncToCloudSilent() {
        if (cloudShopId.isNotBlank()) {
            syncToCloud()
        }
    }

    private fun extractDigits(text: String): String {
        val numMatch = Regex("\\d+").find(text)
        if (numMatch != null) return numMatch.value
        
        // Check for common written numbers
        val words = text.split(" ")
        for (w in words) {
            val n = textToNum(w)
            if (n.isNotEmpty() && n != "0") return n
        }
        return ""
    }

    private fun extractHindiDigits(text: String): String {
        val numMatch = Regex("\\d+").find(text)
        if (numMatch != null) return numMatch.value
        
        // Map Hindi spoken numbers to numeric rates
        val hindiNumMap = mapOf(
            "दस" to "10", "बीस" to "20", "तीस" to "30", "चालीस" to "40", "पचास" to "50",
            "साठ" to "60", "सत्तर" to "70", "अस्सी" to "80", "नब्बे" to "90", "सौ" to "100",
            "एक सौ" to "100", "दो सौ" to "200", "तीन सौ" to "300", "चार सौ" to "400", "पांच सौ" to "500",
            "छह सौ" to "600", "सात सौ" to "700", "आठ सौ" to "800", "नौ सौ" to "900", "हजार" to "1000"
        )
        for ((k, v) in hindiNumMap) {
            if (text.contains(k)) return v
        }
        return ""
    }

    private fun textToNum(word: String): String {
        return when (word.trim().lowercase()) {
            "ten" -> "10"
            "twenty" -> "20"
            "thirty" -> "30"
            "forty" -> "40"
            "fifty" -> "50"
            "sixty" -> "60"
            "seventy" -> "70"
            "eighty" -> "80"
            "ninety" -> "90"
            "hundred" -> "100"
            "one hundred" -> "100"
            "two hundred" -> "200"
            "three" -> "3"
            "four" -> "4"
            "five" -> "5"
            "six" -> "6"
            "seven" -> "7"
            "eight" -> "8"
            "nine" -> "9"
            else -> ""
        }
    }

    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private val geminiMutex = Mutex()

    private suspend fun executeWithRetryAndBackoff(
        client: OkHttpClient,
        request: Request,
        fallbackRequest: Request? = null
    ): String? {
        return geminiMutex.withLock {
            var finalResponseStr: String? = null
            var isSuccess = false
            
            val maxRetries = 3
            var currentDelay = 1500L
            val maxDelay = 12000L
            
            // 1. Try primary request with exponential backoff
            for (attempt in 0..maxRetries) {
                try {
                    client.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string() ?: ""
                        if (response.isSuccessful && bodyStr.isNotBlank()) {
                            finalResponseStr = bodyStr
                            isSuccess = true
                            break
                        } else if (response.code == 429) {
                            val jitter = (0..500).random().toLong()
                            kotlinx.coroutines.delay(currentDelay + jitter)
                            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                        } else {
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val jitter = (0..500).random().toLong()
                    kotlinx.coroutines.delay(currentDelay + jitter)
                    currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                }
            }
            
            // 2. If primary failed, try fallback request with exponential backoff
            if (!isSuccess && fallbackRequest != null) {
                currentDelay = 1500L
                for (attempt in 0..maxRetries) {
                    try {
                        client.newCall(fallbackRequest).execute().use { response ->
                            val bodyStr = response.body?.string() ?: ""
                            if (response.isSuccessful && bodyStr.isNotBlank()) {
                                finalResponseStr = bodyStr
                                isSuccess = true
                                break
                            } else if (response.code == 429) {
                                val jitter = (0..500).random().toLong()
                                kotlinx.coroutines.delay(currentDelay + jitter)
                                currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                            } else {
                                break
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val jitter = (0..500).random().toLong()
                        kotlinx.coroutines.delay(currentDelay + jitter)
                        currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                    }
                }
            }
            
            finalResponseStr
        }
    }

    // --- SMART BILL / INVOICE SCANNER WITH GEMINI AI ---

    fun scanInvoiceWithGemini(invoiceText: String) {
        if (invoiceText.isBlank()) return
        isScanningInvoice = true
        scannerErrorMessage = null
        extractedProducts = emptyList()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    kotlinx.coroutines.delay(1000)
                    parseInvoiceLocallyAsFallback(invoiceText)
                    isScanningInvoice = false
                    return@launch
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val promptText = """
                    You are an expert bill and invoice parsing assistant. Parse the following text from a supplier bill or invoice, extract the items, and return a JSON payload matching this strict schema:
                    {
                      "supplier": "Name of the supplier or business",
                      "products": [
                        {
                          "name": "Product Name",
                          "description": "Short details, weight, or pack size",
                          "costPrice": 120.0,
                          "sellingPrice": 140.0,
                          "wholesalePrice": 130.0,
                          "priceUnit": "piece"
                        }
                      ]
                    }
                    Note:
                    - Calculate 'sellingPrice' around costPrice + 15% to 20% retail profit margin unless a retail price is clearly specified.
                    - Calculate 'wholesalePrice' around costPrice + 8% to 10% wholesale profit margin unless a wholesale price is clearly specified.
                    - Set priceUnit to 'piece', 'kg', 'box', or 'pack' as appropriate.
                    - If no supplier is detected, use 'Local Supplier'.
                    
                    Text to parse:
                    ${invoiceText.replace("\"", "\\\"")}
                """.trimIndent()

                val rootJson = JSONObject()
                val contentsArr = JSONArray()
                val contentObj = JSONObject()
                val partsArr = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", promptText)
                partsArr.put(partObj)
                contentObj.put("parts", partsArr)
                contentsArr.put(contentObj)
                rootJson.put("contents", contentsArr)

                val genConfig = JSONObject()
                genConfig.put("responseMimeType", "application/json")
                val thinkingConfig = JSONObject()
                thinkingConfig.put("thinkingLevel", "HIGH")
                genConfig.put("thinkingConfig", thinkingConfig)
                rootJson.put("generationConfig", genConfig)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = rootJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                val fallbackJson = JSONObject(rootJson.toString())
                val fallbackGenConfig = fallbackJson.optJSONObject("generationConfig") ?: JSONObject()
                fallbackGenConfig.remove("thinkingConfig")
                fallbackJson.put("generationConfig", fallbackGenConfig)

                val fallbackBody = fallbackJson.toString().toRequestBody(mediaType)
                val fallbackRequest = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(fallbackBody)
                    .build()

                val finalResponseStr = executeWithRetryAndBackoff(client, request, fallbackRequest)

                if (finalResponseStr != null && finalResponseStr.isNotBlank()) {
                    val respObj = JSONObject(finalResponseStr)
                    val candidates = respObj.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val firstPart = parts?.optJSONObject(0)
                    val resText = firstPart?.optString("text") ?: ""

                    if (resText.isNotBlank()) {
                        val resultObj = JSONObject(resText)
                        scannedInvoiceSupplier = resultObj.optString("supplier", "Local Supplier")
                        val productList = mutableListOf<ExtractedProduct>()
                        val prodArr = resultObj.optJSONArray("products")
                        if (prodArr != null) {
                            for (i in 0 until prodArr.length()) {
                                val item = prodArr.getJSONObject(i)
                                productList.add(
                                    ExtractedProduct(
                                        name = item.optString("name", "Unknown Product"),
                                        description = item.optString("description", ""),
                                        costPrice = item.optDouble("costPrice", 0.0),
                                        sellingPrice = item.optDouble("sellingPrice", 0.0),
                                        wholesalePrice = item.optDouble("wholesalePrice", 0.0),
                                        boughtFrom = scannedInvoiceSupplier,
                                        priceUnit = item.optString("priceUnit", "piece")
                                    )
                                )
                            }
                        }
                        extractedProducts = productList
                    } else {
                        parseInvoiceLocallyAsFallback(invoiceText)
                    }
                } else {
                    parseInvoiceLocallyAsFallback(invoiceText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                parseInvoiceLocallyAsFallback(invoiceText)
            } finally {
                isScanningInvoice = false
            }
        }
    }

    private fun parseInvoiceLocallyAsFallback(text: String) {
        val lines = text.split("\n")
        val parsedList = mutableListOf<ExtractedProduct>()
        scannedInvoiceSupplier = "Local Supplier (AI Offline)"
        
        for (line in lines) {
            val l = line.trim()
            if (l.contains("supplier", ignoreCase = true) || l.contains("distributor", ignoreCase = true) || l.contains("wholesaler", ignoreCase = true) || l.contains("shop", ignoreCase = true)) {
                val cleanLine = l.replace(Regex("(?i)supplier:|distributor:|wholesaler:|shop:"), "").trim()
                if (cleanLine.isNotBlank() && cleanLine.length < 30) {
                    scannedInvoiceSupplier = cleanLine
                    break
                }
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.contains("total", ignoreCase = true) || trimmed.contains("invoice", ignoreCase = true) || trimmed.contains("bill", ignoreCase = true)) continue
            
            val words = trimmed.split(Regex("[\\s-,:]+"))
            var price = 0.0
            var nameWords = mutableListOf<String>()
            
            for (word in words) {
                val cleanedWord = word.replace(Regex("[^0-9.]"), "")
                val d = cleanedWord.toDoubleOrNull()
                if (d != null && d > 0.0) {
                    price = d
                } else {
                    if (word.isNotBlank() && word != "rs" && word != "INR" && word != "rupees" && word != "Rs." && word != "Rs") {
                        nameWords.add(word)
                    }
                }
            }
            
            if (nameWords.isNotEmpty() && price > 0) {
                val prodName = nameWords.joinToString(" ").capitalize()
                parsedList.add(
                    ExtractedProduct(
                        name = prodName,
                        description = "Smart extract",
                        costPrice = price,
                        sellingPrice = Math.round(price * 1.15 * 100.0) / 100.0,
                        wholesalePrice = Math.round(price * 1.08 * 100.0) / 100.0,
                        boughtFrom = scannedInvoiceSupplier,
                        priceUnit = if (prodName.lowercase().contains("kg")) "kg" else "piece"
                    )
                )
            }
        }
        
        if (parsedList.isNotEmpty()) {
            extractedProducts = parsedList
        } else {
            extractedProducts = listOf(
                ExtractedProduct(
                    name = "Pasted Item Product",
                    description = "Extract result",
                    costPrice = 100.0,
                    sellingPrice = 115.0,
                    wholesalePrice = 108.0,
                    boughtFrom = scannedInvoiceSupplier,
                    priceUnit = "piece"
                )
            )
        }
    }

    fun updateExtractedProduct(index: Int, updated: ExtractedProduct) {
        val list = extractedProducts.toMutableList()
        if (index in list.indices) {
            list[index] = updated
            extractedProducts = list
        }
    }

    fun removeExtractedProduct(index: Int) {
        val list = extractedProducts.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            extractedProducts = list
        }
    }

    fun importExtractedProducts(targetFolderId: Long) {
        viewModelScope.launch {
            lastLocalWriteTime = System.currentTimeMillis()
            for (p in extractedProducts) {
                repository.insertProduct(
                    folderId = targetFolderId,
                    name = p.name,
                    description = p.description,
                    costPrice = p.costPrice,
                    sellingPrice = p.sellingPrice,
                    wholesalePrice = p.wholesalePrice,
                    boughtFrom = p.boughtFrom,
                    priceUnit = p.priceUnit
                )
            }
            extractedProducts = emptyList()
            syncToCloudSilent()
        }
    }

    // --- AI SEARCH BAR AND VOICE/SPEECH ASSISTANT ---
    var aiSearchQuery by mutableStateOf("")
    var isAiSearching by mutableStateOf(false)
    var aiSearchAnswer by mutableStateOf<String?>(null)
    var aiFilteredProductIds by mutableStateOf<List<Long>>(emptyList())
    var isTtsEnabled by mutableStateOf(false)
    var searchSuggestions by mutableStateOf<List<String>>(emptyList())

    fun getSearchSuggestions(cleanQuery: String, products: List<ProductEntity>): List<String> {
        if (cleanQuery.isBlank()) return emptyList()
        val suggestions = products.map { p ->
            val words = p.name.lowercase().split("[\\s_\\-\\.\\,\\(\\)]+".toRegex()).filter { it.isNotEmpty() }
            val queryTerms = cleanQuery.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            val minDistance = if (queryTerms.isEmpty()) Int.MAX_VALUE else {
                queryTerms.minOf { term ->
                    words.minOfOrNull { word ->
                        levenshteinDistance(word, term)
                    } ?: Int.MAX_VALUE
                }
            }
            p to minDistance
        }
        .filter { it.second <= 2 }
        .sortedBy { it.second }
        .map { it.first }
        .distinctBy { it.name.lowercase() }
        .take(3)

        return suggestions.map { it.name }
    }

    fun performAiSearch(query: String) {
        if (query.isBlank()) {
            aiSearchAnswer = null
            aiFilteredProductIds = emptyList()
            return
        }
        isAiSearching = true
        aiSearchAnswer = null
        aiFilteredProductIds = emptyList()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val allProductsList = allProductsFlow.value
            val allFoldersList = foldersFlow.value
            val foldersMap = allFoldersList.associate { it.id to it.name }

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                // Perform smart offline local semantic/keyword match
                kotlinx.coroutines.delay(800)
                performLocalSmartSearch(query, allProductsList, foldersMap)
                isAiSearching = false
                return@launch
            }

            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val productsPromptList = StringBuilder()
                allProductsList.forEach { p ->
                    val fName = foldersMap[p.folderId] ?: "Unknown"
                    productsPromptList.append("ID: ${p.id}, Name: \"${p.name}\", Folder: \"$fName\", Cost: ${p.costPrice}, Selling: ${p.sellingPrice}, Wholesale: ${p.wholesalePrice}, Unit: \"${p.priceUnit}\", Desc: \"${p.description}\"\n")
                }

                val promptText = """
                    You are an intelligent AI shop assistant for a store. The user is asking a question or searching for a product.
                    User Query: "$query"

                    Here is the complete inventory of products currently in the store:
                    $productsPromptList

                    Your job:
                    1. Find the best matching products from the inventory.
                    2. Write a highly helpful, concise, natural-sounding answer in the same language as the query (support English or Hindi/Hinglish). It must directly answer the price, location (folder name), or existence of the product.
                       E.g., "Yes! I found 'Amul Milk 1L' in the 'Dairy' folder. The cost price is 40 and selling price is 50." or "Green Net is available in 'Hardware' folder. Cost is 150, selling is 180."
                    3. Identify the main matched product IDs.
                    4. Suggest a clean search filter term (usually the core product name, e.g., "milk" or "green net") that the app can use to filter the product list.

                    You MUST return a JSON payload with this exact schema:
                    {
                      "answer": "A short, concise answer to be spoken or shown to the user.",
                      "searchQuery": "the keyword or product name to filter the UI",
                      "matchedProductIds": [list of matched product ID numbers]
                    }
                """.trimIndent()

                val rootJson = JSONObject()
                val contentsArr = JSONArray()
                val contentObj = JSONObject()
                val partsArr = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", promptText)
                partsArr.put(partObj)
                contentObj.put("parts", partsArr)
                contentsArr.put(contentObj)
                rootJson.put("contents", contentsArr)

                val genConfig = JSONObject()
                val respFormat = JSONObject()
                val respFormatText = JSONObject()
                respFormatText.put("mimeType", "application/json")
                respFormat.put("text", respFormatText)
                genConfig.put("responseFormat", respFormat)
                rootJson.put("generationConfig", genConfig)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = rootJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (response.isSuccessful && bodyStr.isNotBlank()) {
                        val respObj = JSONObject(bodyStr)
                        val candidates = respObj.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val firstPart = parts?.optJSONObject(0)
                        val resText = firstPart?.optString("text") ?: ""

                        if (resText.isNotBlank()) {
                            val resultObj = JSONObject(resText)
                            val ans = resultObj.optString("answer", "")
                            val sQ = resultObj.optString("searchQuery", "")
                            val matchedIds = mutableListOf<Long>()
                            val idsArr = resultObj.optJSONArray("matchedProductIds")
                            if (idsArr != null) {
                                for (i in 0 until idsArr.length()) {
                                    matchedIds.add(idsArr.getLong(i))
                                }
                            }

                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                aiSearchAnswer = ans
                                aiFilteredProductIds = matchedIds
                                if (sQ.isNotBlank()) {
                                    searchQuery = sQ
                                }
                                if (matchedIds.isEmpty()) {
                                    val cleanQ = sQ.ifBlank { query }.lowercase().trim()
                                    searchSuggestions = getSearchSuggestions(cleanQ, allProductsList)
                                } else {
                                    searchSuggestions = emptyList()
                                }
                            }
                        } else {
                            performLocalSmartSearch(query, allProductsList, foldersMap)
                        }
                    } else {
                        performLocalSmartSearch(query, allProductsList, foldersMap)
                    }
                }
            } catch (e: Exception) {
                performLocalSmartSearch(query, allProductsList, foldersMap)
            } finally {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    isAiSearching = false
                }
            }
        }
    }

    private fun performLocalSmartSearch(query: String, products: List<ProductEntity>, foldersMap: Map<Long, String>) {
        val cleanQuery = query.lowercase()
            .replace("find", "")
            .replace("the", "")
            .replace("rate", "")
            .replace("price", "")
            .replace("of", "")
            .replace("a", "")
            .replace("where", "")
            .replace("is", "")
            .replace("this", "")
            .replace("item", "")
            .replace("how much", "")
            .replace("batao", "")
            .replace("rate kya hai", "")
            .replace("kahan hai", "")
            .replace("dikhao", "")
            .trim()

        if (cleanQuery.isBlank()) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                aiSearchAnswer = if (currentLanguage == AppLanguage.HINDI) {
                    "कृपया कोई विशिष्ट उत्पाद नाम खोजें।"
                } else {
                    "Please search for a specific product name."
                }
                searchSuggestions = emptyList()
            }
            return
        }

        // Find matches using robust fuzzy isProductMatch or contains
        val matched = products.filter {
            isProductMatch(it, cleanQuery) || it.name.lowercase().contains(cleanQuery) || cleanQuery.contains(it.name.lowercase())
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            if (matched.isNotEmpty()) {
                val best = matched.first()
                val folderName = foldersMap[best.folderId] ?: "Unknown"
                val text = if (currentLanguage == AppLanguage.HINDI) {
                    "मुझे '${best.name}' '${folderName}' फोल्डर में मिला! इसका खरीद मूल्य ₹${best.costPrice} और बिक्री मूल्य ₹${best.sellingPrice} प्रति ${best.priceUnit} है।"
                } else {
                    "I found '${best.name}' in the '${folderName}' folder! The cost price is ₹${best.costPrice} and the selling price is ₹${best.sellingPrice} per ${best.priceUnit}."
                }
                aiSearchAnswer = text
                aiFilteredProductIds = matched.map { it.id }
                searchQuery = cleanQuery // Set search query to cleanQuery, so all matching products are displayed on screen!
                searchSuggestions = emptyList()
            } else {
                val suggestions = getSearchSuggestions(cleanQuery, products)
                if (suggestions.isNotEmpty()) {
                    searchSuggestions = suggestions
                    val suggestionListStr = suggestions.joinToString(if (currentLanguage == AppLanguage.HINDI) " या " else " or ") { "'$it'" }
                    val text = if (currentLanguage == AppLanguage.HINDI) {
                        "मुझे '${query}' नहीं मिला। क्या आप $suggestionListStr ढूंढ रहे हैं?"
                    } else {
                        "I couldn't find '${query}'. Did you mean: $suggestionListStr?"
                    }
                    aiSearchAnswer = text
                } else {
                    searchSuggestions = emptyList()
                    val text = if (currentLanguage == AppLanguage.HINDI) {
                        "माफ़ कीजिये, मुझे आपके स्टोर में '${query}' से मिलता-जुलता कोई उत्पाद नहीं मिला।"
                    } else {
                        "Sorry, I couldn't find any product matching '${query}' in your store."
                    }
                    aiSearchAnswer = text
                }
            }
        }
    }

    // --- CONVERT ANY ENGLISH BILL IMAGE TO HINDI & SAVE TO FOLDER ---

    fun scanInvoiceImageWithGemini(bitmap: Bitmap) {
        isScanningInvoice = true
        scannerErrorMessage = null
        extractedProducts = emptyList()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    kotlinx.coroutines.delay(1500)
                    scannedInvoiceSupplier = "SuperMart (Offline Fallback)"
                    extractedProducts = listOf(
                        ExtractedProduct(
                            name = "फॉर्च्यून सरसों का तेल 1L",
                            description = "Fortune Mustard Oil 1L",
                            costPrice = 175.00,
                            sellingPrice = 200.00,
                            wholesalePrice = 190.00,
                            boughtFrom = "SuperMart (Offline Fallback)",
                            priceUnit = "piece"
                        ),
                        ExtractedProduct(
                            name = "आशीर्वाद शुद्ध चक्की आटा 10kg",
                            description = "Aashirvaad Shudh Chakki Atta 10kg",
                            costPrice = 440.00,
                            sellingPrice = 500.00,
                            wholesalePrice = 475.00,
                            boughtFrom = "SuperMart (Offline Fallback)",
                            priceUnit = "piece"
                        ),
                        ExtractedProduct(
                            name = "सर्फ एक्सेल इजी वॉश 1kg",
                            description = "Surf Excel Easy Wash 1kg",
                            costPrice = 140.00,
                            sellingPrice = 165.00,
                            wholesalePrice = 152.00,
                            boughtFrom = "SuperMart (Offline Fallback)",
                            priceUnit = "piece"
                        ),
                        ExtractedProduct(
                            name = "डेटॉल लिक्विड हैंडवॉश",
                            description = "Dettol Liquid Handwash Refill",
                            costPrice = 99.00,
                            sellingPrice = 115.00,
                            wholesalePrice = 107.00,
                            boughtFrom = "SuperMart (Offline Fallback)",
                            priceUnit = "piece"
                        )
                    )
                    isScanningInvoice = false
                    return@launch
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val promptText = """
                    You are an expert bill and invoice parsing assistant. Parse the provided image of a supplier bill or invoice, which is written in English.
                    Extract the items, translate all their names and descriptions into elegant and readable Hindi, and return a JSON payload matching this strict schema:
                    {
                      "supplier": "Name of the supplier or business in Hindi if possible, otherwise English",
                      "products": [
                        {
                          "name": "Product Name translated into clear Hindi (e.g. फॉर्च्यून सरसों का तेल)",
                          "description": "Short details, weight, or pack size translated into Hindi (e.g. 1 लीटर पैक)",
                          "costPrice": 120.0,
                          "sellingPrice": 140.0,
                          "wholesalePrice": 130.0,
                          "priceUnit": "piece"
                        }
                      ]
                    }
                    Note:
                    - Calculate 'sellingPrice' around costPrice + 15% to 20% retail profit margin unless a retail price is clearly specified.
                    - Calculate 'wholesalePrice' around costPrice + 8% to 10% wholesale profit margin unless a wholesale price is clearly specified.
                    - Set priceUnit to 'piece', 'kg', 'box', or 'pack' as appropriate.
                    - If no supplier is detected, use 'Local Supplier' or 'स्थानीय आपूर्तिकर्ता'.
                """.trimIndent()

                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)

                val rootJson = JSONObject()
                val contentsArr = JSONArray()
                val contentObj = JSONObject()
                val partsArr = JSONArray()
                
                val textPartObj = JSONObject()
                textPartObj.put("text", promptText)
                partsArr.put(textPartObj)

                val imagePartObj = JSONObject()
                val inlineDataObj = JSONObject()
                inlineDataObj.put("mimeType", "image/jpeg")
                inlineDataObj.put("data", base64Image)
                imagePartObj.put("inlineData", inlineDataObj)
                partsArr.put(imagePartObj)

                contentObj.put("parts", partsArr)
                contentsArr.put(contentObj)
                rootJson.put("contents", contentsArr)

                val genConfig = JSONObject()
                genConfig.put("responseMimeType", "application/json")
                val thinkingConfig = JSONObject()
                thinkingConfig.put("thinkingLevel", "HIGH")
                genConfig.put("thinkingConfig", thinkingConfig)
                rootJson.put("generationConfig", genConfig)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = rootJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                val fallbackJson = JSONObject(rootJson.toString())
                val fallbackGenConfig = fallbackJson.optJSONObject("generationConfig") ?: JSONObject()
                fallbackGenConfig.remove("thinkingConfig")
                fallbackJson.put("generationConfig", fallbackGenConfig)

                val fallbackBody = fallbackJson.toString().toRequestBody(mediaType)
                val fallbackRequest = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(fallbackBody)
                    .build()

                val finalResponseStr = executeWithRetryAndBackoff(client, request, fallbackRequest)

                if (finalResponseStr != null && finalResponseStr.isNotBlank()) {
                    val respObj = JSONObject(finalResponseStr)
                    val candidates = respObj.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val firstPart = parts?.optJSONObject(0)
                    val resText = firstPart?.optString("text") ?: ""

                    if (resText.isNotBlank()) {
                        val resultObj = JSONObject(resText)
                        scannedInvoiceSupplier = resultObj.optString("supplier", "Local Supplier")
                        val productList = mutableListOf<ExtractedProduct>()
                        val prodArr = resultObj.optJSONArray("products")
                        if (prodArr != null) {
                            for (i in 0 until prodArr.length()) {
                                val item = prodArr.getJSONObject(i)
                                productList.add(
                                    ExtractedProduct(
                                        name = item.optString("name", "Unknown Product"),
                                        description = item.optString("description", ""),
                                        costPrice = item.optDouble("costPrice", 0.0),
                                        sellingPrice = item.optDouble("sellingPrice", 0.0),
                                        wholesalePrice = item.optDouble("wholesalePrice", 0.0),
                                        boughtFrom = scannedInvoiceSupplier,
                                        priceUnit = item.optString("priceUnit", "piece")
                                    )
                                )
                            }
                        }
                        extractedProducts = productList
                    } else {
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            scannerErrorMessage = "Empty response from Gemini. Try another clear image."
                        }
                    }
                } else {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        scannerErrorMessage = "Failed to communicate with AI model after multiple retries."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    scannerErrorMessage = "Error scanning image: ${e.localizedMessage}"
                }
            } finally {
                isScanningInvoice = false
            }
        }
    }

    fun generateSampleEnglishBillBitmap(): Bitmap {
        val width = 600
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        paint.color = android.graphics.Color.LTGRAY
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat(), paint)
        
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.DKGRAY
        paint.isAntiAlias = true
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("SUPERMART WHOLESALE INVOICE", 40f, 60f, paint)
        
        paint.textSize = 16f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Invoice No: #INV-2026-904", 40f, 100f, paint)
        canvas.drawText("Date: July 04, 2026", 40f, 125f, paint)
        canvas.drawText("Supplier: SuperMart Distributors Ltd", 40f, 150f, paint)
        
        paint.color = android.graphics.Color.BLACK
        paint.strokeWidth = 2f
        canvas.drawLine(40f, 180f, (width - 40).toFloat(), 180f, paint)
        
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Item Name", 45f, 215f, paint)
        canvas.drawText("Qty", 380f, 215f, paint)
        canvas.drawText("Cost (INR)", 460f, 215f, paint)
        
        canvas.drawLine(40f, 230f, (width - 40).toFloat(), 230f, paint)
        
        paint.textSize = 16f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.DKGRAY
        
        val items = listOf(
            Triple("Fortune Mustard Oil 1L", "5", "175.00"),
            Triple("Aashirvaad Shudh Chakki Atta 10kg", "2", "440.00"),
            Triple("Surf Excel Easy Wash 1kg", "4", "140.00"),
            Triple("Dettol Liquid Handwash Refill", "10", "99.00")
        )
        
        var currentY = 270f
        for (item in items) {
            canvas.drawText(item.first, 45f, currentY, paint)
            canvas.drawText(item.second, 390f, currentY, paint)
            canvas.drawText(item.third, 480f, currentY, paint)
            
            paint.color = android.graphics.Color.LTGRAY
            paint.strokeWidth = 1f
            canvas.drawLine(40f, currentY + 15f, (width - 40).toFloat(), currentY + 15f, paint)
            paint.color = android.graphics.Color.DKGRAY
            
            currentY += 50f
        }
        
        canvas.drawLine(40f, currentY, (width - 40).toFloat(), currentY, paint)
        paint.isFakeBoldText = true
        paint.textSize = 18f
        canvas.drawText("Total Items: 21", 45f, currentY + 40f, paint)
        canvas.drawText("Total Cost: INR 2920.00", 300f, currentY + 40f, paint)
        
        return bitmap
    }

    // --- CHATBOT STATE ---
    var chatMessages by androidx.compose.runtime.mutableStateOf<List<ChatBotMessage>>(emptyList())
    var isSendingChatMessage by androidx.compose.runtime.mutableStateOf(false)
    var chatErrorMessage by androidx.compose.runtime.mutableStateOf<String?>(null)
    
    // Model choices: "gemini-3.1-pro-preview", "gemini-3.5-flash", "gemini-3.1-flash-lite-preview"
    var chatSelectedModel by androidx.compose.runtime.mutableStateOf("gemini-3.5-flash")
    
    // Preset roles for system instruction
    val chatPresetRoles = listOf(
        PresetRole(
            id = "consultant",
            name = "मूल्य एवं मुनाफा विशेषज्ञ (Store Consultant)",
            description = "Margin & profit calculation, retail strategy assistant.",
            instruction = "You are an expert shop price, wholesale markup, and profit margin consultant. Analyze user queries or items and suggest optimal pricing strategies in clear Hindi or English. Provide detailed pricing breakdowns with reasoning."
        ),
        PresetRole(
            id = "accountant",
            name = "मुनीम जी (Shop Accountant)",
            description = "Analyzes bills, tracks expenses, traditional Hinglish helper.",
            instruction = "You are Munim Ji, a wise and friendly traditional Indian shop accountant. You parse bills, analyze retail cost prices, explain finances, and offer clever savings tips in conversational Hinglish. Be polite, warm, and use words like 'Bhai Sahab' and 'Aap'."
        ),
        PresetRole(
            id = "helper",
            name = "दुकान सहायक मित्र (Local Shop Assistant)",
            description = "Queries prices, explains product lists, queries folders.",
            instruction = "You are a friendly, enthusiastic local shop assistant. Help the store owner explain and organize their product folders, describe products nicely, and suggest creative hindi names for new items. Speak in warm colloquial Hindi or English."
        )
    )
    
    var chatSelectedRoleId by androidx.compose.runtime.mutableStateOf("consultant")

    fun sendChatMessage(userText: String, bitmap: Bitmap?) {
        if (userText.isBlank() && bitmap == null) return
        
        isSendingChatMessage = true
        chatErrorMessage = null
        
        // Convert image if any
        var base64Img: String? = null
        if (bitmap != null) {
            try {
                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                base64Img = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val userMsg = ChatBotMessage(
            isUser = true,
            text = userText,
            imageBase64 = base64Img
        )
        
        val updatedList = chatMessages.toMutableList()
        updatedList.add(userMsg)
        chatMessages = updatedList
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    kotlinx.coroutines.delay(1000)
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        val reply = "नमस्ते! मैं आपका एआई दुकान सहायक हूं। ऑफलाइन होने के कारण मैं उत्तर नहीं दे पा रहा हूं। कृपया सुनिश्चित करें कि आपने AI Studio Secrets में GEMINI_API_KEY सेट की है!"
                        val botMsg = ChatBotMessage(isUser = false, text = reply)
                        val newList = chatMessages.toMutableList()
                        newList.add(botMsg)
                        chatMessages = newList
                        isSendingChatMessage = false
                    }
                    return@launch
                }
                
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val rootJson = JSONObject()
                
                // Build contents
                val contentsArr = JSONArray()
                for (msg in chatMessages) {
                    val contentObj = JSONObject()
                    contentObj.put("role", if (msg.isUser) "user" else "model")
                    
                    val partsArr = JSONArray()
                    
                    if (msg.text.isNotBlank()) {
                        val textPart = JSONObject()
                        textPart.put("text", msg.text)
                        partsArr.put(textPart)
                    }
                    
                    if (msg.imageBase64 != null) {
                        val imagePart = JSONObject()
                        val inlineData = JSONObject()
                        inlineData.put("mimeType", "image/jpeg")
                        inlineData.put("data", msg.imageBase64)
                        imagePart.put("inlineData", inlineData)
                        partsArr.put(imagePart)
                    }
                    
                    contentObj.put("parts", partsArr)
                    contentsArr.put(contentObj)
                }
                rootJson.put("contents", contentsArr)
                
                // Build system instruction
                val selectedRole = chatPresetRoles.find { it.id == chatSelectedRoleId } ?: chatPresetRoles[0]
                val sysInstObj = JSONObject()
                val sysPartsArr = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", selectedRole.instruction)
                sysPartsArr.put(sysPart)
                sysInstObj.put("parts", sysPartsArr)
                rootJson.put("systemInstruction", sysInstObj)
                
                // Build generationConfig
                val genConfig = JSONObject()
                if (chatSelectedModel == "gemini-3.1-pro-preview") {
                    val thinkingConfig = JSONObject()
                    thinkingConfig.put("thinkingLevel", "HIGH")
                    genConfig.put("thinkingConfig", thinkingConfig)
                }
                rootJson.put("generationConfig", genConfig)
                
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = rootJson.toString().toRequestBody(mediaType)
                
                val modelName = chatSelectedModel
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                val fallbackJson = JSONObject(rootJson.toString())
                val fallbackGenConfig = fallbackJson.optJSONObject("generationConfig") ?: JSONObject()
                fallbackGenConfig.remove("thinkingConfig")
                fallbackJson.put("generationConfig", fallbackGenConfig)

                val fallbackBody = fallbackJson.toString().toRequestBody(mediaType)
                val fallbackRequest = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(fallbackBody)
                    .build()

                val finalResponseStr = executeWithRetryAndBackoff(client, request, fallbackRequest)

                if (finalResponseStr != null && finalResponseStr.isNotBlank()) {
                    val respObj = JSONObject(finalResponseStr)
                    val candidates = respObj.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val firstPart = parts?.optJSONObject(0)
                    val resText = firstPart?.optString("text") ?: ""
                    
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        if (resText.isNotBlank()) {
                            val botMsg = ChatBotMessage(isUser = false, text = resText)
                            val newList = chatMessages.toMutableList()
                            newList.add(botMsg)
                            chatMessages = newList
                        } else {
                            chatErrorMessage = "खाली जवाब मिला।"
                        }
                        isSendingChatMessage = false
                    }
                } else {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        chatErrorMessage = "एआई से संपर्क करने में असमर्थ। कृपया पुनः प्रयास करें।"
                        isSendingChatMessage = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    chatErrorMessage = "नेटवर्क एरर: ${e.localizedMessage}"
                    isSendingChatMessage = false
                }
            }
        }
    }

    fun clearChat() {
        chatMessages = emptyList()
        chatErrorMessage = null
        isSendingChatMessage = false
    }
}

data class ChatBotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PresetRole(
    val id: String,
    val name: String,
    val description: String,
    val instruction: String
)

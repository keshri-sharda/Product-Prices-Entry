package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private val repository = PriceListRepository(db.priceListDao)

    // Language state
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    // Navigation and screen states
    var currentScreen by mutableStateOf(Screen.LOGIN)

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

    // View layout option (Card vs Spreadsheet row/columns)
    var viewMode by mutableStateOf(ViewMode.TABLE)

    // Passcode lock security
    var isLoggedIn by mutableStateOf(false)
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
        foldersFlow
    ) { folder, query, allProducts, folders ->
        val folderProducts = if (folder != null) {
            val nestedFolderIds = getDescendantFolderIds(folder.id, folders)
            allProducts.filter { it.folderId in nestedFolderIds }
        } else {
            allProducts
        }
        if (query.isBlank()) {
            folderProducts
        } else {
            folderProducts.filter { isProductMatch(it, query) }
        }
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
            if (term.length >= 3) {
                val words = (prodName.split("[\\s_\\-\\.\\,\\(\\)]+".toRegex()) + 
                             prodDesc.split("[\\s_\\-\\.\\,\\(\\)]+".toRegex()) + 
                             prodSupplier.split("[\\s_\\-\\.\\,\\(\\)]+".toRegex())).filter { it.length >= 3 }
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
    var showProductDialog by mutableStateOf(false)
    var editingProduct by mutableStateOf<ProductEntity?>(null)
    var showDeleteFolderConfirm by mutableStateOf<FolderEntity?>(null)
    var showDeleteProductConfirm by mutableStateOf<ProductEntity?>(null)

    // Voice typing feedback
    var voiceError by mutableStateOf<String?>(null)
    var isVoiceListening by mutableStateOf(false)

    // --- CUSTOMIZED SETTINGS STATES ---
    var showCostAndMargins by mutableStateOf(true)
    var showSupplierInfo by mutableStateOf(true)
    var defaultTaxRate by mutableStateOf(0f)
    var customShopName by mutableStateOf("")
    var customShopTagline by mutableStateOf("")
    var appThemeMode by mutableStateOf("SYSTEM") // SYSTEM, LIGHT, DARK
    var appThemePalette by mutableStateOf("SAGE") // SAGE, BLUE, CRIMSON, TEAL, GOLDEN
    var fontSizeScale by mutableStateOf(1.0f) // 0.85f (Small), 1.0f (Normal), 1.15f (Large), 1.3f (Extra Large)

    init {
        // Retrieve language or lock presets
        val prefs = application.getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("selected_lang", "ENGLISH") ?: "ENGLISH"
        currentLanguage = if (savedLang == "HINDI") AppLanguage.HINDI else AppLanguage.ENGLISH
        savedPasscode = prefs.getString("security_passcode", "1234") ?: "1234"
        
        loginMethod = prefs.getString("login_method", "pin") ?: "pin"
        cloudUsername = prefs.getString("cloud_username", "") ?: ""
        cloudShopId = prefs.getString("cloud_shop_id", "") ?: ""

        // Retrieve customized settings
        showCostAndMargins = prefs.getBoolean("settings_show_cost_margins", true)
        showSupplierInfo = prefs.getBoolean("settings_show_supplier", true)
        defaultTaxRate = prefs.getFloat("settings_tax_rate", 0f)
        customShopName = prefs.getString("settings_shop_name", "") ?: ""
        customShopTagline = prefs.getString("settings_shop_tagline", "") ?: ""
        appThemeMode = prefs.getString("settings_theme_mode", "SYSTEM") ?: "SYSTEM"
        appThemePalette = prefs.getString("settings_theme_palette", "SAGE") ?: "SAGE"
        fontSizeScale = prefs.getFloat("settings_font_size_scale", 1.0f)
        
        val securityBypass = prefs.getBoolean("security_bypass", false)
        if (securityBypass) {
            isLoggedIn = true
            currentScreen = Screen.FOLDERS
        } else if (loginMethod == "cloud" && cloudShopId.isNotBlank()) {
            isLoggedIn = true
            currentScreen = Screen.FOLDERS
            syncFromCloud()
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

    fun selectFolder(folder: FolderEntity?) {
        selectedFolder = folder
        currentScreen = if (folder != null) Screen.PRODUCTS else Screen.FOLDERS
        searchQuery = "" // Clear search queries on fold navigation
    }

    // CRUD FOLDERS
    fun createFolder() {
        if (folderNameInput.isNotBlank()) {
            viewModelScope.launch {
                lastLocalWriteTime = System.currentTimeMillis()
                repository.insertFolder(folderNameInput.trim(), folderParentId)
                folderNameInput = ""
                showFolderDialog = false
                folderParentId = null
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
            }
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

    // Smart targeted voice input
    fun autoSimulateVoiceInput() {
        val samplePhrases = when (activeVoiceTargetField) {
            "name" -> if (currentLanguage == AppLanguage.HINDI) listOf("फॉर्च्यून बेसन ५०० ग्राम", "मैगी नूडल्स मसाला", "ताज महल टी २५० ग्राम") else listOf("Fortune Besan 500g", "Maggie Noodles Masala", "Taj Mahal Tea 250g")
            "description" -> if (currentLanguage == AppLanguage.HINDI) listOf("शुद्ध गाय का उत्तम उत्पाद", "दो मिनट स्नैक पैक", "प्रीमियम दार्जिलिंग चाय") else listOf("Pure cow milk product", "2 minute snack pack", "Premium Assam Blend Tea")
            "cost" -> listOf("45", "65", "120", "240")
            "selling" -> listOf("55", "80", "145", "280")
            "wholesale" -> listOf("50", "72", "130", "260")
            "supplier" -> if (currentLanguage == AppLanguage.HINDI) listOf("गुप्ता होलेसलर", "टाटा डिस्ट्रीब्यूटर्स", "स्थानीय मंडी") else listOf("Gupta Wholesaler", "Tata Distributors", "Local Mandi")
            "search" -> if (currentLanguage == AppLanguage.HINDI) listOf("बेसन", "मसाला", "चाय") else listOf("Besan", "Noodles", "Tea")
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

    fun syncToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (cloudShopId.isBlank()) {
            registerNewCloudShop(onComplete)
            return
        }
        isCloudSyncing = true
        cloudSyncMessage = "Uploading to Cloud..."
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val jsonPayload = getCloudJsonPayload()
                
                val bodyJson = JSONObject()
                bodyJson.put("name", "ShopPriceList_Group")
                val dataObj = JSONObject()
                dataObj.put("payload", jsonPayload)
                bodyJson.put("data", dataObj)
                
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = bodyJson.toString().toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url("https://api.restful-api.dev/objects/$cloudShopId")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .put(body)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        isCloudSyncing = false
                        cloudSyncMessage = "Successfully uploaded!"
                        onComplete(true, "Data pushed successfully.")
                    } else {
                        registerNewCloudShop(onComplete)
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
        isCloudSyncing = true
        cloudSyncMessage = "Creating Sync Channel..."
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val jsonPayload = getCloudJsonPayload()
                
                val bodyJson = JSONObject()
                bodyJson.put("name", "ShopPriceList_Group")
                val dataObj = JSONObject()
                dataObj.put("payload", jsonPayload)
                bodyJson.put("data", dataObj)
                
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = bodyJson.toString().toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url("https://api.restful-api.dev/objects")
                    .post(body)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val respBody = response.body?.string() ?: ""
                    if (response.isSuccessful && respBody.isNotBlank()) {
                        val respObj = JSONObject(respBody)
                        val newId = respObj.getString("id")
                        cloudShopId = newId
                        saveCloudCredentials()
                        isCloudSyncing = false
                        cloudSyncMessage = "Channel registered!"
                        onComplete(true, "Created new sync: $newId")
                    } else {
                        isCloudSyncing = false
                        cloudSyncMessage = "Failed to register channel."
                        onComplete(false, "Registration response failed.")
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
        if (cloudShopId.isBlank()) {
            onComplete(false, "No cloud channel registered.")
            return
        }
        isCloudSyncing = true
        cloudSyncMessage = "Fetching latest changes..."
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.restful-api.dev/objects/$cloudShopId?nocache=" + System.currentTimeMillis())
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .get()
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val respBody = response.body?.string() ?: ""
                    if (response.isSuccessful && respBody.isNotBlank()) {
                        val respObj = JSONObject(respBody)
                        val dataObj = respObj.optJSONObject("data")
                        val payload = dataObj?.optString("payload") ?: ""
                        if (payload.isNotBlank()) {
                            val success = importCloudJsonPayload(payload)
                            if (success) {
                                isCloudSyncing = false
                                cloudSyncMessage = "Synchronized!"
                                onComplete(true, "Downloaded entries.")
                            } else {
                                isCloudSyncing = false
                                cloudSyncMessage = "Corrupted server payload."
                                onComplete(false, "Failed to parse cloud payload.")
                            }
                        } else {
                            isCloudSyncing = false
                            cloudSyncMessage = "Empty remote payload."
                            onComplete(false, "Remote payload is blank.")
                        }
                    } else {
                        isCloudSyncing = false
                        cloudSyncMessage = "Remote sync group not found."
                        onComplete(false, "No cloud document exists for this ID.")
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
}

package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.FolderEntity
import com.example.data.ProductEntity
import com.example.data.BinFolder
import com.example.data.BinProduct
import com.example.ui.theme.*
import com.example.ui.translation.AppLanguage
import com.example.ui.translation.Translation
import com.example.ui.viewmodel.PriceListViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.ViewMode
import com.example.ui.viewmodel.ProductSortOption
import com.example.ui.voice.VoiceInputHelper
import kotlinx.coroutines.launch

enum class MarkupType {
    NONE, PERCENT, FIXED
}

data class FolderColorTheme(
    val background: Color,
    val primaryText: Color,
    val subText: Color,
    val iconTint: Color,
    val borderColor: Color
)

fun getFolderColorTheme(folderId: Any, isLightTheme: Boolean): FolderColorTheme {
    val lightPastels = listOf(
        Color(0xFFFFCCD1), // Vibrant Rose Pink
        Color(0xFFD1F2D9), // Vibrant Mint Green
        Color(0xFFD6EBFF), // Vibrant Sky Blue
        Color(0xFFFFE3CC), // Vibrant Peach Orange
        Color(0xFFEEDDFF), // Vibrant Lavender Purple
        Color(0xFFFFF9C4), // Vibrant Yellow Canary
        Color(0xFFD2F7F6), // Vibrant Robin Teal
        Color(0xFFFCD3F9), // Vibrant Delicate Orchid
        Color(0xFFFFDDD1), // Vibrant Spice Coral
        Color(0xFFE4F5D4), // Vibrant Sage Lime
        Color(0xFFD1F7FF), // Vibrant Clean Cyan
        Color(0xFFFFECB3)  // Vibrant Honey Amber
    )
    val lightBorderPastels = listOf(
        Color(0xFFFF8A95), // Vibrant Darker Rose
        Color(0xFF8CD8A3), // Vibrant Darker Mint
        Color(0xFF96CDFF), // Vibrant Darker Blue
        Color(0xFFFFB885), // Vibrant Darker Peach
        Color(0xFFD4B3FF), // Vibrant Darker Lavender
        Color(0xFFFFF176), // Vibrant Darker Yellow
        Color(0xFF94EBE6), // Vibrant Darker Teal
        Color(0xFFFA9FF2), // Vibrant Darker Orchid
        Color(0xFFFFA88F), // Vibrant Darker Coral
        Color(0xFFBFE59A), // Vibrant Darker Sage
        Color(0xFF8CEDFF), // Vibrant Darker Cyan
        Color(0xFFFFD54F)  // Vibrant Darker Amber
    )
    val lightDarkerTones = listOf(
        Color(0xFFC2185B), // Vibrant Rose Icon/Text
        Color(0xFF2E7D32), // Vibrant Mint Icon/Text
        Color(0xFF1565C0), // Vibrant Blue Icon/Text
        Color(0xFFE65100), // Vibrant Peach Icon/Text
        Color(0xFF6A1B9A), // Vibrant Lavender Icon/Text
        Color(0xFFF57F17), // Vibrant Yellow Icon/Text
        Color(0xFF00796B), // Vibrant Teal Icon/Text
        Color(0xFF8E24AA), // Vibrant Orchid Icon/Text
        Color(0xFFD84315), // Vibrant Coral Icon/Text
        Color(0xFF33691E), // Vibrant Sage Icon/Text
        Color(0xFF00838F), // Vibrant Cyan Icon/Text
        Color(0xFFFF8F00)  // Vibrant Amber Icon/Text
    )
    val darkPastels = listOf(
        Color(0xFF5C1C24), // Vibrant Dark Rose
        Color(0xFF114220), // Vibrant Dark Mint
        Color(0xFF162D5C), // Vibrant Dark Blue
        Color(0xFF5C3317), // Vibrant Dark Caramel
        Color(0xFF37175C), // Vibrant Dark Purple
        Color(0xFF4D410A), // Vibrant Dark Yellow
        Color(0xFF0F4743), // Vibrant Dark Teal
        Color(0xFF541C50), // Vibrant Dark Orchid
        Color(0xFF5C2318), // Vibrant Dark Coral
        Color(0xFF2C4C1B), // Vibrant Dark Sage
        Color(0xFF11475C), // Vibrant Dark Cyan
        Color(0xFF523D14)  // Vibrant Dark Amber
    )
    val darkBorderPastels = listOf(
        Color(0xFFFF5252), // Vibrant Dark Rose Border
        Color(0xFF69F0AE), // Vibrant Dark Mint Border
        Color(0xFF40C4FF), // Vibrant Dark Blue Border
        Color(0xFFFFAB40), // Vibrant Dark Caramel Border
        Color(0xFFE040FB), // Vibrant Dark Purple Border
        Color(0xFFFFD740), // Vibrant Dark Yellow Border
        Color(0xFF64FFDA), // Vibrant Dark Teal Border
        Color(0xFFEA80FC), // Vibrant Dark Orchid Border
        Color(0xFFFF6E40), // Vibrant Dark Coral Border
        Color(0xFFB2FF59), // Vibrant Dark Sage Border
        Color(0xFF18FFFF), // Vibrant Dark Cyan Border
        Color(0xFFFFC400)  // Vibrant Dark Amber Border
    )
    val darkLighterTones = listOf(
        Color(0xFFFFB2B7), // Bright Pink Tint
        Color(0xFFA1E3AF), // Bright Green Tint
        Color(0xFFABCFFF), // Bright Blue Tint
        Color(0xFFFFCE9F), // Bright Peach Tint
        Color(0xFFDFCDFF), // Bright Purple Tint
        Color(0xFFFFF2A3), // Bright Yellow Tint
        Color(0xFFA3EFF8), // Bright Cyan Tint
        Color(0xFFFFA9EB), // Bright Orchid Tint
        Color(0xFFFFB6A3), // Bright Coral Tint
        Color(0xFFC0E5A6), // Bright Sage Tint
        Color(0xFFA6E5D9), // Bright Teal Tint
        Color(0xFFFFF7A7)  // Bright Gold Tint
    )
    val hash = folderId.hashCode()
    val index = (hash % 12).let { if (it < 0) -it else it }
    
    return if (isLightTheme) {
        FolderColorTheme(
            background = lightPastels[index],
            primaryText = Color(0xFF1A1A1A),
            subText = lightDarkerTones[index].copy(alpha = 0.95f),
            iconTint = lightDarkerTones[index],
            borderColor = lightBorderPastels[index]
        )
    } else {
        FolderColorTheme(
            background = darkPastels[index],
            primaryText = Color(0xFFFFFFFF),
            subText = darkLighterTones[index],
            iconTint = darkBorderPastels[index],
            borderColor = darkBorderPastels[index]
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListAppScreen(viewModel: PriceListViewModel) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashOpeningAnimation(onAnimationFinished = { showSplash = false })
        return
    }

    val context = LocalContext.current
    val language = viewModel.currentLanguage
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAiScannerDialog by remember { mutableStateOf(false) }

    if (viewModel.showFirebaseConfigDialog) {
        var apiKeyText by remember { mutableStateOf(viewModel.firebaseApiKeyInput) }
        var projectIdText by remember { mutableStateOf(viewModel.firebaseProjectIdInput) }
        var appIdText by remember { mutableStateOf(viewModel.firebaseAppIdInput) }

        AlertDialog(
            onDismissRequest = { viewModel.showFirebaseConfigDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.HINDI) "कस्टम फायरबेस सेटिंग्स" else "Custom Firebase Settings",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "अपना स्वयं का फायरबेस/फायरस्टोर डेटाबेस उपयोग करने के लिए नीचे दी गई कुंजियाँ दर्ज करें। खाली रखने पर डिफ़ॉल्ट डेटाबेस का उपयोग किया जाएगा।"
                            else "Enter your Firebase Project credentials below to use your own private cloud database. Leave blank to reset to default.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("Firebase API Key") },
                        placeholder = { Text("e.g. AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("firebase_api_key_field")
                    )

                    OutlinedTextField(
                        value = projectIdText,
                        onValueChange = { projectIdText = it },
                        label = { Text("Firebase Project ID") },
                        placeholder = { Text("e.g. my-shop-backup") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("firebase_project_id_field")
                    )

                    OutlinedTextField(
                        value = appIdText,
                        onValueChange = { appIdText = it },
                        label = { Text("Firebase Application ID") },
                        placeholder = { Text("e.g. 1:123456:android:abcd") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("firebase_app_id_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveFirebaseConfig(apiKeyText, projectIdText, appIdText)
                        viewModel.showFirebaseConfigDialog = false
                        Toast.makeText(context, if (language == AppLanguage.HINDI) "सेटिंग्स सहेजी गईं और सिंक प्रारंभ हुआ!" else "Firebase keys updated and synced!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("save_firebase_config_btn")
                ) {
                    Text(if (language == AppLanguage.HINDI) "सहेजें" else "Save & Sync")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showFirebaseConfigDialog = false }
                ) {
                    Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    if (viewModel.showWelcomeUser) {
        AlertDialog(
            onDismissRequest = { viewModel.showWelcomeUser = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Welcome Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (language == AppLanguage.HINDI) "आपका स्वागत है!" else "Welcome back!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (viewModel.customShopName.isNotBlank()) viewModel.customShopName else viewModel.userPhoneNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI) "स्टोर मैनेजर के रूप में लॉगिन सफल रहा।" else "Successfully logged in as Store Manager.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.showWelcomeUser = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (language == AppLanguage.HINDI) "आगे बढ़ें" else "Proceed")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val folders by viewModel.foldersFlow.collectAsState()

    val backHandlerEnabled = showSettingsSheet || 
            showAiScannerDialog ||
            viewModel.showFolderDialog || 
            viewModel.showProductDialog || 
            viewModel.currentScreen == Screen.PRODUCTS || 
            viewModel.currentScreen == Screen.TRASH

    BackHandler(enabled = backHandlerEnabled) {
        when {
            showSettingsSheet -> {
                showSettingsSheet = false
            }
            showAiScannerDialog -> {
                showAiScannerDialog = false
            }
            viewModel.showFolderDialog -> {
                viewModel.showFolderDialog = false
            }
            viewModel.showProductDialog -> {
                viewModel.showProductDialog = false
            }
            viewModel.currentScreen == Screen.PRODUCTS -> {
                val activeFolder = viewModel.selectedFolder
                if (activeFolder != null) {
                    val parent = folders.find { it.id == activeFolder.parentId }
                    viewModel.selectFolder(parent)
                } else {
                    viewModel.currentScreen = Screen.FOLDERS
                }
            }
            viewModel.currentScreen == Screen.TRASH -> {
                viewModel.currentScreen = Screen.FOLDERS
            }
        }
    }

    // Setup speech helper
    val voiceHelper = remember(context) {
        VoiceInputHelper(
            context = context,
            onResult = { result ->
                viewModel.parseVoicePhrase(result)
                Toast.makeText(context, "${Translation.getString("copied_toast", language)}: $result", Toast.LENGTH_SHORT).show()
            },
            onError = { err ->
                viewModel.voiceError = err
                Toast.makeText(context, "Voice error: $err", Toast.LENGTH_LONG).show()
            },
            onListeningStateChange = { listening ->
                viewModel.isVoiceListening = listening
            }
        )
    }

    // Speech Audio permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val localeCode = if (viewModel.currentLanguage == AppLanguage.HINDI) "hi-IN" else "en-US"
                voiceHelper.startListening(localeCode)
            } else {
                Toast.makeText(context, "Microphone permission is required for voice typing.", Toast.LENGTH_LONG).show()
            }
        }
    )

    Scaffold(
        topBar = {
            if (viewModel.currentScreen != Screen.PRODUCTS) {
                TopAppBar(
                    title = {
                        if (viewModel.isLoggedIn) {
                            val displayName = if (viewModel.cloudUsername.isNotBlank()) {
                                viewModel.cloudUsername
                            } else if (viewModel.userPhoneNumber.isNotBlank()) {
                                viewModel.userPhoneNumber
                            } else {
                                if (language == AppLanguage.HINDI) "उपयोगकर्ता" else "User"
                            }
                            val welcomeText = if (language == AppLanguage.HINDI) {
                                "स्वागत है, $displayName"
                            } else {
                                "Welcome, $displayName"
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Classy Avatar with gradient background showing first letter of user name
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val firstLetter = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                                    Text(
                                        text = firstLetter,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                
                                Column {
                                    Text(
                                        text = welcomeText,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    color = if (viewModel.isPhoneConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "स्टोर मैनेजर • फोन सिंक चालू" else "Store Manager • Phone Sync Active",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = "App Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = Translation.getString("app_title", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    },
                    actions = {
                        if (viewModel.isLoggedIn && viewModel.currentScreen == Screen.FOLDERS) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                IconButton(
                                    onClick = { showSettingsSheet = true },
                                    modifier = Modifier.testTag("top_bar_settings_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {},
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (viewModel.currentScreen) {
                Screen.LOGIN -> LoginScreen(viewModel)
                Screen.FOLDERS -> FoldersScreen(
                    viewModel = viewModel,
                    onVoiceTypingClick = {
                        val recordPermission = Manifest.permission.RECORD_AUDIO
                        val hasPermission = ContextCompat.checkSelfPermission(context, recordPermission) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            val localeCode = if (viewModel.currentLanguage == AppLanguage.HINDI) "hi-IN" else "en-US"
                            voiceHelper.startListening(localeCode)
                        } else {
                            permissionLauncher.launch(recordPermission)
                        }
                    }
                )
                Screen.PRODUCTS -> ProductsScreen(
                    viewModel = viewModel,
                    onVoiceTypingClick = {
                        val recordPermission = Manifest.permission.RECORD_AUDIO
                        val hasPermission = ContextCompat.checkSelfPermission(context, recordPermission) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            val localeCode = if (viewModel.currentLanguage == AppLanguage.HINDI) "hi-IN" else "en-US"
                            voiceHelper.startListening(localeCode)
                        } else {
                            permissionLauncher.launch(recordPermission)
                        }
                    },
                    onStopVoiceListening = {
                        voiceHelper.stopListening()
                    },
                    onSettingsClick = { showSettingsSheet = true }
                )
                Screen.TRASH -> TrashScreen(viewModel = viewModel)
            }
        }
    }

    if (showAiScannerDialog) {
        SmartAiScannerDialog(
            viewModel = viewModel,
            onDismiss = { showAiScannerDialog = false }
        )
    }

    if (showSettingsSheet) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsSheet = false }
        )
    }



    if (viewModel.showSuccessDialog) {
        val speakText = if (viewModel.successDialogMessage == "Item added.") {
            if (language == AppLanguage.HINDI) "सामान सफलतापूर्वक जोड़ा गया।" else "Item was successfully added."
        } else {
            if (language == AppLanguage.HINDI) "सामान सफलतापूर्वक अद्यतन किया गया।" else "Item was successfully updated."
        }

        val dialogContext = LocalContext.current

        DisposableEffect(Unit) {
            if (viewModel.isAudioGuideEnabled) {
                // Play chime sound
                try {
                    val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                    toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_CONFIRM, 150)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Initialize TTS (only if audio guide is enabled)
            var tts: android.speech.tts.TextToSpeech? = null
            if (viewModel.isAudioGuideEnabled) {
                tts = android.speech.tts.TextToSpeech(dialogContext) { status ->
                    if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                        val locale = if (language == AppLanguage.HINDI) java.util.Locale("hi", "IN") else java.util.Locale("en", "IN")
                        try {
                            tts?.let { engine ->
                                val result = engine.setLanguage(locale)
                                if (result != android.speech.tts.TextToSpeech.LANG_MISSING_DATA && 
                                    result != android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                                    
                                    // Select high quality native Indian Voice
                                    try {
                                        val voices = engine.voices
                                        if (voices != null && voices.isNotEmpty()) {
                                            val preferredVoice = voices.firstOrNull { voice ->
                                                val voiceLocale = voice.locale
                                                voiceLocale.language == locale.language && 
                                                voiceLocale.country == locale.country && 
                                                !voice.isNetworkConnectionRequired
                                            } ?: voices.firstOrNull { voice ->
                                                val voiceLocale = voice.locale
                                                voiceLocale.language == locale.language && 
                                                voiceLocale.country == locale.country
                                            }
                                            preferredVoice?.let { engine.voice = it }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    
                                    engine.speak(speakText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "success_tts")
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }

            onDispose {
                try {
                    tts?.stop()
                    tts?.shutdown()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        Dialog(
            onDismissRequest = { viewModel.showSuccessDialog = false }
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .width(280.dp)
                    .padding(16.dp)
                    .testTag("save_success_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFE8F5E9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success checkmark",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = if (viewModel.successDialogMessage == "Item added.") {
                            Translation.getString("item_added", language)
                        } else {
                            Translation.getString("item_updated", language)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2200)
            viewModel.showSuccessDialog = false
        }
    }



    // Custom Add/Edit Folder dialog (top-level so accessible from all screens)
    if (viewModel.showFolderDialog) {
        val isEditing = viewModel.editingFolder != null
        AlertDialog(
            onDismissRequest = { 
                viewModel.showFolderDialog = false 
                viewModel.editingFolder = null
                viewModel.folderNameInput = ""
            },
            title = {
                Text(
                    text = if (isEditing) {
                        if (language == AppLanguage.HINDI) "व्यापारी का नाम बदलें" else "Rename / Edit Folder"
                    } else {
                        Translation.getString("add_folder", language)
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEditing) {
                            if (language == AppLanguage.HINDI) "नया व्यापारी नाम" else "New Folder Name"
                        } else {
                            Translation.getString("folder_name", language)
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    OutlinedTextField(
                        value = viewModel.folderNameInput,
                        onValueChange = { viewModel.folderNameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_name_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createFolder() },
                    modifier = Modifier.testTag("folder_submit_btn")
                ) {
                    Text(
                        text = if (isEditing) {
                            if (language == AppLanguage.HINDI) "सहेजें" else "Save"
                        } else {
                            Translation.getString("create", language)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.showFolderDialog = false 
                    viewModel.editingFolder = null
                    viewModel.folderNameInput = ""
                }) {
                    Text(text = Translation.getString("cancel", language))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Folder Options Dialog (displays when user long presses a folder)
    viewModel.showFolderOptionsFor?.let { folder ->
        AlertDialog(
            onDismissRequest = { viewModel.showFolderOptionsFor = null },
            title = {
                Text(
                    text = folder.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "इस व्यापारी के लिए विकल्प चुनें:" else "Choose options for this folder:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Rename / Edit option
                    Surface(
                        onClick = {
                            viewModel.showFolderOptionsFor = null
                            viewModel.editingFolder = folder
                            viewModel.folderNameInput = folder.name
                            viewModel.showFolderDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "नाम बदलें (संपादन)" else "Rename / Edit Name",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Delete option
                    Surface(
                        onClick = {
                            viewModel.showFolderOptionsFor = null
                            viewModel.showDeleteFolderConfirm = folder
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "व्यापारी सूची से हटाएं" else "Delete Folder",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.showFolderOptionsFor = null }) {
                    Text(text = Translation.getString("cancel", language))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete folder cascading dialog (top-level so accessible from all screens)
    viewModel.showDeleteFolderConfirm?.let { folder ->
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteFolderConfirm = null },
            title = {
                Text(
                    text = "${Translation.getString("delete", language)}: ${folder.name}?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(text = Translation.getString("delete_folder_confirm", language))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteFolderCascade(folder) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_folder_confirm_btn")
                ) {
                    Text(text = Translation.getString("delete", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteFolderConfirm = null }) {
                    Text(text = Translation.getString("cancel", language))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val sizeMin = minOf(width, height)
        val strokeWidth = sizeMin * 0.24f
        val radius = (sizeMin - strokeWidth) / 2f
        val center = Offset(width / 2f, height / 2f)
        val rect = Rect(center - Offset(radius, radius), center + Offset(radius, radius))
        
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)
        val blue = Color(0xFF4285F4)
        
        // Draw Red Arc (Top)
        drawArc(
            color = red,
            startAngle = 193f,
            sweepAngle = 114f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            topLeft = rect.topLeft,
            size = rect.size
        )
        // Draw Yellow Arc (Left)
        drawArc(
            color = yellow,
            startAngle = 105f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            topLeft = rect.topLeft,
            size = rect.size
        )
        // Draw Green Arc (Bottom)
        drawArc(
            color = green,
            startAngle = 0f,
            sweepAngle = 107f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            topLeft = rect.topLeft,
            size = rect.size
        )
        // Draw Blue Arc (Right-ish)
        drawArc(
            color = blue,
            startAngle = -45f,
            sweepAngle = 47f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            topLeft = rect.topLeft,
            size = rect.size
        )
        // Draw the horizontal bar of the 'G'
        drawRect(
            color = blue,
            topLeft = Offset(center.x, center.y - strokeWidth / 2f),
            size = Size(radius + strokeWidth / 2f, strokeWidth)
        )
    }
}

@Composable
fun LoginScreen(viewModel: PriceListViewModel) {
    val language = viewModel.currentLanguage
    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    var ownerNameInput by remember { mutableStateOf("") }
    var storeNameInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }

    val bgColor = MaterialTheme.colorScheme.background
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
    val ambientGradient = remember(bgColor, primaryContainer) {
        Brush.verticalGradient(
            colors = listOf(primaryContainer, bgColor)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientGradient)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Store Front Logo
            Card(
                modifier = Modifier
                    .size(110.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                        contentDescription = "Shop Front Logo",
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            Text(
                text = if (language == AppLanguage.HINDI) "प्रोफ़ाइल सेटअप" else "Create Your Profile",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (language == AppLanguage.HINDI) 
                    "शुरू करने के लिए अपनी प्रोफ़ाइल और स्टोर विवरण सेटअप करें" 
                    else "Set up your profile and store details to get started with your digital catalogs",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Profile setup card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "विवरण दर्ज करें" else "Enter Profile Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Input 1: Owner's Name
                    OutlinedTextField(
                        value = ownerNameInput,
                        onValueChange = { 
                            ownerNameInput = it 
                            validationError = ""
                        },
                        label = { Text(if (language == AppLanguage.HINDI) "आपका नाम (प्रोफ़ाइल मालिक)" else "Your Name (Profile Owner)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "User Icon", tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_owner_name_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Input 2: Store's Name
                    OutlinedTextField(
                        value = storeNameInput,
                        onValueChange = { 
                            storeNameInput = it 
                            validationError = ""
                        },
                        label = { Text(if (language == AppLanguage.HINDI) "स्टोर/दुकान का नाम" else "Store / Shop Name") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = "Store Icon", tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_store_name_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Store Mention live feedback block
                    if (storeNameInput.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "स्टोर नाम पूर्वावलोकन:" else "Store Name Preview:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = storeNameInput,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    if (validationError.isNotBlank()) {
                        Text(
                            text = validationError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            val nameTrim = ownerNameInput.trim()
                            val storeTrim = storeNameInput.trim()
                            if (nameTrim.isBlank()) {
                                validationError = if (language == AppLanguage.HINDI) "कृपया अपना नाम दर्ज करें।" else "Please enter your name."
                            } else if (storeTrim.isBlank()) {
                                validationError = if (language == AppLanguage.HINDI) "कृपया अपने स्टोर का नाम दर्ज करें।" else "Please enter your store's name."
                            } else {
                                keyboardController?.hide()
                                viewModel.handleProfileCreation(nameTrim, storeTrim)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("create_profile_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "प्रोफ़ाइल बनाएं और प्रवेश करें" else "Create Profile & Enter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(
    viewModel: PriceListViewModel,
    onVoiceTypingClick: () -> Unit
) {
    val context = LocalContext.current
    val language = viewModel.currentLanguage
    val folders by viewModel.foldersFlow.collectAsState()
    val allProducts by viewModel.allProductsFlow.collectAsState()

    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }

    DisposableEffect(context) {
        val speechEngine = android.speech.tts.TextToSpeech(context) { status ->
            // Engine initialized
        }
        tts = speechEngine
        onDispose {
            speechEngine.stop()
            speechEngine.shutdown()
        }
    }

    LaunchedEffect(viewModel.aiSearchAnswer) {
        val answer = viewModel.aiSearchAnswer
        if (!answer.isNullOrBlank() && viewModel.isTtsEnabled) {
            tts?.speak(answer, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
    val ambientGradient = remember(bgColor, primaryContainer) {
        Brush.verticalGradient(
            colors = listOf(primaryContainer, bgColor)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // AI-Enabled Search Bar (always visible below store manager / shop banner)
            val isVoiceActive = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "aiSearch"
            
            OutlinedTextField(
                value = viewModel.aiSearchQuery,
                onValueChange = { 
                    viewModel.aiSearchQuery = it
                    if (it.isBlank()) {
                        viewModel.aiSearchAnswer = null
                        viewModel.aiFilteredProductIds = emptyList()
                        viewModel.searchQuery = ""
                    }
                },
                placeholder = {
                    Text(
                        text = if (language == AppLanguage.HINDI) "अपना उत्पाद आप खोजें" else "Search for the product you want to find here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "AI Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        // Mic Button
                        IconButton(
                            onClick = {
                                viewModel.activeVoiceTargetField = "aiSearch"
                                onVoiceTypingClick()
                            },
                            modifier = Modifier.testTag("ai_mic_btn")
                        ) {
                            Icon(
                                imageVector = if (isVoiceActive) Icons.Default.MicNone else Icons.Default.Mic,
                                contentDescription = "Voice Input for AI Search",
                                tint = if (isVoiceActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Search Submit / AI Button
                        IconButton(
                            onClick = {
                                viewModel.performAiSearch(viewModel.aiSearchQuery)
                            },
                            modifier = Modifier.testTag("ai_submit_btn")
                        ) {
                            if (viewModel.isAiSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Search with AI",
                                    tint = Color(0xFFFF9800) // Beautiful Sparkle Gold
                                )
                            }
                        }
                        
                        // Clear Button
                        if (viewModel.aiSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                viewModel.aiSearchQuery = "" 
                                viewModel.aiSearchAnswer = null
                                viewModel.aiFilteredProductIds = emptyList()
                                viewModel.searchQuery = ""
                                viewModel.searchSuggestions = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(28.dp))
                    .testTag("ai_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // AI Assistant Speech & Text Answer Card
            viewModel.aiSearchAnswer?.let { answer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("ai_answer_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.2.dp, Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF9800), // Sparkling Orange/Gold
                            MaterialTheme.colorScheme.primary
                        )
                    ))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "AI सहायक उत्तर" else "AI Assistant Answer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Speak / Audio button
                            IconButton(
                                onClick = {
                                    tts?.speak(answer, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak answer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = answer,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        if (viewModel.searchSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "क्या आप ये ढूंढ रहे हैं?" else "Did you mean?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(viewModel.searchSuggestions) { suggestion ->
                                    Surface(
                                        onClick = {
                                            viewModel.aiSearchQuery = suggestion
                                            viewModel.performAiSearch(suggestion)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.testTag("suggestion_chip_$suggestion")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = suggestion,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enhanced search logic: displays matching folders and products across categories
            val isSearchActive = viewModel.searchQuery.isNotBlank()

            val searchedFolders = remember(folders, viewModel.searchQuery) {
                if (viewModel.searchQuery.isBlank()) {
                    emptyList<FolderEntity>()
                } else {
                    folders.filter { it.name.contains(viewModel.searchQuery, ignoreCase = true) }
                }
            }

            val searchedProducts = remember(allProducts, viewModel.searchQuery) {
                if (viewModel.searchQuery.isBlank()) {
                    emptyList<ProductEntity>()
                } else {
                    allProducts.filter { viewModel.isProductMatch(it, viewModel.searchQuery) }
                }
            }

            if (isSearchActive) {
                if (searchedFolders.isEmpty() && searchedProducts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "no results",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Translation.getString("no_results", language),
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val suggestions = remember(viewModel.searchQuery, allProducts) {
                            viewModel.getSearchSuggestions(viewModel.searchQuery, allProducts)
                        }
                        if (suggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "क्या आप ये ढूंढ रहे हैं?" else "Did you mean?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(suggestions) { suggestion ->
                                    Surface(
                                        onClick = {
                                            viewModel.searchQuery = suggestion
                                            viewModel.aiSearchQuery = suggestion
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.testTag("search_no_results_chip_$suggestion")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = suggestion,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (searchedFolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = Translation.getString("matching_folders", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            itemsIndexed(searchedFolders, key = { _, folder -> "sf_${folder.id}" }) { index, folder ->
                                val parentName = remember(folders, folder.parentId) {
                                    folders.find { it.id == folder.parentId }?.name
                                }
                                val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }
                                val folderColorTheme = getFolderColorTheme(folder.id, isLightTheme)

                                Card(
                                    onClick = { viewModel.selectFolder(folder) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("searched_folder_${folder.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = folderColorTheme.background),
                                    border = BorderStroke(1.dp, folderColorTheme.borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "Folder",
                                            tint = folderColorTheme.iconTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (viewModel.isFolderAutoNumberingEnabled) "${index + 1}. ${folder.name}" else folder.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = folderColorTheme.primaryText
                                            )
                                            if (parentName != null) {
                                                Text(
                                                    text = "In category: $parentName",
                                                    fontSize = 11.sp,
                                                    color = folderColorTheme.subText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (searchedProducts.isNotEmpty()) {
                            item {
                                Text(
                                    text = Translation.getString("matching_products", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(searchedProducts, key = { "sp_${it.id}" }) { product ->
                                ProductCardView(viewModel, product, language)
                            }
                        }
                    }
                }
            } else {
                val filteredFolders = remember(folders) {
                    folders.filter { it.parentId == null }
                }
                if (filteredFolders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderCopy,
                            contentDescription = "blank folders",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Translation.getString("no_folders", language),
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(filteredFolders, key = { _, folder -> folder.id }) { index, folder ->
                            val pCount = remember(allProducts, folder.id) {
                                allProducts.count { it.folderId == folder.id }
                            }
                            
                            val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }
                            val folderColorTheme = getFolderColorTheme(folder.id, isLightTheme)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .testTag("folder_card_${folder.id}")
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { viewModel.selectFolder(folder) },
                                        onLongClick = { viewModel.showFolderOptionsFor = folder }
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = folderColorTheme.background
                                ),
                                border = BorderStroke(1.5.dp, folderColorTheme.borderColor.copy(alpha = if (isLightTheme) 0.85f else 0.75f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isLightTheme) 3.dp else 0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    folderColorTheme.background,
                                                    folderColorTheme.background.copy(alpha = if (isLightTheme) 0.75f else 0.65f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (viewModel.isFolderAutoNumberingEnabled) "${index + 1}. ${folder.name}" else folder.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = folderColorTheme.primaryText
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$pCount ${Translation.getString("total_items", language)}",
                                            fontSize = 12.sp,
                                            color = if (isLightTheme) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Add Folder
        LargeFloatingActionButton(
            onClick = { viewModel.showFolderDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_folder_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Add Folder icon")
                Text(
                    text = Translation.getString("add_folder", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpreadsheetView(
    viewModel: PriceListViewModel,
    products: List<ProductEntity>,
    language: AppLanguage
) {
    val showCostAndMargins = viewModel.showCostAndMargins
    val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }

    // Proportional column weights so that everything fits the screen perfectly without side-scrolling
    val nameWeight = if (showCostAndMargins) 1.6f else 2.0f
    val costWeight = 0.9f
    val wholesaleWeight = 1.0f
    val retailWeight = 1.1f

    val activeFolder = viewModel.selectedFolder
    val folderColorTheme = if (activeFolder != null) {
        getFolderColorTheme(activeFolder.id, isLightTheme)
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Table Header row (Fixed, fits on screen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(folderColorTheme?.borderColor ?: MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language == AppLanguage.HINDI) "सामान" else "Product",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.weight(nameWeight),
                color = folderColorTheme?.primaryText ?: MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (showCostAndMargins) {
                Text(
                    text = if (language == AppLanguage.HINDI) "क्रय" else "Cost",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(costWeight),
                    textAlign = TextAlign.End,
                    color = folderColorTheme?.primaryText ?: MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = if (language == AppLanguage.HINDI) "थोक" else "Whole",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.weight(wholesaleWeight),
                textAlign = TextAlign.End,
                color = folderColorTheme?.primaryText ?: MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = if (language == AppLanguage.HINDI) "बिक्री" else "Retail",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.weight(retailWeight),
                textAlign = TextAlign.End,
                color = folderColorTheme?.primaryText ?: MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Table Body Rows
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            itemsIndexed(products, key = { _, it -> it.id }) { index, product ->
                val rowBgColor = if (isLightTheme) {
                    when (index % 3) {
                        0 -> Color(0xFFE8F5E9) // Soft light mint green
                        1 -> Color(0xFFFCE4EC) // Soft light pink/rose
                        else -> Color(0xFFE8EAF6) // Soft light blue/indigo
                    }
                } else {
                    when (index % 3) {
                        0 -> Color(0xFF142D1B) // Deep forest/pine green
                        1 -> Color(0xFF1C2341) // Deep navy blue
                        else -> Color(0xFF2F1A2C) // Deep dark amethyst/plum
                    }
                }
                val nameTextColor = if (isLightTheme) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)
                val subTextColor = if (isLightTheme) Color(0xFF555555) else Color(0xFFD1D5DB)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBgColor)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { viewModel.activeProductForOptions = product }
                        )
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Row product descriptive content in multiple columns
                    Column(
                        modifier = Modifier.weight(nameWeight)
                    ) {
                        Text(
                            text = "${index + 1}. ${product.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Serif,
                            color = nameTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isKg = product.priceUnit == "kg"
                            val unitColor = if (isKg) {
                                if (isLightTheme) Color(0xFF0284C7) else Color(0xFF38BDF8) // Vibrant sky blue/teal
                            } else {
                                if (isLightTheme) Color(0xFF7C3AED) else Color(0xFFA78BFA) // Vibrant deep purple
                            }
                            val unitText = if (isKg) "per kg" else "per pc"
                            val unitTextHindi = if (isKg) "प्रति किलो" else "प्रति नग"
                            Text(
                                text = if (language == AppLanguage.HINDI) unitTextHindi else unitText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = unitColor
                            )
                            if (product.boughtFrom.isNotEmpty() && viewModel.showSupplierInfo) {
                                Text(
                                    text = "• ${product.boughtFrom}",
                                    fontSize = 11.sp,
                                    color = subTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (product.description.isNotEmpty()) {
                            val listDescColor = if (isLightTheme) Color(0xFF4338CA) else Color(0xFF818CF8)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = product.description,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = listDescColor,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                    
                    // Cost Price (optional)
                    if (showCostAndMargins) {
                        Text(
                            text = "₹${product.costPrice}",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(costWeight),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            color = if (isLightTheme) Color(0xFFC5221F) else Color(0xFFF28B82)
                        )
                    }

                    // Wholesale Price
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(wholesaleWeight)
                    ) {
                        Text(
                            text = "₹${product.wholesalePrice}",
                            fontSize = 14.sp,
                            color = if (isLightTheme) Color(0xFF1D4ED8) else Color(0xFF60A5FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                        val margin = product.wholesalePrice - product.costPrice
                        if (margin > 0) {
                            Text(
                                text = "+₹${if (margin % 1.0 == 0.0) margin.toInt().toString() else String.format("%.1f", margin)}",
                                fontSize = 10.sp,
                                color = if (isLightTheme) Color(0xFF1E40AF) else Color(0xFF93C5FD).copy(alpha = 0.8f),
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    // Selling Retail Price
                    Text(
                        text = "₹${product.sellingPrice}",
                        fontSize = 14.sp,
                        modifier = Modifier.weight(retailWeight),
                        fontWeight = FontWeight.Bold,
                        color = if (isLightTheme) Color(0xFF2E7D32) else Color(0xFF81C784),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun InlineProductForm(
    viewModel: PriceListViewModel,
    language: AppLanguage,
    onVoiceTypingClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var customPercentVal by remember { mutableStateOf("") }
    var customFixedVal by remember { mutableStateOf("") }
    var isMarkupAssistantExpanded by remember { mutableStateOf(false) }
    var activeMarkupType by remember { mutableStateOf(MarkupType.NONE) }
    var activeMarkupValue by remember { mutableStateOf(0.0) }

    // Local state for Guided Step-by-Step Audio Assistant mode vs standard mode
    val isGuidedMode = false
    var currentWizardStep by remember { mutableStateOf(1) }

    val context = LocalContext.current
    var ttsInstance by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        ttsInstance = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Stop speaking immediately if audio guide is toggled off
    LaunchedEffect(viewModel.isAudioGuideEnabled) {
        if (!viewModel.isAudioGuideEnabled) {
            ttsInstance?.stop()
        }
    }

    val speakStepPrompt = { step: Int ->
        if (viewModel.isAudioGuideEnabled) {
            ttsInstance?.let { tts ->
                if (isTtsReady) {
                    val textToSpeak = when (step) {
                        1 -> if (language == AppLanguage.HINDI) "कृपया अपने उत्पाद का नाम बताएं।" else "Please state the name of your product."
                        2 -> if (language == AppLanguage.HINDI) "अपने उत्पाद का विवरण प्रदान करें।" else "Provide a description of your product."
                        3 -> if (language == AppLanguage.HINDI) "उत्पाद की खरीद तिथि दर्ज करें।" else "Enter the purchase date of the product."
                        4 -> if (language == AppLanguage.HINDI) "उत्पाद का खरीद मूल्य दर्ज करें।" else "Enter the purchase price of the product."
                        5 -> if (language == AppLanguage.HINDI) "थोक मूल्य के लिए आप कितने प्रतिशत या रुपये जोड़ना चाहते हैं?" else "For the wholesale price of the product, how much percentage or money do you want to add?"
                        6 -> if (language == AppLanguage.HINDI) "क्या यह प्रति नग या प्रति किलोग्राम है? और खुदरा बिक्री मूल्य क्या है?" else "Is the product priced per piece or per kilogram? And what is the retail selling price?"
                        else -> ""
                    }
                    if (textToSpeak.isNotEmpty()) {
                        val locale = if (language == AppLanguage.HINDI) java.util.Locale("hi", "IN") else java.util.Locale("en", "IN")
                        tts.language = locale
                        
                        // Select high quality native Indian Voice
                        try {
                            val voices = tts.voices
                            if (voices != null && voices.isNotEmpty()) {
                                val preferredVoice = voices.firstOrNull { voice ->
                                    val voiceLocale = voice.locale
                                    voiceLocale.language == locale.language && 
                                    voiceLocale.country == locale.country && 
                                    !voice.isNetworkConnectionRequired
                                } ?: voices.firstOrNull { voice ->
                                    val voiceLocale = voice.locale
                                    voiceLocale.language == locale.language && 
                                    voiceLocale.country == locale.country
                                }
                                preferredVoice?.let { tts.voice = it }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        tts.speak(textToSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "step_guidance")
                    }
                }
            }
        }
    }

    LaunchedEffect(currentWizardStep, isGuidedMode, isTtsReady) {
        if (isGuidedMode && isTtsReady) {
            viewModel.activeVoiceTargetField = when (currentWizardStep) {
                1 -> "name"
                2 -> "description"
                3 -> "supplier"
                4 -> "cost"
                5 -> "wholesale"
                6 -> "selling"
                else -> "name"
            }

            if (currentWizardStep == 3 && viewModel.productSupplierInput.isBlank()) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    viewModel.productSupplierInput = sdf.format(java.util.Date())
                } catch (e: Exception) {
                    viewModel.productSupplierInput = "2026-06-30"
                }
            }

            speakStepPrompt(currentWizardStep)
        }
    }

    LaunchedEffect(viewModel.productCostPriceInput) {
        val cost = viewModel.productCostPriceInput.toDoubleOrNull()
        if (cost != null && cost >= 0.0) {
            when (activeMarkupType) {
                MarkupType.PERCENT -> {
                    val calculated = cost * (1.0 + activeMarkupValue / 100.0)
                    viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) {
                        calculated.toInt().toString()
                    } else {
                        String.format("%.2f", calculated)
                    }
                }
                MarkupType.FIXED -> {
                    val calculated = cost + activeMarkupValue
                    viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) {
                        calculated.toInt().toString()
                    } else {
                        String.format("%.2f", calculated)
                    }
                }
                MarkupType.NONE -> {}
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("product_input_inline_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val heading = if (viewModel.editingProduct != null) {
                Translation.getString("edit", language)
            } else {
                Translation.getString("add_product", language)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = heading,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }



            if (isGuidedMode) {
                // Visual Progress Indicator showing remaining steps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "कदम $currentWizardStep का ६" else "Step $currentWizardStep of 6",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        val remaining = 6 - currentWizardStep
                        val remainingText = if (remaining == 0) {
                            if (language == AppLanguage.HINDI) "अंतिम कदम!" else "Final step!"
                        } else if (remaining == 1) {
                            if (language == AppLanguage.HINDI) "१ कदम शेष" else "1 step remaining"
                        } else {
                            if (language == AppLanguage.HINDI) "$remaining कदम शेष" else "$remaining steps remaining"
                        }
                        
                        Text(
                            text = remainingText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (remaining == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Horizontal progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = currentWizardStep.toFloat() / 6f)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Audio Voice Toggle inside Guided Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (viewModel.isAudioGuideEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Voice Guide Status",
                            tint = if (viewModel.isAudioGuideEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "आवाज़ मार्गदर्शिका सक्षम करें" else "Enable Voice Guide",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isAudioGuideEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = viewModel.isAudioGuideEnabled,
                        onCheckedChange = { viewModel.isAudioGuideEnabled = it },
                        modifier = Modifier
                            .scale(0.8f)
                            .testTag("inline_audio_guide_switch")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- GUIDED STEP BY STEP WIZARD FLOW ---
                when (currentWizardStep) {
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कदम १: उत्पाद का नाम बताएं" else "Step 1: Product Name",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "कृपया अपने उत्पाद का नाम बताएं।" else "Please state the name of your product.",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )

                            OutlinedTextField(
                                value = viewModel.productNameInput,
                                onValueChange = { viewModel.productNameInput = it },
                                label = { Text(Translation.getString("name_col", language)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inline_input_product_name"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "name"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "name"
                                            onVoiceTypingClick()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Name",
                                            tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कदम २: उत्पाद का विवरण" else "Step 2: Product Description",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "अपने उत्पाद का विवरण प्रदान करें।" else "Provide a description of your product.",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )

                            OutlinedTextField(
                                value = viewModel.productDescriptionInput,
                                onValueChange = { viewModel.productDescriptionInput = it },
                                label = { Text(Translation.getString("desc_col", language)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inline_input_product_desc"),
                                singleLine = false,
                                minLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "description"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "description"
                                            onVoiceTypingClick()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Description",
                                            tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                    3 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कदम ३: खरीद की तारीख" else "Step 3: Purchase Date",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "उत्पाद की खरीद तिथि दर्ज करें।" else "Enter the purchase date of the product.",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )

                            OutlinedTextField(
                                value = viewModel.productSupplierInput,
                                onValueChange = { viewModel.productSupplierInput = it },
                                label = { Text(if (language == AppLanguage.HINDI) "खरीद तिथि" else "Purchase Date") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inline_input_product_supplier"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "supplier"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "supplier"
                                            onVoiceTypingClick()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Date",
                                            tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "उदा. 2026-06-30 या आज की तारीख दर्ज करें।" else "e.g. 2026-06-30 or today's date is pre-filled.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    4 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कदम ४: खरीद मूल्य" else "Step 4: Purchase Price",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "उत्पाद का खरीद मूल्य दर्ज करें।" else "Enter the purchase price of the product.",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )

                            OutlinedTextField(
                                value = viewModel.productCostPriceInput,
                                onValueChange = { viewModel.productCostPriceInput = it },
                                label = { Text(Translation.getString("cost_col", language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inline_input_product_cost"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "cost"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "cost"
                                            onVoiceTypingClick()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Cost",
                                            tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                    5 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कदम ५: थोक मुनाफा जोड़ें" else "Step 5: Wholesale Price Markup",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "थोक मूल्य के लिए आप कितने प्रतिशत या रुपये जोड़ना चाहते हैं?" else "For the wholesale price of the product, how much percentage or money do you want to add?",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )

                            val costVal = viewModel.productCostPriceInput.toDoubleOrNull() ?: 0.0
                            Text(
                                text = "${if (language == AppLanguage.HINDI) "खरीद मूल्य (लागत)" else "Purchase Price (Cost)"}: ₹$costVal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customPercentVal,
                                    onValueChange = { newValue ->
                                        customPercentVal = newValue
                                        customFixedVal = ""
                                        val pct = newValue.toDoubleOrNull()
                                        if (pct != null && pct >= 0) {
                                            activeMarkupType = MarkupType.PERCENT
                                            activeMarkupValue = pct
                                            val calculated = costVal * (1.0 + pct / 100.0)
                                            viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) calculated.toInt().toString() else String.format("%.2f", calculated)
                                        } else {
                                            activeMarkupType = MarkupType.NONE
                                            activeMarkupValue = 0.0
                                        }
                                    },
                                    label = { Text(if (language == AppLanguage.HINDI) "मुनाफा (%)" else "Add Profit (%)") },
                                    placeholder = { Text("e.g. 15") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = customFixedVal,
                                    onValueChange = { newValue ->
                                        customFixedVal = newValue
                                        customPercentVal = ""
                                        val amt = newValue.toDoubleOrNull()
                                        if (amt != null && amt >= 0) {
                                            activeMarkupType = MarkupType.FIXED
                                            activeMarkupValue = amt
                                            val calculated = costVal + amt
                                            viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) calculated.toInt().toString() else String.format("%.2f", calculated)
                                        } else {
                                            activeMarkupType = MarkupType.NONE
                                            activeMarkupValue = 0.0
                                        }
                                    },
                                    label = { Text(if (language == AppLanguage.HINDI) "मुनाफा (₹)" else "Add Profit (₹)") },
                                    placeholder = { Text("e.g. 50") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = viewModel.productWholesalePriceInput,
                                onValueChange = {
                                    viewModel.productWholesalePriceInput = it
                                    activeMarkupType = MarkupType.NONE
                                    activeMarkupValue = 0.0
                                },
                                label = { Text(Translation.getString("whole_col", language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inline_input_product_wholesale"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "wholesale"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "wholesale"
                                            onVoiceTypingClick()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Wholesale",
                                            tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कदम ६: इकाई और बिक्री मूल्य" else "Step 6: Pricing Unit & Selling Price",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "क्या यह प्रति नग या प्रति किलोग्राम है? और खुदरा बिक्री मूल्य क्या है?" else "Is the product priced per piece or per kilogram? And what is the retail selling price?",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )

                            val isPiece = viewModel.productPriceUnitInput == "piece"
                            val isKg = viewModel.productPriceUnitInput == "kg"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .clickable { viewModel.productPriceUnitInput = "piece" }
                                        .testTag("inline_unit_piece_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPiece) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(
                                        width = if (isPiece) 2.dp else 1.dp,
                                        color = if (isPiece) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isPiece) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isPiece) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = Translation.getString("unit_per_piece", language),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isPiece) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .clickable { viewModel.productPriceUnitInput = "kg" }
                                        .testTag("inline_unit_kg_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isKg) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(
                                        width = if (isKg) 2.dp else 1.dp,
                                        color = if (isKg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isKg) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isKg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = Translation.getString("unit_per_kg", language),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isKg) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = viewModel.productSellingPriceInput,
                                onValueChange = { viewModel.productSellingPriceInput = it },
                                label = { Text(Translation.getString("sell_col", language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inline_input_product_selling"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "selling"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "selling"
                                            onVoiceTypingClick()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Selling",
                                            tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Guided Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { speakStepPrompt(currentWizardStep) },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Replay audio instructions",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentWizardStep > 1) {
                            OutlinedButton(
                                onClick = { currentWizardStep-- },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text(text = if (language == AppLanguage.HINDI) "पीछे" else "Back")
                            }
                        }

                        if (currentWizardStep < 6) {
                            Button(
                                onClick = {
                                    if (currentWizardStep == 1 && viewModel.productNameInput.isBlank()) {
                                        Toast.makeText(context, if (language == AppLanguage.HINDI) "उत्पाद का नाम आवश्यक है!" else "Product Name is required!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentWizardStep++
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text(text = if (language == AppLanguage.HINDI) "आगे" else "Next")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (viewModel.productNameInput.isNotBlank()) {
                                        viewModel.saveProduct()
                                        onSaveSuccess()
                                    } else {
                                        Toast.makeText(context, Translation.getString("validation_err", language), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(42.dp),
                                colors = if (language == AppLanguage.HINDI) {
                                    ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                if (language == AppLanguage.HINDI) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(text = Translation.getString("save", language))
                            }
                        }
                    }
                }

                // Bottom Step indicators dots
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..6).forEach { stepIdx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = 24.dp, height = 4.dp)
                                .background(
                                    color = if (stepIdx == currentWizardStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

            } else {
                // --- TRADITIONAL STANDARD DIALOG FORM MODE ---

                // Name and Supplier side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.productNameInput,
                        onValueChange = { viewModel.productNameInput = it },
                        label = { Text(Translation.getString("name_col", language)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inline_input_product_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.productSupplierInput,
                        onValueChange = { viewModel.productSupplierInput = it },
                        label = { Text(Translation.getString("supplier_col", language)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inline_input_product_supplier"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Description
                OutlinedTextField(
                    value = viewModel.productDescriptionInput,
                    onValueChange = { viewModel.productDescriptionInput = it },
                    label = { Text(Translation.getString("desc_col", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inline_input_product_desc"),
                    singleLine = false,
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                // Numerical Columns (Stacked): Cost Price, Wholesale Price, Retail Price
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.productCostPriceInput,
                            onValueChange = { viewModel.productCostPriceInput = it },
                            label = { Text(Translation.getString("cost_col", language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inline_input_product_cost"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                            )
                        )

                        OutlinedTextField(
                            value = viewModel.productWholesalePriceInput,
                            onValueChange = { newValue ->
                                viewModel.productWholesalePriceInput = newValue
                                // Reset active markup helper tracking if user edits manually
                                activeMarkupType = MarkupType.NONE
                                activeMarkupValue = 0.0
                                if (customPercentVal.isNotEmpty() || customFixedVal.isNotEmpty()) {
                                    customPercentVal = ""
                                    customFixedVal = ""
                                }
                            },
                            label = { Text(Translation.getString("whole_col", language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inline_input_product_wholesale"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                focusedLabelColor = MaterialTheme.colorScheme.tertiary,
                                focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f)
                            )
                        )
                    }

                    // Openable / Collapsible Markup calculation helper
                    val costPriceVal = remember(viewModel.productCostPriceInput) {
                        viewModel.productCostPriceInput.toDoubleOrNull()
                    }
                    if (costPriceVal != null && costPriceVal > 0.0) {
                        // Expand/Collapse toggle button
                        TextButton(
                            onClick = { isMarkupAssistantExpanded = !isMarkupAssistantExpanded },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMarkupAssistantExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Assistant",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (language == AppLanguage.HINDI) {
                                        if (isMarkupAssistantExpanded) "गणना सहायक छिपाएं ✖" else "लागत मूल्य से थोक मुनाफा जोड़ें (मुनाफा % या रुपये) ⚙️"
                                    } else {
                                        if (isMarkupAssistantExpanded) "Hide Assistant" else "Add wholesale profit markup (Calculator) ⚙️"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // The expandable Assistant panel itself
                        AnimatedVisibility(
                            visible = isMarkupAssistantExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कस्टम थोक मुनाफा जोड़ें (लागत मूल्य: ₹$costPriceVal)" else "Add Wholesale Profit Markup (Cost: ₹$costPriceVal)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                // Custom calculation fields (percentage & added rupees)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customPercentVal,
                                        onValueChange = { newValue ->
                                            customPercentVal = newValue
                                            customFixedVal = ""
                                            val pct = newValue.toDoubleOrNull()
                                            if (pct != null && pct >= 0) {
                                                activeMarkupType = MarkupType.PERCENT
                                                activeMarkupValue = pct
                                                val calculated = costPriceVal * (1.0 + pct / 100.0)
                                                viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) calculated.toInt().toString() else String.format("%.2f", calculated)
                                            } else {
                                                activeMarkupType = MarkupType.NONE
                                                activeMarkupValue = 0.0
                                            }
                                        },
                                        label = { Text(if (language == AppLanguage.HINDI) "प्रतिशत (%)" else "Custom % Profit") },
                                        placeholder = { Text("e.g. 10") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = customFixedVal,
                                        onValueChange = { newValue ->
                                            customFixedVal = newValue
                                            customPercentVal = ""
                                            val amt = newValue.toDoubleOrNull()
                                            if (amt != null && amt >= 0) {
                                                activeMarkupType = MarkupType.FIXED
                                                activeMarkupValue = amt
                                                val calculated = costPriceVal + amt
                                                viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) calculated.toInt().toString() else String.format("%.2f", calculated)
                                            } else {
                                                activeMarkupType = MarkupType.NONE
                                                activeMarkupValue = 0.0
                                            }
                                        },
                                        label = { Text(if (language == AppLanguage.HINDI) "रुपये (₹)" else "Custom ₹ Profit") },
                                        placeholder = { Text("e.g. 50") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.productSellingPriceInput,
                        onValueChange = { viewModel.productSellingPriceInput = it },
                        label = { Text(Translation.getString("sell_col", language)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inline_input_product_selling"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Translation.getString("price_unit_label", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isPiece = viewModel.productPriceUnitInput == "piece"
                        val isKg = viewModel.productPriceUnitInput == "kg"

                        OutlinedButton(
                            onClick = { viewModel.productPriceUnitInput = "piece" },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("inline_unit_piece_btn_std"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isPiece) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isPiece) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isPiece) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isPiece) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "selected",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = Translation.getString("unit_per_piece", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.productPriceUnitInput = "kg" },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("inline_unit_kg_btn_std"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isKg) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isKg) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isKg) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isKg) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "selected",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = Translation.getString("unit_per_kg", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inline_product_cancel_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = Translation.getString("cancel", language))
                    }

                    Button(
                        onClick = {
                            if (viewModel.productNameInput.isNotBlank()) {
                                viewModel.saveProduct()
                                onSaveSuccess()
                            } else {
                                Toast.makeText(context, Translation.getString("validation_err", language), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inline_product_save_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (language == AppLanguage.HINDI) {
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        if (language == AppLanguage.HINDI) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(text = Translation.getString("save", language))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(
    viewModel: PriceListViewModel,
    onVoiceTypingClick: () -> Unit,
    onStopVoiceListening: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onAiScanClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val language = viewModel.currentLanguage
    val activeFolder = viewModel.selectedFolder ?: return
    val products by viewModel.filteredProductsFlow.collectAsState(initial = emptyList())
    val folders by viewModel.foldersFlow.collectAsState()
    val allProducts by viewModel.allProductsFlow.collectAsState()
    var showMoreFolderOptions by remember { mutableStateOf(false) }

    val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }
    val folderColorTheme = remember(activeFolder.id, isLightTheme) {
        getFolderColorTheme(activeFolder.id, isLightTheme)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(folderColorTheme.background)
    ) {
        // Navigation header back arrow
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = folderColorTheme.background),
            border = BorderStroke(0.5.dp, folderColorTheme.borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val parent = folders.find { it.id == activeFolder.parentId }
                        viewModel.selectFolder(parent)
                    },
                    modifier = Modifier.testTag("back_to_folders")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "back",
                        tint = folderColorTheme.iconTint
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Folder",
                            tint = folderColorTheme.iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val activeFolderIdx = remember(folders, activeFolder.id) {
                            folders.indexOfFirst { it.id == activeFolder.id }
                        }
                        Text(
                            text = if (viewModel.isFolderAutoNumberingEnabled && activeFolderIdx != -1) "${activeFolderIdx + 1}. ${activeFolder.name}" else activeFolder.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = folderColorTheme.primaryText
                        )
                    }
                    Text(
                        text = "${products.size} ${Translation.getString("total_items", language)}",
                        fontSize = 12.sp,
                        color = if (isLightTheme) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Layout option selector (Card View vs Rows/Columns Spreadsheet table)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(folderColorTheme.borderColor.copy(alpha = 0.3f))
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.viewMode = ViewMode.CARD },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.viewMode == ViewMode.CARD) folderColorTheme.iconTint else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Card View",
                            tint = if (viewModel.viewMode == ViewMode.CARD) folderColorTheme.background else folderColorTheme.primaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.viewMode = ViewMode.TABLE },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.viewMode == ViewMode.TABLE) folderColorTheme.iconTint else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Table View",
                            tint = if (viewModel.viewMode == ViewMode.TABLE) folderColorTheme.background else folderColorTheme.primaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        viewModel.isSearchExpanded = !viewModel.isSearchExpanded
                    },
                    modifier = Modifier.testTag("toggle_products_search_btn")
                ) {
                    Icon(
                        imageVector = if (viewModel.isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = folderColorTheme.iconTint
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    IconButton(
                        onClick = { showMoreFolderOptions = true },
                        modifier = Modifier.testTag("folder_more_options_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = folderColorTheme.iconTint
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreFolderOptions,
                        onDismissRequest = { showMoreFolderOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "खोजें" else "Search Items") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            onClick = {
                                viewModel.isSearchExpanded = !viewModel.isSearchExpanded
                                showMoreFolderOptions = false
                            }
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = if (language == AppLanguage.HINDI) "क्रमबद्ध करें (Sort By):" else "Sort By:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "नाम (A से Z)" else "Name (A to Z)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.SortByAlpha, 
                                    contentDescription = null,
                                    tint = if (viewModel.appProductSortOption == ProductSortOption.NAME_ASC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = {
                                viewModel.updateProductSortOption(ProductSortOption.NAME_ASC)
                                showMoreFolderOptions = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "नाम (Z से A)" else "Name (Z to A)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.SortByAlpha, 
                                    contentDescription = null,
                                    tint = if (viewModel.appProductSortOption == ProductSortOption.NAME_DESC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = {
                                viewModel.updateProductSortOption(ProductSortOption.NAME_DESC)
                                showMoreFolderOptions = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "मूल्य (अधिक से कम)" else "Value (High to Low)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.TrendingDown, 
                                    contentDescription = null,
                                    tint = if (viewModel.appProductSortOption == ProductSortOption.VALUE_HIGH_TO_LOW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = {
                                viewModel.updateProductSortOption(ProductSortOption.VALUE_HIGH_TO_LOW)
                                showMoreFolderOptions = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "मूल्य (कम से अधिक)" else "Value (Low to High)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.TrendingUp, 
                                    contentDescription = null,
                                    tint = if (viewModel.appProductSortOption == ProductSortOption.VALUE_LOW_TO_HIGH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = {
                                viewModel.updateProductSortOption(ProductSortOption.VALUE_LOW_TO_HIGH)
                                showMoreFolderOptions = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "खरीद तिथि (नवीनतम)" else "Date Purchased (Newest)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.CalendarToday, 
                                    contentDescription = null,
                                    tint = if (viewModel.appProductSortOption == ProductSortOption.DATE_PURCHASED_NEWEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = {
                                viewModel.updateProductSortOption(ProductSortOption.DATE_PURCHASED_NEWEST)
                                showMoreFolderOptions = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "खरीद तिथि (पुराना)" else "Date Purchased (Oldest)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.CalendarToday, 
                                    contentDescription = null,
                                    tint = if (viewModel.appProductSortOption == ProductSortOption.DATE_PURCHASED_OLDEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = {
                                viewModel.updateProductSortOption(ProductSortOption.DATE_PURCHASED_OLDEST)
                                showMoreFolderOptions = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "जोड़ने की तिथि (नवीनतम)" else "Date Added (Newest)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Schedule, 
                                    contentDescription = null,
                                    tint = if (viewModel.appProductSortOption == ProductSortOption.DATE_ADDED_NEWEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = {
                                viewModel.updateProductSortOption(ProductSortOption.DATE_ADDED_NEWEST)
                                showMoreFolderOptions = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.HINDI) "जोड़ने की तिथि (पुराना)" else "Date Added (Oldest)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Schedule, 
                                    contentDescription = null,
                                    tint = if (viewModel.appProductSortOption == ProductSortOption.DATE_ADDED_OLDEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = {
                                viewModel.updateProductSortOption(ProductSortOption.DATE_ADDED_OLDEST)
                                showMoreFolderOptions = false
                            }
                        )
                    }
                }
            }
        }


                // Search Bar in category (collapsible)
                AnimatedVisibility(
                    visible = viewModel.isSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.searchQuery = it },
                        placeholder = {
                            Text(
                                text = Translation.getString("search_placeholder", language),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "search",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "search"
                                IconButton(
                                    onClick = {
                                        viewModel.activeVoiceTargetField = "search"
                                        onVoiceTypingClick()
                                    },
                                    modifier = Modifier.testTag("mic_btn_products_search")
                                ) {
                                    Icon(
                                        imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                        contentDescription = "Voice Input Search",
                                        tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (viewModel.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(28.dp))
                            .testTag("products_search_bar"),
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                    )
                }



                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (products.isEmpty()) {
                        val isSearchActiveLocally = viewModel.searchQuery.isNotBlank()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSearchActiveLocally) Icons.Default.Search else Icons.Outlined.FormatListBulleted,
                                contentDescription = "No products",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isSearchActiveLocally) Translation.getString("no_results", language) else Translation.getString("no_products", language),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                            if (isSearchActiveLocally) {
                                val suggestions = remember(viewModel.searchQuery, allProducts) {
                                    viewModel.getSearchSuggestions(viewModel.searchQuery, allProducts)
                                }
                                if (suggestions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "क्या आप ये ढूंढ रहे हैं?" else "Did you mean?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(suggestions) { suggestion ->
                                            Surface(
                                                onClick = {
                                                    viewModel.searchQuery = suggestion
                                                    viewModel.aiSearchQuery = suggestion
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                                modifier = Modifier.testTag("product_search_no_results_chip_$suggestion")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = suggestion,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Display view according to choice
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (viewModel.viewMode == ViewMode.CARD) {
                                // Card View Listing
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(products, key = { _, it -> it.id }) { index, product ->
                                        ProductCardView(viewModel, product, language, index + 1)
                                    }
                                }
                            } else {
                                // Spreadsheet Grid Table View
                                SpreadsheetView(viewModel, products, language)
                            }
                        }
                    }

                    // Add Product Floating Button overlays perfectly in the bottom corner of the Box
                    ExtendedFloatingActionButton(
                        onClick = { 
                            viewModel.openAddProduct()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .testTag("add_product_fab"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(20.dp),
                        icon = { Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add Product") },
                        text = {
                            Text(
                                text = Translation.getString("add_product", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    )
                }
    }

    ProductsDialogs(
        viewModel = viewModel,
        onVoiceTypingClick = onVoiceTypingClick,
        onStopVoiceListening = onStopVoiceListening
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductsDialogs(
    viewModel: PriceListViewModel,
    onVoiceTypingClick: () -> Unit,
    onStopVoiceListening: () -> Unit
) {
    val context = LocalContext.current
    val language = viewModel.currentLanguage

    // Product options menu (Edit or Delete) triggered on long-press
    viewModel.activeProductForOptions?.let { product ->
        AlertDialog(
            onDismissRequest = { viewModel.activeProductForOptions = null },
            title = {
                Text(
                    text = if (language == AppLanguage.HINDI) "उत्पाद विकल्प" else "Product Options",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    
                    // Edit option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.activeProductForOptions = null
                                viewModel.openEditProduct(product)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "उत्पाद का संपादन करें (Edit)" else "Edit Product",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Remove option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.activeProductForOptions = null
                                viewModel.showDeleteProductConfirm = product
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Product",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "उत्पाद हटाएं (Remove)" else "Remove Product",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.activeProductForOptions = null }) {
                    Text(text = Translation.getString("cancel", language))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Billing confirmation dialog for product deletion (Hindi / English with far buttons)
    viewModel.showDeleteProductConfirm?.let { product ->
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteProductConfirm = null },
            title = {
                Text(
                    text = if (language == AppLanguage.HINDI) "उत्पाद हटाएं?" else "Delete Product?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = Translation.getString("delete_product_confirm", language) + "\n\n" + product.name,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        viewModel.showDeleteProductConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_product_confirm_btn")
                ) {
                    Text(text = Translation.getString("yes", language))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showDeleteProductConfirm = null },
                    modifier = Modifier.testTag("delete_product_cancel_btn")
                ) {
                    Text(text = Translation.getString("no", language))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Add / Edit Product Multi-Column input form dialog
    if (viewModel.showProductDialog) {
        var customPercentVal by remember { mutableStateOf("") }
        var customFixedVal by remember { mutableStateOf("") }
        var isMarkupAssistantExpanded by remember { mutableStateOf(false) }
        var activeMarkupType by remember { mutableStateOf(MarkupType.NONE) }
        var activeMarkupValue by remember { mutableStateOf(0.0) }

        // Local state for Guided Step-by-Step Audio Assistant mode vs standard mode
        var isGuidedMode by remember { mutableStateOf(true) }
        var currentWizardStep by remember { mutableStateOf(1) }

        // Setup TextToSpeech for Guided voice prompt instructions
        val context = LocalContext.current
        var ttsInstance by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
        var isTtsReady by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            val tts = android.speech.tts.TextToSpeech(context) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    isTtsReady = true
                }
            }
            ttsInstance = tts
            onDispose {
                tts.stop()
                tts.shutdown()
            }
        }

        // Stop speaking immediately if audio guide is toggled off
        LaunchedEffect(viewModel.isAudioGuideEnabled) {
            if (!viewModel.isAudioGuideEnabled) {
                ttsInstance?.stop()
            }
        }

        val speakStepPrompt = { step: Int ->
            if (viewModel.isAudioGuideEnabled) {
                ttsInstance?.let { tts ->
                    if (isTtsReady) {
                        val textToSpeak = when (step) {
                            1 -> if (language == AppLanguage.HINDI) "कृपया अपने उत्पाद का नाम बताएं।" else "Please state the name of your product."
                            2 -> if (language == AppLanguage.HINDI) "अपने उत्पाद का विवरण प्रदान करें।" else "Provide a description of your product."
                            3 -> if (language == AppLanguage.HINDI) "उत्पाद की खरीद तिथि दर्ज करें।" else "Enter the purchase date of the product."
                            4 -> if (language == AppLanguage.HINDI) "उत्पाद का खरीद मूल्य दर्ज करें।" else "Enter the purchase price of the product."
                            5 -> if (language == AppLanguage.HINDI) "थोक मूल्य के लिए आप कितने प्रतिशत या रुपये जोड़ना चाहते हैं?" else "For the wholesale price of the product, how much percentage or money do you want to add?"
                            6 -> if (language == AppLanguage.HINDI) "क्या यह प्रति नग या प्रति किलोग्राम है? और खुदरा बिक्री मूल्य क्या है?" else "Is the product priced per piece or per kilogram? And what is the retail selling price?"
                            else -> ""
                        }
                        if (textToSpeak.isNotEmpty()) {
                            val locale = if (language == AppLanguage.HINDI) java.util.Locale("hi", "IN") else java.util.Locale("en", "IN")
                            tts.language = locale
                            
                            // Select high quality native Indian Voice
                            try {
                                val voices = tts.voices
                                if (voices != null && voices.isNotEmpty()) {
                                    val preferredVoice = voices.firstOrNull { voice ->
                                        val voiceLocale = voice.locale
                                        voiceLocale.language == locale.language && 
                                        voiceLocale.country == locale.country && 
                                        !voice.isNetworkConnectionRequired
                                    } ?: voices.firstOrNull { voice ->
                                        val voiceLocale = voice.locale
                                        voiceLocale.language == locale.language && 
                                        voiceLocale.country == locale.country
                                    }
                                    preferredVoice?.let { tts.voice = it }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            tts.speak(textToSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "step_guidance")
                        }
                    }
                }
            }
        }

        // Auto trigger audio guidance on step change or when entering guided mode
        LaunchedEffect(currentWizardStep, isGuidedMode, isTtsReady) {
            if (isGuidedMode && isTtsReady) {
                // Set the active target field for voice recognition automatically based on step!
                viewModel.activeVoiceTargetField = when (currentWizardStep) {
                    1 -> "name"
                    2 -> "description"
                    3 -> "supplier"
                    4 -> "cost"
                    5 -> "wholesale"
                    6 -> "selling"
                    else -> "name"
                }

                // Pre-fill today's date in Step 3 if it's currently empty
                if (currentWizardStep == 3 && viewModel.productSupplierInput.isBlank()) {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        viewModel.productSupplierInput = sdf.format(java.util.Date())
                    } catch (e: Exception) {
                        viewModel.productSupplierInput = "2026-06-30"
                    }
                }

                speakStepPrompt(currentWizardStep)
            }
        }

        // Automatically sync wholesale price when cost price changes, if a markup calculation helper is selected
        LaunchedEffect(viewModel.productCostPriceInput) {
            val cost = viewModel.productCostPriceInput.toDoubleOrNull()
            if (cost != null && cost >= 0.0) {
                when (activeMarkupType) {
                    MarkupType.PERCENT -> {
                        val calculated = cost * (1.0 + activeMarkupValue / 100.0)
                        viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) {
                            calculated.toInt().toString()
                        } else {
                            String.format("%.2f", calculated)
                        }
                    }
                    MarkupType.FIXED -> {
                        val calculated = cost + activeMarkupValue
                        viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) {
                            calculated.toInt().toString()
                        } else {
                            String.format("%.2f", calculated)
                        }
                    }
                    MarkupType.NONE -> {
                        // Manual overrides remain untouched
                    }
                }
            }
        }

        Dialog(onDismissRequest = { viewModel.showProductDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 12.dp)
                    .testTag("product_input_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val dialogHeading = if (viewModel.editingProduct != null) {
                        Translation.getString("edit", language)
                    } else {
                        Translation.getString("add_product", language)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dialogHeading,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Toggle for Guided Mode vs Standard mode
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "एक-एक करके" else "Step-by-Step",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGuidedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Switch(
                                checked = isGuidedMode,
                                onCheckedChange = { isGuidedMode = it },
                                modifier = Modifier
                                    .scale(0.8f)
                                    .testTag("dialog_guided_mode_toggle_switch")
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    if (isGuidedMode) {
                        // Visual Progress Indicator showing remaining steps
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कदम $currentWizardStep का ६" else "Step $currentWizardStep of 6",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                val remaining = 6 - currentWizardStep
                                val remainingText = if (remaining == 0) {
                                    if (language == AppLanguage.HINDI) "अंतिम कदम!" else "Final step!"
                                } else if (remaining == 1) {
                                    if (language == AppLanguage.HINDI) "१ कदम शेष" else "1 step remaining"
                                } else {
                                    if (language == AppLanguage.HINDI) "$remaining कदम शेष" else "$remaining steps remaining"
                                }
                                
                                Text(
                                    text = remainingText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (remaining == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                                )
                            }

                            // Horizontal progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = currentWizardStep.toFloat() / 6f)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            ),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Audio Voice Toggle inside Guided Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (viewModel.isAudioGuideEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Voice Guide Status",
                                    tint = if (viewModel.isAudioGuideEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (language == AppLanguage.HINDI) "आवाज़ मार्गदर्शिका सक्षम करें" else "Enable Voice Guide",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewModel.isAudioGuideEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = viewModel.isAudioGuideEnabled,
                                onCheckedChange = { viewModel.isAudioGuideEnabled = it },
                                modifier = Modifier
                                    .scale(0.8f)
                                    .testTag("dialog_audio_guide_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // --- GUIDED STEP BY STEP WIZARD FLOW ---
                        when (currentWizardStep) {
                            1 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कदम १: उत्पाद का नाम बताएं" else "Step 1: Product Name",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कृपया अपने उत्पाद का नाम बताएं।" else "Please state the name of your product.",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )

                                    OutlinedTextField(
                                        value = viewModel.productNameInput,
                                        onValueChange = { viewModel.productNameInput = it },
                                        label = { Text(Translation.getString("name_col", language)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_product_name"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "name"
                                            IconButton(
                                                onClick = {
                                                    viewModel.activeVoiceTargetField = "name"
                                                    onVoiceTypingClick()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                                    contentDescription = "Voice Input Name",
                                                    tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            2 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कदम २: उत्पाद का विवरण" else "Step 2: Product Description",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "अपने उत्पाद का विवरण प्रदान करें।" else "Provide a description of your product.",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )

                                    OutlinedTextField(
                                        value = viewModel.productDescriptionInput,
                                        onValueChange = { viewModel.productDescriptionInput = it },
                                        label = { Text(Translation.getString("desc_col", language)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_product_desc"),
                                        singleLine = false,
                                        minLines = 3,
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "description"
                                            IconButton(
                                                onClick = {
                                                    viewModel.activeVoiceTargetField = "description"
                                                    onVoiceTypingClick()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                                    contentDescription = "Voice Input Description",
                                                    tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            3 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कदम ३: खरीद की तारीख" else "Step 3: Purchase Date",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "उत्पाद की खरीद तिथि दर्ज करें।" else "Enter the purchase date of the product.",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )

                                    OutlinedTextField(
                                        value = viewModel.productSupplierInput,
                                        onValueChange = { viewModel.productSupplierInput = it },
                                        label = { Text(if (language == AppLanguage.HINDI) "खरीद तिथि" else "Purchase Date") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_product_supplier"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "supplier"
                                            IconButton(
                                                onClick = {
                                                    viewModel.activeVoiceTargetField = "supplier"
                                                    onVoiceTypingClick()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                                    contentDescription = "Voice Input Date",
                                                    tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "उदा. 2026-06-30 या आज की तारीख दर्ज करें।" else "e.g. 2026-06-30 or today's date is pre-filled.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            4 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कदम ४: खरीद मूल्य" else "Step 4: Purchase Price",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "उत्पाद का खरीद मूल्य दर्ज करें।" else "Enter the purchase price of the product.",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )

                                    OutlinedTextField(
                                        value = viewModel.productCostPriceInput,
                                        onValueChange = { viewModel.productCostPriceInput = it },
                                        label = { Text(Translation.getString("cost_col", language)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_product_cost"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "cost"
                                            IconButton(
                                                onClick = {
                                                    viewModel.activeVoiceTargetField = "cost"
                                                    onVoiceTypingClick()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                                    contentDescription = "Voice Input Cost",
                                                    tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            5 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कदम ५: थोक मुनाफा जोड़ें" else "Step 5: Wholesale Price Markup",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "थोक मूल्य के लिए आप कितने प्रतिशत या रुपये जोड़ना चाहते हैं?" else "For the wholesale price of the product, how much percentage or money do you want to add?",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )

                                    val costVal = viewModel.productCostPriceInput.toDoubleOrNull() ?: 0.0
                                    Text(
                                        text = "${if (language == AppLanguage.HINDI) "खरीद मूल्य (लागत)" else "Purchase Price (Cost)"}: ₹$costVal",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customPercentVal,
                                            onValueChange = { newValue ->
                                                customPercentVal = newValue
                                                customFixedVal = ""
                                                val pct = newValue.toDoubleOrNull()
                                                if (pct != null && pct >= 0) {
                                                    activeMarkupType = MarkupType.PERCENT
                                                    activeMarkupValue = pct
                                                    val calculated = costVal * (1.0 + pct / 100.0)
                                                    viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) calculated.toInt().toString() else String.format("%.2f", calculated)
                                                } else {
                                                    activeMarkupType = MarkupType.NONE
                                                    activeMarkupValue = 0.0
                                                }
                                            },
                                            label = { Text(if (language == AppLanguage.HINDI) "मुनाफा (%)" else "Add Profit (%)") },
                                            placeholder = { Text("e.g. 15") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = customFixedVal,
                                            onValueChange = { newValue ->
                                                customFixedVal = newValue
                                                customPercentVal = ""
                                                val amt = newValue.toDoubleOrNull()
                                                if (amt != null && amt >= 0) {
                                                    activeMarkupType = MarkupType.FIXED
                                                    activeMarkupValue = amt
                                                    val calculated = costVal + amt
                                                    viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) calculated.toInt().toString() else String.format("%.2f", calculated)
                                                } else {
                                                    activeMarkupType = MarkupType.NONE
                                                    activeMarkupValue = 0.0
                                                }
                                            },
                                            label = { Text(if (language == AppLanguage.HINDI) "मुनाफा (₹)" else "Add Profit (₹)") },
                                            placeholder = { Text("e.g. 50") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = viewModel.productWholesalePriceInput,
                                        onValueChange = {
                                            viewModel.productWholesalePriceInput = it
                                            activeMarkupType = MarkupType.NONE
                                            activeMarkupValue = 0.0
                                        },
                                        label = { Text(Translation.getString("whole_col", language)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_product_wholesale"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "wholesale"
                                            IconButton(
                                                onClick = {
                                                    viewModel.activeVoiceTargetField = "wholesale"
                                                    onVoiceTypingClick()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                                    contentDescription = "Voice Input Wholesale",
                                                    tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            else -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कदम ६: इकाई और बिक्री मूल्य" else "Step 6: Pricing Unit & Selling Price",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "क्या यह प्रति नग या प्रति किलोग्राम है? और खुदरा बिक्री मूल्य क्या है?" else "Is the product priced per piece or per kilogram? And what is the retail selling price?",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )

                                    val isPiece = viewModel.productPriceUnitInput == "piece"
                                    val isKg = viewModel.productPriceUnitInput == "kg"

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clickable { viewModel.productPriceUnitInput = "piece" }
                                                .testTag("unit_piece_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isPiece) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            ),
                                            border = BorderStroke(
                                                width = if (isPiece) 2.dp else 1.dp,
                                                color = if (isPiece) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            )
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPiece) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = null,
                                                        tint = if (isPiece) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = Translation.getString("unit_per_piece", language),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isPiece) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clickable { viewModel.productPriceUnitInput = "kg" }
                                                .testTag("unit_kg_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isKg) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            ),
                                            border = BorderStroke(
                                                width = if (isKg) 2.dp else 1.dp,
                                                color = if (isKg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            )
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isKg) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = null,
                                                        tint = if (isKg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = Translation.getString("unit_per_kg", language),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isKg) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = viewModel.productSellingPriceInput,
                                        onValueChange = { viewModel.productSellingPriceInput = it },
                                        label = { Text(Translation.getString("sell_col", language)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_product_selling"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "selling"
                                            IconButton(
                                                onClick = {
                                                    viewModel.activeVoiceTargetField = "selling"
                                                    onVoiceTypingClick()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                                    contentDescription = "Voice Input Selling",
                                                    tint = if (active) Color.Red else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Guided Navigation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { speakStepPrompt(currentWizardStep) },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                    .size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Replay audio instructions",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentWizardStep > 1) {
                                    OutlinedButton(
                                        onClick = { currentWizardStep-- },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text(text = if (language == AppLanguage.HINDI) "पीछे" else "Back")
                                    }
                                }

                                if (currentWizardStep < 6) {
                                    Button(
                                        onClick = {
                                            if (currentWizardStep == 1 && viewModel.productNameInput.isBlank()) {
                                                Toast.makeText(context, if (language == AppLanguage.HINDI) "उत्पाद का नाम आवश्यक है!" else "Product Name is required!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                currentWizardStep++
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text(text = if (language == AppLanguage.HINDI) "आगे" else "Next")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (viewModel.productNameInput.isNotBlank()) {
                                                viewModel.saveProduct()
                                            } else {
                                                Toast.makeText(context, Translation.getString("validation_err", language), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp),
                                        colors = if (language == AppLanguage.HINDI) {
                                            ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                                        } else {
                                            ButtonDefaults.buttonColors()
                                        }
                                    ) {
                                        if (language == AppLanguage.HINDI) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Save",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(text = Translation.getString("save", language))
                                    }
                                }
                            }
                        }

                        // Bottom Step indicators dots
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            (1..6).forEach { stepIdx ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(width = 24.dp, height = 4.dp)
                                        .background(
                                            color = if (stepIdx == currentWizardStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }

                    } else {
                        // --- TRADITIONAL STANDARD DIALOG FORM MODE ---
                        // Simulated Speak pre-fill button for quick mock in emulators
                        OutlinedButton(
                            onClick = { viewModel.autoSimulateVoiceInput() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("simulate_voice_input_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoMode, contentDescription = "auto simulate", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Translation.getString("fill_fields", language) + " (आवाज अनुकरण)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Column Row 1: Product Name & Supplier
                        OutlinedTextField(
                            value = viewModel.productNameInput,
                            onValueChange = { viewModel.productNameInput = it },
                            label = { Text(Translation.getString("name_col", language)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_product_name"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "name"
                                IconButton(
                                    onClick = {
                                        viewModel.activeVoiceTargetField = "name"
                                        onVoiceTypingClick()
                                    },
                                    modifier = Modifier.testTag("mic_btn_name")
                                ) {
                                    Icon(
                                        imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                        contentDescription = "Voice Input Name",
                                        tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        // Row Column: Description
                        OutlinedTextField(
                            value = viewModel.productDescriptionInput,
                            onValueChange = { viewModel.productDescriptionInput = it },
                            label = { Text(Translation.getString("desc_col", language)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_product_desc"),
                            singleLine = false,
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "description"
                                IconButton(
                                    onClick = {
                                        viewModel.activeVoiceTargetField = "description"
                                        onVoiceTypingClick()
                                    },
                                    modifier = Modifier.testTag("mic_btn_description")
                                ) {
                                    Icon(
                                        imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                        contentDescription = "Voice Input Description",
                                        tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = viewModel.productSupplierInput,
                            onValueChange = { viewModel.productSupplierInput = it },
                            label = { Text(Translation.getString("supplier_col", language)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_product_supplier"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "supplier"
                                IconButton(
                                    onClick = {
                                        viewModel.activeVoiceTargetField = "supplier"
                                        onVoiceTypingClick()
                                    },
                                    modifier = Modifier.testTag("mic_btn_supplier")
                                ) {
                                    Icon(
                                        imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                        contentDescription = "Voice Input Supplier",
                                        tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        // Numerical Columns (Stacked): Cost Price, Wholesale Price, Retail Price
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = viewModel.productCostPriceInput,
                                onValueChange = { viewModel.productCostPriceInput = it },
                                label = { Text(Translation.getString("cost_col", language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_product_cost"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                                ),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "cost"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "cost"
                                            onVoiceTypingClick()
                                        },
                                        modifier = Modifier.testTag("mic_btn_cost")
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Cost",
                                            tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = viewModel.productWholesalePriceInput,
                                onValueChange = { newValue ->
                                    viewModel.productWholesalePriceInput = newValue
                                    // Reset active markup helper tracking if user edits manually
                                    activeMarkupType = MarkupType.NONE
                                    activeMarkupValue = 0.0
                                    if (customPercentVal.isNotEmpty() || customFixedVal.isNotEmpty()) {
                                        customPercentVal = ""
                                        customFixedVal = ""
                                    }
                                },
                                label = { Text(Translation.getString("whole_col", language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_product_wholesale"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                    focusedLabelColor = MaterialTheme.colorScheme.tertiary,
                                    focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f)
                                ),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "wholesale"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "wholesale"
                                            onVoiceTypingClick()
                                        },
                                        modifier = Modifier.testTag("mic_btn_wholesale")
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Wholesale",
                                            tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            // Openable / Collapsible Markup calculation helper
                            val costPriceVal = remember(viewModel.productCostPriceInput) {
                                viewModel.productCostPriceInput.toDoubleOrNull()
                            }
                            if (costPriceVal != null && costPriceVal > 0.0) {
                                // Expand/Collapse toggle button
                                TextButton(
                                    onClick = { isMarkupAssistantExpanded = !isMarkupAssistantExpanded },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMarkupAssistantExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Toggle Assistant",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) {
                                                if (isMarkupAssistantExpanded) "गणना सहायक छिपाएं ✖" else "लागत मूल्य से थोक मुनाफा जोड़ें (मुनाफा % या रुपये) ⚙️"
                                            } else {
                                                if (isMarkupAssistantExpanded) "Hide Assistant" else "Add wholesale profit markup (Calculator) ⚙️"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // The expandable Assistant panel itself
                                AnimatedVisibility(
                                    visible = isMarkupAssistantExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (language == AppLanguage.HINDI) "कस्टम थोक मुनाफा जोड़ें (लागत मूल्य: ₹$costPriceVal)" else "Add Wholesale Profit Markup (Cost: ₹$costPriceVal)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }

                                        // Custom calculation fields (percentage & added rupees)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = customPercentVal,
                                                onValueChange = { newValue ->
                                                    customPercentVal = newValue
                                                    customFixedVal = ""
                                                    val pct = newValue.toDoubleOrNull()
                                                    if (pct != null && pct >= 0) {
                                                        activeMarkupType = MarkupType.PERCENT
                                                        activeMarkupValue = pct
                                                        val calculated = costPriceVal * (1.0 + pct / 100.0)
                                                        viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) calculated.toInt().toString() else String.format("%.2f", calculated)
                                                    } else {
                                                        activeMarkupType = MarkupType.NONE
                                                        activeMarkupValue = 0.0
                                                    }
                                                },
                                                label = { Text(if (language == AppLanguage.HINDI) "प्रतिशत (%)" else "Custom % Profit") },
                                                placeholder = { Text("e.g. 10") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                )
                                            )

                                            OutlinedTextField(
                                                value = customFixedVal,
                                                onValueChange = { newValue ->
                                                    customFixedVal = newValue
                                                    customPercentVal = ""
                                                    val amt = newValue.toDoubleOrNull()
                                                    if (amt != null && amt >= 0) {
                                                        activeMarkupType = MarkupType.FIXED
                                                        activeMarkupValue = amt
                                                        val calculated = costPriceVal + amt
                                                        viewModel.productWholesalePriceInput = if (calculated % 1.0 == 0.0) calculated.toInt().toString() else String.format("%.2f", calculated)
                                                    } else {
                                                        activeMarkupType = MarkupType.NONE
                                                        activeMarkupValue = 0.0
                                                    }
                                                },
                                                label = { Text(if (language == AppLanguage.HINDI) "रुपये (₹)" else "Custom ₹ Profit") },
                                                placeholder = { Text("e.g. 50") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = viewModel.productSellingPriceInput,
                                onValueChange = { viewModel.productSellingPriceInput = it },
                                label = { Text(Translation.getString("sell_col", language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_product_selling"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                                ),
                                trailingIcon = {
                                    val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "selling"
                                    IconButton(
                                        onClick = {
                                            viewModel.activeVoiceTargetField = "selling"
                                            onVoiceTypingClick()
                                        },
                                        modifier = Modifier.testTag("mic_btn_selling")
                                    ) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                            contentDescription = "Voice Input Selling",
                                            tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }

                        // Pricing Unit Selection Area (Per piece vs Per KG)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = Translation.getString("price_unit_label", language),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isPiece = viewModel.productPriceUnitInput == "piece"
                                val isKg = viewModel.productPriceUnitInput == "kg"

                                // Per Piece Option button
                                OutlinedButton(
                                    onClick = { viewModel.productPriceUnitInput = "piece" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("unit_piece_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isPiece) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        contentColor = if (isPiece) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = if (isPiece) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isPiece) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "selected",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = Translation.getString("unit_per_piece", language),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Per KG Option button
                                OutlinedButton(
                                    onClick = { viewModel.productPriceUnitInput = "kg" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("unit_kg_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isKg) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        contentColor = if (isKg) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = if (isKg) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isKg) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "selected",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = Translation.getString("unit_per_kg", language),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Save / Cancel row keys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.showProductDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_cancel_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = Translation.getString("cancel", language))
                            }

                            Button(
                                onClick = {
                                    if (viewModel.productNameInput.isNotBlank()) {
                                        viewModel.saveProduct()
                                    } else {
                                        Toast.makeText(context, Translation.getString("validation_err", language), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_save_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = if (language == AppLanguage.HINDI) {
                                    ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                if (language == AppLanguage.HINDI) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(text = Translation.getString("save", language))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCardView(
    viewModel: PriceListViewModel,
    product: ProductEntity,
    language: AppLanguage,
    serialNumber: Int? = null
) {
    val showCostAndMargins = viewModel.showCostAndMargins
    val showSupplierInfo = viewModel.showSupplierInfo
    val taxRate = viewModel.defaultTaxRate

    val retailProfitPr = remember(product.costPrice, product.sellingPrice) {
        val calculated = product.sellingPrice - product.costPrice
        if (calculated < 0) 0.0 else calculated
    }
    val wholesaleProfitPr = remember(product.costPrice, product.wholesalePrice) {
        val calculated = product.wholesalePrice - product.costPrice
        if (calculated < 0) 0.0 else calculated
    }

    val retailTaxInclPrice = remember(product.sellingPrice, taxRate) {
        product.sellingPrice * (1.0 + taxRate / 100.0)
    }
    val wholesaleTaxInclPrice = remember(product.wholesalePrice, taxRate) {
        product.wholesalePrice * (1.0 + taxRate / 100.0)
    }

    val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }
    val activeFolder = viewModel.selectedFolder
    val folderColorTheme = if (activeFolder != null) {
        getFolderColorTheme(activeFolder.id, isLightTheme)
    } else {
        null
    }

    val cardIndex = if (serialNumber != null) (serialNumber - 1) % 3 else 0
    val cardBgColor = if (isLightTheme) {
        when (cardIndex) {
            0 -> Color(0xFFE8F5E9) // Soft light mint green
            1 -> Color(0xFFFCE4EC) // Soft light pink/rose
            else -> Color(0xFFE8EAF6) // Soft light blue/indigo
        }
    } else {
        when (cardIndex) {
            0 -> Color(0xFF142D1B) // Deep forest/pine green
            1 -> Color(0xFF1C2341) // Deep navy blue
            else -> Color(0xFF2F1A2C) // Deep dark amethyst/plum
        }
    }

    val cardBorderColor = if (isLightTheme) {
        when (cardIndex) {
            0 -> Color(0xFFA5D6A7)
            1 -> Color(0xFFF48FB1)
            else -> Color(0xFF9FA8DA)
        }
    } else {
        when (cardIndex) {
            0 -> Color(0xFF2E5A3F)
            1 -> Color(0xFF3B4F81)
            else -> Color(0xFF5A3E61)
        }
    }

    val textColor = if (isLightTheme) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = { viewModel.activeProductForOptions = product }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(1.5.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card visual title & actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (serialNumber != null) "$serialNumber. ${product.name}" else product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            color = textColor,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // Pricing unit simple, colorful pill badge (vibrant & highly visible)
                        val isKg = product.priceUnit == "kg"
                        val unitColor = if (isKg) {
                            if (isLightTheme) Color(0xFF0284C7) else Color(0xFF38BDF8) // Vibrant sky blue/teal
                        } else {
                            if (isLightTheme) Color(0xFF7C3AED) else Color(0xFFA78BFA) // Vibrant deep purple
                        }
                        val unitText = if (isKg) Translation.getString("unit_per_kg", language) else Translation.getString("unit_per_piece", language)
                        
                        Surface(
                            color = unitColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, unitColor.copy(alpha = 0.6f)),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = unitText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = unitColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (product.boughtFrom.isNotEmpty() && showSupplierInfo) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "supplier symbol",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${Translation.getString("supplier_col", language)}: ${product.boughtFrom}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            if (product.description.isNotEmpty()) {
                val descTextColor = if (isLightTheme) {
                    when (cardIndex) {
                        0 -> Color(0xFF1B5E20) // Deep rich forest green
                        1 -> Color(0xFF880E4F) // Deep rich pink/burgundy
                        else -> Color(0xFF1A237E) // Deep rich indigo/navy blue
                    }
                } else {
                    when (cardIndex) {
                        0 -> Color(0xFF4ADE80) // Bright glowing mint green
                        1 -> Color(0xFFF472B6) // Bright glowing pink
                        else -> Color(0xFF818CF8) // Bright glowing indigo/blue
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(descTextColor.copy(alpha = 0.08f))
                        .border(BorderStroke(0.5.dp, descTextColor.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "description icon",
                        tint = descTextColor,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = product.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = descTextColor,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing details grids (Cost price vs Selling Retail price vs Wholesale Selling price) rearranged in Column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cost Column Card - High contrast Red for cost outflow
                if (showCostAndMargins) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isLightTheme) Color(0xFF331518) else Color(0xFFFCE8E6)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (!isLightTheme) Color(0xFFC5221F).copy(alpha = 0.4f) else Color(0xFFFAD2CF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Translation.getString("cost_col", language).split(" ").first(),
                                fontSize = 12.sp,
                                color = if (!isLightTheme) Color(0xFFF28B82) else Color(0xFFC5221F),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "₹${product.costPrice} / ${if (product.priceUnit == "kg") "kg" else "pc"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isLightTheme) Color(0xFFF28B82) else Color(0xFFC5221F)
                            )
                        }
                    }
                }

                // Wholesale Column Card - Premium Dark Blue / Light Blue for wholesale values
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isLightTheme) Color(0xFF1E293B) else Color(0xFFE8EAF6)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (!isLightTheme) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFF93C5FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Translation.getString("whole_col", language).split(" ").first(),
                                fontSize = 12.sp,
                                color = if (!isLightTheme) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                fontWeight = FontWeight.Bold
                            )
                            if (taxRate > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%.1f", wholesaleTaxInclPrice)} (${Translation.getString("tax_inclusive", language)})",
                                    fontSize = 10.sp,
                                    color = if (!isLightTheme) Color(0xFF93C5FD).copy(alpha = 0.7f) else Color(0xFF1E40AF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${product.wholesalePrice} / ${if (product.priceUnit == "kg") "kg" else "pc"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isLightTheme) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                            )
                            val margin = product.wholesalePrice - product.costPrice
                            if (margin > 0) {
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "+₹${if (margin % 1.0 == 0.0) margin.toInt().toString() else String.format("%.1f", margin)} ${if (language == AppLanguage.HINDI) "मुनाफा" else "margin"}",
                                    fontSize = 11.sp,
                                    color = if (!isLightTheme) Color(0xFF93C5FD).copy(alpha = 0.8f) else Color(0xFF1E40AF),
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }

                // Retail Column Card - Active Green for retail sales & profit!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isLightTheme) Color(0xFF142918) else Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (!isLightTheme) Color(0xFF81C784).copy(alpha = 0.4f) else Color(0xFFC8E6C9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Translation.getString("sell_col", language).split(" ").first(),
                                fontSize = 12.sp,
                                color = if (!isLightTheme) Color(0xFF81C784) else Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                            if (taxRate > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%.1f", retailTaxInclPrice)} (${Translation.getString("tax_inclusive", language)})",
                                    fontSize = 10.sp,
                                    color = if (!isLightTheme) Color(0xFF81C784).copy(alpha = 0.7f) else Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "₹${product.sellingPrice} / ${if (product.priceUnit == "kg") "kg" else "pc"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isLightTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
                        )
                    }
                }
            }

            if (showCostAndMargins) {
                Spacer(modifier = Modifier.height(8.dp))

                // Calculated margins banner details (profit indicators)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightProfitBg.copy(alpha = 0.5f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "profit",
                            tint = ProfitGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${Translation.getString("retail_profit", language)}: +₹$retailProfitPr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )

                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "wholesale profit",
                            tint = if (isLightTheme) Color(0xFF1D4ED8) else Color(0xFF60A5FA),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${Translation.getString("wholesale_profit", language)}: +₹$wholesaleProfitPr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLightTheme) Color(0xFF1D4ED8) else Color(0xFF60A5FA)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusWidget(
    viewModel: PriceListViewModel,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
) {
    if (viewModel.loginMethod != "cloud" || viewModel.cloudShopId.isBlank()) return
    val context = LocalContext.current
    val language = viewModel.currentLanguage

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Cloud Connected",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (viewModel.cloudUsername.isNotEmpty()) viewModel.cloudUsername else "Operator",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Text(
                    text = "${Translation.getString("shop_sync_code", language).split(" (")[0]}: ${viewModel.cloudShopId.take(8)}...",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (viewModel.cloudSyncMessage.isNotEmpty()) {
                    Text(
                        text = viewModel.cloudSyncMessage,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.syncFromCloud { success, msg ->
                            if (success) {
                                Toast.makeText(context, Translation.getString("sync_success", language), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, Translation.getString("sync_error", language) + ": $msg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.size(28.dp).testTag("header_sync_btn")
                ) {
                    if (viewModel.isCloudSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Now",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeImage(text: String, modifier: Modifier = Modifier) {
    val bitmap = remember(text) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 256, 256)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = modifier
        )
    }
}

fun scanQrCode(context: Context, onResult: (String) -> Unit) {
    try {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = GmsBarcodeScanning.getClient(context, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrBlank()) {
                    onResult(rawValue)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Scanning failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    } catch (e: Exception) {
        Toast.makeText(context, "Scanner error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

enum class SettingsSubPage {
    LAYOUT_THEME,
    FONT_SIZE,
    LANGUAGE,
    SYNC_BACKUP,
    PROFILE,
    DEVICES,
    SORT_SETTINGS,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: PriceListViewModel,
    onDismiss: () -> Unit
) {
    val language = viewModel.currentLanguage
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var newPasscodeVal by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    var activeSubPage by remember { mutableStateOf<SettingsSubPage?>(null) }

    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var importErrorMsg by remember { mutableStateOf<String?>(null) }
    var importSummaryMsg by remember { mutableStateOf<String?>(null) }
    var parsedProductsToImport by remember { mutableStateOf<List<ParsedProduct>?>(null) }
    var isImportingInProgress by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream, Charsets.UTF_8))
                        val lines = reader.readLines()
                        if (lines.isEmpty()) {
                            importErrorMsg = if (language == AppLanguage.HINDI) "CSV फ़ाइल खाली है।" else "The CSV file is empty."
                            showImportConfirmDialog = true
                            return@launch
                        }
                        
                        val headerLine = lines.firstOrNull() ?: ""
                        val headers = parseCsvLine(headerLine).map { it.lowercase().trim('\"', ' ') }
                        
                        // Validate Schema headers
                        val categoryIdx = headers.indexOfFirst { it == "category" || it == "folder" || it == "वर्ग" }
                        val nameIdx = headers.indexOfFirst { it == "product name" || it == "item name" || it == "name" || it == "नाम" }
                        val sellingIdx = headers.indexOfFirst { it == "selling price" || it == "price" || it == "selling" || it == "विक्रय मूल्य" }
                        
                        if (categoryIdx == -1 || nameIdx == -1 || sellingIdx == -1) {
                            importErrorMsg = if (language == AppLanguage.HINDI) {
                                "अमान्य CSV संरचना! फ़ाइल में 'Category', 'Product Name' और 'Selling Price' कॉलम होने चाहिए।"
                            } else {
                                "Invalid CSV structure! The file must contain 'Category', 'Product Name', and 'Selling Price' columns."
                            }
                            showImportConfirmDialog = true
                            return@launch
                        }
                        
                        // Optional indices
                        val descIdx = headers.indexOfFirst { it == "description" || it == "desc" || it == "विवरण" }
                        val costIdx = headers.indexOfFirst { it == "cost price" || it == "cost" || it == "लागत" }
                        val wholesaleIdx = headers.indexOfFirst { it == "wholesale price" || it == "wholesale" || it == "थोक" }
                        val boughtFromIdx = headers.indexOfFirst { it == "bought from" || it == "supplier" || it == "विक्रेता" }
                        val unitIdx = headers.indexOfFirst { it == "price unit" || it == "unit" || it == "इकाई" }
                        
                        val parsedList = mutableListOf<ParsedProduct>()
                        val maxIdx = maxOf(categoryIdx, nameIdx, sellingIdx, descIdx, costIdx, wholesaleIdx, boughtFromIdx, unitIdx)
                        
                        val errorsList = mutableListOf<String>()
                        
                        lines.drop(1).forEachIndexed { index, line ->
                            if (line.isBlank()) return@forEachIndexed
                            val tokens = padList(parseCsvLine(line), maxIdx + 1)
                            
                            val category = tokens[categoryIdx].trim('\"', ' ')
                            val name = tokens[nameIdx].trim('\"', ' ')
                            val sellingStr = tokens[sellingIdx].trim('\"', ' ')
                            
                            val rowNum = index + 2 // 1-indexed plus header row
                            
                            if (category.isBlank() || name.isBlank()) {
                                errorsList.add("Row $rowNum: " + (if (language == AppLanguage.HINDI) "श्रेणी या उत्पाद का नाम खाली नहीं हो सकता।" else "Category or Product Name cannot be empty."))
                                return@forEachIndexed
                            }
                            
                            val sellingPrice = sellingStr.toDoubleOrNull()
                            if (sellingPrice == null) {
                                errorsList.add("Row $rowNum: " + (if (language == AppLanguage.HINDI) "विक्रय मूल्य '$sellingStr' एक मान्य संख्या होनी चाहिए।" else "Selling Price '$sellingStr' must be a valid number."))
                                return@forEachIndexed
                            }
                            
                            val description = if (descIdx != -1) tokens[descIdx].trim('\"', ' ') else ""
                            val costPrice = if (costIdx != -1) tokens[costIdx].trim('\"', ' ').toDoubleOrNull() ?: 0.0 else 0.0
                            val wholesalePrice = if (wholesaleIdx != -1) tokens[wholesaleIdx].trim('\"', ' ').toDoubleOrNull() ?: sellingPrice else sellingPrice
                            val boughtFrom = if (boughtFromIdx != -1) tokens[boughtFromIdx].trim('\"', ' ') else ""
                            val priceUnit = if (unitIdx != -1) tokens[unitIdx].trim('\"', ' ').ifBlank { "piece" } else "piece"
                            
                            parsedList.add(
                                ParsedProduct(
                                    category = category,
                                    name = name,
                                    description = description,
                                    costPrice = costPrice,
                                    sellingPrice = sellingPrice,
                                    wholesalePrice = wholesalePrice,
                                    boughtFrom = boughtFrom,
                                    priceUnit = priceUnit
                                )
                            )
                        }
                        
                        if (errorsList.isNotEmpty()) {
                            val limit = 5
                            val displayedErrors = errorsList.take(limit).joinToString("\n")
                            val remaining = errorsList.size - limit
                            val extraText = if (remaining > 0) "\n...and $remaining more errors" else ""
                            importErrorMsg = (if (language == AppLanguage.HINDI) "डेटा सत्यापन विफल:\n" else "Data Validation Failed:\n") + displayedErrors + extraText
                            parsedProductsToImport = null
                        } else if (parsedList.isEmpty()) {
                            importErrorMsg = if (language == AppLanguage.HINDI) "आयात करने के लिए कोई मान्य उत्पाद डेटा नहीं मिला।" else "No valid product rows found to import."
                            parsedProductsToImport = null
                        } else {
                            val uniqueCategoriesCount = parsedList.map { it.category }.distinct().size
                            importErrorMsg = null
                            parsedProductsToImport = parsedList
                            importSummaryMsg = if (language == AppLanguage.HINDI) {
                                "सफलतापूर्वक सत्यापित!\n\n" +
                                "• आयात करने योग्य उत्पाद: ${parsedList.size}\n" +
                                "• श्रेणियां: $uniqueCategoriesCount\n\n" +
                                "क्या आप इस डेटा को आयात करना चाहते हैं?"
                            } else {
                                "Successfully Validated!\n\n" +
                                "• Products to import: ${parsedList.size}\n" +
                                "• Categories to map: $uniqueCategoriesCount\n\n" +
                                "Do you want to proceed with importing this data?"
                            }
                        }
                        showImportConfirmDialog = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    importErrorMsg = (if (language == AppLanguage.HINDI) "फ़ाइल पढ़ने में त्रुटि: " else "Error reading file: ") + e.localizedMessage
                    showImportConfirmDialog = true
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }
        val settingsColorScheme = if (isLightTheme) {
            androidx.compose.material3.lightColorScheme(
                primary = Color(0xFF6B21A8), // Rich Royal Purple
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF3E8FF), // Very soft lavender
                secondary = Color(0xFF7E22CE),
                secondaryContainer = Color(0xFFFAF5FF),
                background = Color(0xFFF9F5FF), // Pale lavender bg
                surface = Color(0xFFFFFFFF),
                onBackground = Color(0xFF3B0764),
                onSurface = Color(0xFF3B0764),
                outline = Color(0xFFD8B4FE),
                outlineVariant = Color(0xFFF3E8FF)
            )
        } else {
            androidx.compose.material3.darkColorScheme(
                primary = Color(0xFFC084FC), // Vibrant neon amethyst
                onPrimary = Color(0xFF2E004F),
                primaryContainer = Color(0xFF4A1D73), // Deep velvet purple container
                secondary = Color(0xFFA855F7),
                secondaryContainer = Color(0xFF2E1A47),
                background = Color(0xFF130E1F), // Dark midnight amethyst
                surface = Color(0xFF1E142F), // Deep space-violet surface
                onBackground = Color(0xFFF5F3FF),
                onSurface = Color(0xFFF5F3FF),
                outline = Color(0xFF6B21A8),
                outlineVariant = Color(0xFF3B0764)
            )
        }

        MaterialTheme(colorScheme = settingsColorScheme) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .testTag("settings_dialog_surface"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                tonalElevation = 8.dp
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header (changes based on activeSubPage)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeSubPage != null) {
                            IconButton(onClick = { activeSubPage = null }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back to main settings"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = when (activeSubPage) {
                                null -> Translation.getString("settings_title", language).replace(" ⚙️", "")
                                SettingsSubPage.LAYOUT_THEME -> if (language == AppLanguage.HINDI) "कस्टम लेआउट और थीम" else "Custom Layout & Theme"
                                SettingsSubPage.FONT_SIZE -> Translation.getString("settings_font_size", language).replace(" 🔍", "")
                                SettingsSubPage.LANGUAGE -> if (language == AppLanguage.HINDI) "ऐप की भाषा" else "App Language"
                                SettingsSubPage.SYNC_BACKUP -> if (language == AppLanguage.HINDI) "डेटा बैकअप और सिंक" else "Data Sync & Backup"
                                SettingsSubPage.PROFILE -> if (language == AppLanguage.HINDI) "दुकान और मालिक प्रोफ़ाइल" else "Shop & Owner Profile"
                                SettingsSubPage.DEVICES -> if (language == AppLanguage.HINDI) "डिवाइसेस (कंप्यूटर/फ़ोन)" else "Connected Devices"
                                SettingsSubPage.SORT_SETTINGS -> if (language == AppLanguage.HINDI) "उत्पाद क्रमबद्ध सेटिंग" else "Product Sort Settings"
                                SettingsSubPage.ABOUT -> if (language == AppLanguage.HINDI) "ऐप के बारे में और संस्करण" else "About App & Version"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Settings")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (activeSubPage) {
                        null -> {
                            // MAIN SETTINGS LIST - Extremely Short, Uncluttered & Simple!
                            
                            // 1. My Profile / Shop Profile
                            SettingsMenuRow(
                                icon = Icons.Default.Person,
                                title = if (language == AppLanguage.HINDI) "मेरी प्रोफ़ाइल" else "My Profile",
                                subtitle = if (language == AppLanguage.HINDI) "दुकान का नाम, मालिक का नाम और विवरण बदलें" else "Manage store name, owner name & details",
                                onClick = { activeSubPage = SettingsSubPage.PROFILE },
                                testTag = "settings_menu_profile"
                            )

                            // 2. Custom Layout & Theme
                            SettingsMenuRow(
                                icon = Icons.Default.Palette,
                                title = if (language == AppLanguage.HINDI) "कस्टम लेआउट और थीम" else "Custom Layout & Theme",
                                subtitle = if (language == AppLanguage.HINDI) "थीम मोड, रंग और दृश्यता विकल्प" else "Customize theme, branding, colors & visibility",
                                onClick = { activeSubPage = SettingsSubPage.LAYOUT_THEME },
                                testTag = "settings_menu_layout_theme"
                            )

                            // 3. App Font Size
                            SettingsMenuRow(
                                icon = Icons.Default.FormatSize,
                                title = if (language == AppLanguage.HINDI) "फ़ॉन्ट का आकार" else "App Font Size",
                                subtitle = if (language == AppLanguage.HINDI) "स्क्रीन पर लिखावट का आकार बदलें" else "Adjust the size of the text on screen",
                                onClick = { activeSubPage = SettingsSubPage.FONT_SIZE },
                                testTag = "settings_menu_font_size"
                            )

                            // 4. Language
                            SettingsMenuRow(
                                icon = Icons.Default.Language,
                                title = if (language == AppLanguage.HINDI) "भाषा बदलें" else "Language",
                                subtitle = if (language == AppLanguage.HINDI) "हिन्दी और English के बीच बदलें" else "Switch between English and Hindi",
                                onClick = { activeSubPage = SettingsSubPage.LANGUAGE },
                                testTag = "settings_menu_language"
                            )

                            // 5. Data Sync & Backup
                            SettingsMenuRow(
                                icon = Icons.Default.CloudSync,
                                title = if (language == AppLanguage.HINDI) "डेटा बैकअप और सिंक" else "Data Sync & Backup",
                                subtitle = if (language == AppLanguage.HINDI) "क्लाउड सिंक, क्यूआर कोड और गूगल बैकअप" else "Cloud sync, WhatsApp-like QR link & Google login",
                                onClick = { activeSubPage = SettingsSubPage.SYNC_BACKUP },
                                testTag = "settings_menu_sync"
                            )

                            // 6. Devices Settings
                            SettingsMenuRow(
                                icon = Icons.Default.Devices,
                                title = if (language == AppLanguage.HINDI) "डिवाइसेस (कंप्यूटर/फ़ोन)" else "Devices (Computer/Phone)",
                                subtitle = if (language == AppLanguage.HINDI) "अन्य कंप्यूटर या फ़ोन लिंक करें और स्कैन करें" else "Link other computer or phone and scan",
                                onClick = { activeSubPage = SettingsSubPage.DEVICES },
                                testTag = "settings_menu_devices"
                            )

                            // 7. Product Sort Settings
                            val currentSortLabel = when (viewModel.appProductSortOption) {
                                ProductSortOption.NAME_ASC -> if (language == AppLanguage.HINDI) "नाम (A से Z)" else "Name (A to Z)"
                                ProductSortOption.NAME_DESC -> if (language == AppLanguage.HINDI) "नाम (Z से A)" else "Name (Z to A)"
                                ProductSortOption.VALUE_HIGH_TO_LOW -> if (language == AppLanguage.HINDI) "मूल्य (अधिक से कम)" else "Value (High to Low)"
                                ProductSortOption.VALUE_LOW_TO_HIGH -> if (language == AppLanguage.HINDI) "मूल्य (कम से अधिक)" else "Value (Low to High)"
                                ProductSortOption.DATE_PURCHASED_NEWEST -> if (language == AppLanguage.HINDI) "खरीद तिथि (नवीनतम)" else "Date Purchased (Newest)"
                                ProductSortOption.DATE_PURCHASED_OLDEST -> if (language == AppLanguage.HINDI) "खरीद तिथि (सबसे पुराना)" else "Date Purchased (Oldest)"
                                ProductSortOption.DATE_ADDED_NEWEST -> if (language == AppLanguage.HINDI) "जोड़ने की तिथि (नवीनतम)" else "Date Added (Newest)"
                                ProductSortOption.DATE_ADDED_OLDEST -> if (language == AppLanguage.HINDI) "जोड़ने की तिथि (सबसे पुराना)" else "Date Added (Oldest)"
                            }
                            SettingsMenuRow(
                                icon = Icons.Default.Sort,
                                title = if (language == AppLanguage.HINDI) "उत्पाद क्रमबद्ध करें (सॉर्ट)" else "Product Sort Settings",
                                subtitle = "${if (language == AppLanguage.HINDI) "वर्तमान सॉर्ट" else "Current sorting"}: $currentSortLabel",
                                onClick = { activeSubPage = SettingsSubPage.SORT_SETTINGS },
                                testTag = "settings_menu_sort"
                            )

                            // 7. About App & Version
                            SettingsMenuRow(
                                icon = Icons.Default.Info,
                                title = if (language == AppLanguage.HINDI) "ऐप के बारे में और संस्करण" else "About App & Version",
                                subtitle = if (language == AppLanguage.HINDI) "संस्करण v${viewModel.appVersionName}" else "Version v${viewModel.appVersionName}",
                                onClick = { activeSubPage = SettingsSubPage.ABOUT },
                                testTag = "settings_menu_about"
                             )

                            // Separator to place Recycle Bin clearly at the very bottom
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // 6. Recycle Bin at the bottom of settings list as requested
                            SettingsMenuRow(
                                icon = Icons.Default.DeleteOutline,
                                title = if (language == AppLanguage.HINDI) "रीसायकल बिन" else "Recycle Bin",
                                subtitle = if (language == AppLanguage.HINDI) "हटाए गए उत्पाद और फ़ोल्डर्स देखें" else "View and restore deleted products or folders",
                                iconColor = MaterialTheme.colorScheme.error,
                                onClick = {
                                    onDismiss()
                                    viewModel.currentScreen = Screen.TRASH
                                },
                                testTag = "settings_menu_trash"
                            )
                        }

                        SettingsSubPage.LAYOUT_THEME -> {
                            // Subpage layout & theme content
                            
                            // A. App Theme Mode Title & Options
                            Text(
                                text = Translation.getString("settings_theme", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("SYSTEM", "LIGHT", "DARK").forEach { themeOption ->
                                    val isSelected = viewModel.appThemeMode == themeOption
                                    Button(
                                        onClick = { viewModel.updateAppThemeMode(themeOption) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(themeOption, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // B. Brand Color Palette Selector
                            Text(
                                text = Translation.getString("settings_palette", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val palettes = listOf(
                                Triple("SAGE", "palette_sage", Color(0xFF3E4F3C)),
                                Triple("BLUE", "palette_blue", Color(0xFF1B365D)),
                                Triple("CRIMSON", "palette_crimson", Color(0xFF722F37)),
                                Triple("TEAL", "palette_teal", Color(0xFF1A5249)),
                                Triple("GOLDEN", "palette_golden", Color(0xFF85642B))
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                palettes.forEach { (id, translationKey, primaryCol) ->
                                    val isSelected = viewModel.appThemePalette == id
                                    Surface(
                                        onClick = { viewModel.updateAppThemePalette(id) },
                                        modifier = Modifier.fillMaxWidth().testTag("palette_option_$id"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(20.dp),
                                                shape = CircleShape,
                                                color = primaryCol
                                            ) {}
                                            Text(
                                                text = Translation.getString(translationKey, language),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // D. Display Toggles
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Cost toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Translation.getString("settings_show_cost", language),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = viewModel.showCostAndMargins,
                                            onCheckedChange = { viewModel.updateShowCostAndMargins(it) },
                                            modifier = Modifier.testTag("settings_cost_switch")
                                        )
                                    }
                                    
                                    // Auto-numbering toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Translation.getString("settings_auto_numbering", language),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = viewModel.isFolderAutoNumberingEnabled,
                                            onCheckedChange = { viewModel.updateFolderAutoNumbering(it) },
                                            modifier = Modifier.testTag("settings_auto_numbering_switch")
                                        )
                                    }
                                    
                                    // Supplier toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Translation.getString("settings_show_supplier", language),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = viewModel.showSupplierInfo,
                                            onCheckedChange = { viewModel.updateShowSupplierInfo(it) },
                                            modifier = Modifier.testTag("settings_supplier_switch")
                                        )
                                    }

                                    // Audio Guide toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = Translation.getString("audio_guide", language),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = Translation.getString("audio_guide_desc", language),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Switch(
                                            checked = viewModel.isAudioGuideEnabled,
                                            onCheckedChange = { viewModel.isAudioGuideEnabled = it },
                                            modifier = Modifier.testTag("settings_audio_guide_switch")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // E. Default Tax Rate (GST/VAT Rate)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = Translation.getString("settings_tax", language),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(0f, 5f, 12f, 18f, 28f).forEach { tax ->
                                            val isSelected = viewModel.defaultTaxRate == tax
                                            Button(
                                                onClick = { viewModel.updateDefaultTaxRate(tax) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Text("${tax.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsSubPage.FONT_SIZE -> {
                            // Subpage App Font Size
                            Text(
                                text = Translation.getString("settings_font_size", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = Translation.getString("settings_font_size_desc", language),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Decrease button
                                OutlinedButton(
                                    onClick = {
                                        val currentVal = viewModel.fontSizeScale
                                        if (currentVal > 0.85f) {
                                            viewModel.updateFontSizeScale(currentVal - 0.15f)
                                        }
                                    },
                                    enabled = viewModel.fontSizeScale > 0.85f,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("A-", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // Selected Options Center
                                val scaleLabel = when {
                                    viewModel.fontSizeScale < 0.9f -> Translation.getString("font_size_small", language)
                                    viewModel.fontSizeScale < 1.1f -> Translation.getString("font_size_normal", language)
                                    viewModel.fontSizeScale < 1.25f -> Translation.getString("font_size_large", language)
                                    else -> Translation.getString("font_size_extra_large", language)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .height(44.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$scaleLabel (${(viewModel.fontSizeScale * 100).toInt()}%)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                // Increase button
                                OutlinedButton(
                                    onClick = {
                                        val currentVal = viewModel.fontSizeScale
                                        if (currentVal < 1.35f) {
                                            viewModel.updateFontSizeScale(currentVal + 0.15f)
                                        }
                                    },
                                    enabled = viewModel.fontSizeScale < 1.35f,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("A+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Font size live preview card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "लाइव पाठ पूर्वावलोकन:" else "Live Text Preview:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "आलू प्याज थोक सूची" else "Potato & Onion Catalog",
                                        fontSize = (16 * viewModel.fontSizeScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "लागत ₹२०, रिटेल ₹३०, मुनाफा ₹१०" else "Cost ₹20, Retail ₹30, Margin ₹10",
                                        fontSize = (12 * viewModel.fontSizeScale).sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        SettingsSubPage.LANGUAGE -> {
                            // Subpage App Language Selection
                            Text(
                                text = if (language == AppLanguage.HINDI) "ऐप की भाषा चुनें:" else "Select App Language:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(
                                    AppLanguage.ENGLISH to "English (US)",
                                    AppLanguage.HINDI to "हिन्दी (Hindi)"
                                ).forEach { (langOpt, name) ->
                                    val isSelected = viewModel.currentLanguage == langOpt
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!isSelected) {
                                                    viewModel.toggleLanguage()
                                                }
                                            }
                                            .testTag("lang_option_${langOpt.name}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsSubPage.SYNC_BACKUP -> {
                            // Subpage Sync & Cloud Backup
                            SyncStatusWidget(
                                viewModel = viewModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                            
                            // A. Google Account Card
                            if (viewModel.isPhoneConnected) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("phone_settings_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Phone Account",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = viewModel.customShopName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = viewModel.userPhoneNumber,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.disconnectPhone()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("phone_disconnect_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (language == AppLanguage.HINDI) "लॉगआउट करें" else "Log Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("phone_settings_card_connect"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = "Backup",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (language == AppLanguage.HINDI) "फ़ोन बैकअप लिंक करें" else "Link Phone Backup",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (language == AppLanguage.HINDI) "ऑटोमेटिक क्लाउड सिंक सक्षम करने के लिए लॉगिन करें" else "Log in to enable automatic cloud sync",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.currentScreen = Screen.LOGIN
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (language == AppLanguage.HINDI) "लॉगिन पेज पर जाएँ" else "Go to Login Screen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // B. Backup Shield Status
                            val hasCustomFirebase = viewModel.firebaseApiKeyInput.isNotBlank() && viewModel.firebaseProjectIdInput.isNotBlank()
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("google_backup_protection_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (hasCustomFirebase) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (hasCustomFirebase) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Cloud Protection",
                                            tint = if (hasCustomFirebase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "गूगल क्लाउड / बैकअप सेटिंग्स" else "Google Auto-Backup Settings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = if (hasCustomFirebase) {
                                            if (language == AppLanguage.HINDI)
                                                "सुरक्षित आटोमेटिक क्लाउड सिंक सक्रिय है। ऐप को अपग्रेड या पुनः स्थापित करने पर भी आपका पूरा केटेगरी इतिहास सुरक्षित रहेगा।"
                                                else "Backup Shield Active! Categories, prices, and history are auto-saved securely under Google Cloud standards to safeguard against data loss."
                                        } else {
                                            if (language == AppLanguage.HINDI)
                                                "ऑफ़लाइन स्थानीय डेटाबेस सक्रिय। आपका डेटा आपके फ़ोन पर पूरी तरह से सुरक्षित है। यदि आप अन्य डिवाइस के साथ सिंक या क्लाउड बैकअप चाहते हैं, तो अपना फ़ायरबेस डेटाबेस सेटअप करें।"
                                                else "Offline Local-First Mode active. All price lists, categories, and folders are saved securely on your device. To sync across multiple devices or enable cloud backups, configure your Firebase database keys below."
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                    TextButton(
                                        onClick = { viewModel.showFirebaseConfigDialog = true },
                                        modifier = Modifier.align(Alignment.End).testTag("open_firebase_settings_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "कस्टम डेटाबेस कुंजियाँ दर्ज करें" else "Set Custom Firebase Database",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // C. QR Code Multi-Device Linking
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = Translation.getString("multi_device_sync", language),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = Translation.getString("multi_device_desc", language),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    if (viewModel.cloudShopId.isBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.registerNewCloudShop { success, _ ->
                                                        if (success) { viewModel.syncToCloud() }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).testTag("settings_enable_linking_btn"),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                            ) {
                                                if (viewModel.isCloudSyncing) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                                                } else {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Text(if (language == AppLanguage.HINDI) "नया ग्रुप" else "New Group", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    scanQrCode(context) { scannedCode ->
                                                        if (scannedCode.isNotBlank()) {
                                                            viewModel.cloudShopId = scannedCode
                                                            viewModel.saveCloudCredentials()
                                                            viewModel.syncFromCloud { success, msg ->
                                                                if (success) {
                                                                    Toast.makeText(context, Translation.getString("sync_success", language), Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    Toast.makeText(context, Translation.getString("sync_error", language) + ": $msg", Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).testTag("settings_scan_qr_btn"),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Text(if (language == AppLanguage.HINDI) "स्कैन क्यूआर" else "Scan QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White)
                                            ) {
                                                QrCodeImage(
                                                    text = viewModel.cloudShopId,
                                                    modifier = Modifier.size(140.dp).padding(10.dp)
                                                )
                                            }

                                            Text(
                                                text = "${Translation.getString("shop_sync_code", language).split(" (")[0]}: ${viewModel.cloudShopId}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.syncToCloud { success, msg ->
                                                            if (success) {
                                                                Toast.makeText(context, Translation.getString("sync_success", language), Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, Translation.getString("sync_error", language) + ": $msg", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1.2f).height(38.dp).testTag("settings_sync_force_btn"),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    if (viewModel.isCloudSyncing) {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                                    } else {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Text(Translation.getString("sync_now", language), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.cloudShopId = ""
                                                        viewModel.saveCloudCredentials()
                                                        Toast.makeText(context, if (language == AppLanguage.HINDI) "डिवाइस अनलिंक किया गया" else "Device unlinked", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(0.8f).height(38.dp).testTag("settings_unlink_device_btn"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                                 ) {
                                                     Text(if (language == AppLanguage.HINDI) "अनलिंक" else "Unlink", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }

                            // D. Manual JSON Export/Import
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = Translation.getString("settings_backup", language),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = Translation.getString("settings_backup_desc", language),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val backupPayload = viewModel.getCloudJsonPayload()
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("PriceList Backup", backupPayload)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, Translation.getString("sync_code_copied", language), Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("export_backup_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text(Translation.getString("settings_export", language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    OutlinedTextField(
                                        value = importText,
                                        onValueChange = { importText = it },
                                        placeholder = { Text(Translation.getString("settings_import_hint", language), fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().height(70.dp).testTag("import_backup_input"),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Button(
                                        onClick = {
                                            if (importText.isNotBlank()) {
                                                scope.launch {
                                                    val restored = viewModel.importCloudJsonPayload(importText.trim())
                                                    if (restored) {
                                                        importText = ""
                                                        Toast.makeText(context, Translation.getString("settings_import_success", language), Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, Translation.getString("settings_import_fail", language), Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = importText.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth().testTag("import_backup_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text(Translation.getString("settings_import", language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // E. Physical Backup (CSV & PDF)
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("physical_backup_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "फाइल बैकअप (CSV और PDF)" else "Physical Backup (CSV & PDF)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) 
                                            "पूरी दुकान की रेट लिस्ट को स्प्रेडशीट (CSV) या प्रिंट करने योग्य PDF फाइल के रूप में सहेजें।"
                                            else "Export your entire shop price list as a spreadsheet (CSV) or printable PDF file for physical backups.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    
                                    val foldersList by viewModel.foldersFlow.collectAsState()
                                    val allProductsList by viewModel.allProductsFlow.collectAsState()
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (allProductsList.isEmpty()) {
                                                    Toast.makeText(context, if (language == AppLanguage.HINDI) "कोई सामान नहीं मिला!" else "No products found!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val csvFile = exportPriceListToCsv(
                                                        context = context,
                                                        folders = foldersList,
                                                        products = allProductsList,
                                                        shopName = viewModel.customShopName.ifBlank { "My_Shop" },
                                                        showCost = viewModel.showCostAndMargins
                                                    )
                                                    if (csvFile != null) {
                                                        shareBackupFile(context, csvFile, "text/csv", if (language == AppLanguage.HINDI) "CSV रेट लिस्ट साझा करें" else "Share CSV Price List")
                                                    } else {
                                                        Toast.makeText(context, "Failed to create CSV", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("export_csv_btn"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Text(if (language == AppLanguage.HINDI) "CSV एक्सपोर्ट" else "Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (allProductsList.isEmpty()) {
                                                    Toast.makeText(context, if (language == AppLanguage.HINDI) "कोई सामान नहीं मिला!" else "No products found!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val pdfFile = exportPriceListToPdf(
                                                        context = context,
                                                        folders = foldersList,
                                                        products = allProductsList,
                                                        shopName = viewModel.customShopName.ifBlank { "My Shop Price List" },
                                                        shopTagline = viewModel.customShopTagline.ifBlank { "Physical Backup" },
                                                        showCost = viewModel.showCostAndMargins
                                                    )
                                                    if (pdfFile != null) {
                                                        shareBackupFile(context, pdfFile, "application/pdf", if (language == AppLanguage.HINDI) "PDF रेट लिस्ट साझा करें" else "Share PDF Price List")
                                                    } else {
                                                        Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("export_pdf_btn"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Text(if (language == AppLanguage.HINDI) "PDF एक्सपोर्ट" else "Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = if (language == AppLanguage.HINDI) "डेटा आयात करें (CSV)" else "Import Data (CSV)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            importLauncher.launch("text/*")
                                        },
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("import_csv_btn"),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = if (language == AppLanguage.HINDI) "CSV फ़ाइल आयात करें" else "Import CSV File",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsSubPage.PROFILE -> {
                            // Subpage Profile content
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val initial = if (viewModel.cloudUsername.isNotBlank()) {
                                                viewModel.cloudUsername.take(1).uppercase()
                                            } else if (viewModel.customShopName.isNotBlank()) {
                                                viewModel.customShopName.take(1).uppercase()
                                            } else {
                                                "O"
                                            }
                                            Text(
                                                text = initial,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = if (viewModel.cloudUsername.isNotBlank()) viewModel.cloudUsername else (if (language == AppLanguage.HINDI) "प्रोफ़ाइल मालिक" else "Profile Owner"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (viewModel.customShopName.isNotBlank()) viewModel.customShopName else (if (language == AppLanguage.HINDI) "दुकान की जानकारी" else "Shop Profile"),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Input fields inside a Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // A. Owner Name
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "आपका नाम (प्रोफ़ाइल मालिक)" else "Your Name (Profile Owner)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    OutlinedTextField(
                                        value = viewModel.cloudUsername,
                                        onValueChange = { viewModel.updateCloudUsername(it) },
                                        placeholder = { Text(if (language == AppLanguage.HINDI) "नाम दर्ज करें" else "E.g. Sharda Prasad", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_owner_name_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // B. Shop Name
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "दुकान का नाम (ब्रांडिंग)" else "Shop Profile / Branding Name",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    OutlinedTextField(
                                        value = viewModel.customShopName,
                                        onValueChange = { viewModel.updateCustomShopName(it) },
                                        placeholder = { Text("E.g. Sharda General Store", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_shop_name_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // C. Shop Tagline
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "दुकान का पता / विवरण" else "Shop Tagline / Description",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    OutlinedTextField(
                                        value = viewModel.customShopTagline,
                                        onValueChange = { viewModel.updateCustomShopTagline(it) },
                                        placeholder = { Text("E.g. Shop 42, Market Gali, Pune", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_shop_tagline_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Phone & Sync Account Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "खाते की जानकारी" else "Account & Connection Status",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "फ़ोन नंबर:" else "Phone Number:",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (viewModel.userPhoneNumber.isNotBlank()) viewModel.userPhoneNumber else (if (language == AppLanguage.HINDI) "लिंक नहीं है" else "Not Linked"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "सिंक मोड:" else "Sync Mode:",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (viewModel.cloudShopId.isNotBlank()) {
                                                if (language == AppLanguage.HINDI) "क्लाउड सिंक एक्टिव" else "Cloud Sync Active"
                                            } else {
                                                if (language == AppLanguage.HINDI) "ऑफ़लाइन लोकल-फ़र्स्ट" else "Offline Local-First"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (viewModel.cloudShopId.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }

                        SettingsSubPage.DEVICES -> {
                            // Subpage Devices content
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "लिंक किए गए डिवाइसेस" else "Linked Devices",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (viewModel.linkedDevices.isEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Devices,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (language == AppLanguage.HINDI) "कोई डिवाइस लिंक नहीं है" else "No linked devices",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (language == AppLanguage.HINDI) 
                                                    "अपने कंप्यूटर या दूसरे फ़ोन को लिंक करें ताकि वे आपस में ऑटो-सिंक हो सकें।" 
                                                else "Link your PC or another phone to keep your price list auto-synchronized.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                } else {
                                    viewModel.linkedDevices.forEach { deviceName ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Laptop,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = deviceName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = if (language == AppLanguage.HINDI) "सक्रिय (सिंक चालू)" else "Active (Synced)",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { viewModel.removeLinkedDevice(deviceName) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remove Device",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Scanning and adding a device section
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "नया डिवाइस जोड़ें" else "Add New Device",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Text(
                                            text = if (language == AppLanguage.HINDI) 
                                                "क्यूआर कोड स्कैन करके या नीचे दिए गए विकल्प का उपयोग करके अन्य डिवाइस को जोड़ें।" 
                                            else "Link another device by scanning its link QR code.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )

                                        Button(
                                            onClick = {
                                                scanQrCode(context) { scannedCode ->
                                                    viewModel.addLinkedDevice(scannedCode)
                                                    Toast.makeText(context, if (language == AppLanguage.HINDI) "डिवाइस सफलतापूर्वक लिंक हुआ!" else "Device linked successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("link_device_scan_btn"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (language == AppLanguage.HINDI) "डिवाइस QR कोड स्कैन करें 📷" else "Scan Device QR Code 📷",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        // Manual Input/Demo Device Link Option for testing in emulator
                                        var manualDeviceName by remember { mutableStateOf("") }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = manualDeviceName,
                                                onValueChange = { manualDeviceName = it },
                                                label = { Text(if (language == AppLanguage.HINDI) "मैन्युअल कोड / नाम" else "Manual Code / Name", fontSize = 11.sp) },
                                                placeholder = { Text("e.g. Chrome-PC-1", fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f).height(50.dp),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    if (manualDeviceName.isNotBlank()) {
                                                        viewModel.addLinkedDevice(manualDeviceName.trim())
                                                        manualDeviceName = ""
                                                        Toast.makeText(context, if (language == AppLanguage.HINDI) "डिवाइस सफलतापूर्वक लिंक हुआ!" else "Device linked successfully!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.height(44.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                enabled = manualDeviceName.isNotBlank()
                                            ) {
                                                Text(if (language == AppLanguage.HINDI) "जोड़ें" else "Add")
                                            }
                                        }

                                        // Demo Simulation Button
                                        Button(
                                            onClick = {
                                                val demoNames = listOf("My Laptop Pro", "Billing Counter-2", "Store Manager iPad", "Office PC")
                                                val selectedDemo = demoNames.random()
                                                viewModel.addLinkedDevice(selectedDemo)
                                                Toast.makeText(context, if (language == AppLanguage.HINDI) "$selectedDemo सफलतापूर्वक सिंक हो गया!" else "Linked $selectedDemo successfully!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (language == AppLanguage.HINDI) "त्वरित डेमो डिवाइस लिंक करें ✨" else "Quick Demo Link Device ✨",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsSubPage.SORT_SETTINGS -> {
                            val sortOptions: List<Triple<ProductSortOption, String, String>> = listOf(
                                Triple(ProductSortOption.NAME_ASC, if (language == AppLanguage.HINDI) "नाम: A से Z (वर्णमाला)" else "Name: A to Z (Alphabetical)", if (language == AppLanguage.HINDI) "वर्णानुक्रम के अनुसार वर्णमाला अनुसार व्यवस्थित करें" else "Sort alphabetically from A to Z"),
                                Triple(ProductSortOption.NAME_DESC, if (language == AppLanguage.HINDI) "नाम: Z से A (उल्टा वर्णमाला)" else "Name: Z to A (Reverse Alphabetical)", if (language == AppLanguage.HINDI) "Z से A की ओर वर्णमाला अनुसार व्यवस्थित करें" else "Sort alphabetically from Z to A"),
                                Triple(ProductSortOption.VALUE_HIGH_TO_LOW, if (language == AppLanguage.HINDI) "मूल्य: अधिक से कम (महंगा पहले)" else "Value: High to Low (Expensive first)", if (language == AppLanguage.HINDI) "अधिकतम विक्रय मूल्य वाले उत्पाद पहले दिखाएं" else "Show products with highest selling price first"),
                                Triple(ProductSortOption.VALUE_LOW_TO_HIGH, if (language == AppLanguage.HINDI) "मूल्य: कम से अधिक (सस्ता पहले)" else "Value: Low to High (Cheapest first)", if (language == AppLanguage.HINDI) "न्यूनतम विक्रय मूल्य वाले उत्पाद पहले दिखाएं" else "Show products with lowest selling price first"),
                                Triple(ProductSortOption.DATE_PURCHASED_NEWEST, if (language == AppLanguage.HINDI) "खरीद तिथि: नवीनतम पहले" else "Date Purchased: Newest First", if (language == AppLanguage.HINDI) "हाल ही में खरीदे गए उत्पाद पहले दिखाएं" else "Show recently purchased products first"),
                                Triple(ProductSortOption.DATE_PURCHASED_OLDEST, if (language == AppLanguage.HINDI) "खरीद तिथि: सबसे पुराना पहले" else "Date Purchased: Oldest First", if (language == AppLanguage.HINDI) "पुराने खरीदे गए उत्पाद पहले दिखाएं" else "Show products purchased longest ago first"),
                                Triple(ProductSortOption.DATE_ADDED_NEWEST, if (language == AppLanguage.HINDI) "जोड़ने की तिथि: नवीनतम पहले" else "Date Added: Newest First", if (language == AppLanguage.HINDI) "हाल ही में जोड़े गए उत्पाद पहले दिखाएं" else "Show recently added products first"),
                                Triple(ProductSortOption.DATE_ADDED_OLDEST, if (language == AppLanguage.HINDI) "जोड़ने की तिथि: सबसे पुराना पहले" else "Date Added: Oldest First", if (language == AppLanguage.HINDI) "पुराने जोड़े गए उत्पाद पहले दिखाएं" else "Show products added longest ago first")
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "अपने डिफ़ॉल्ट उत्पाद प्रदर्शन क्रम का चयन करें। यह सेटिंग स्वचालित रूप से आपके कैटलॉग सूचियों पर लागू हो जाएगी।" else "Select your default product display order. This setting will automatically apply across your catalog lists.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )

                                sortOptions.forEach { (option, title, desc) ->
                                    val isSelected = viewModel.appProductSortOption == option
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.updateProductSortOption(option) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.updateProductSortOption(option) }
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = title,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = desc,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsSubPage.ABOUT -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Stylized Premium App Icon / Logo
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory,
                                        contentDescription = "App Logo",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "शॉप प्राइस लिस्ट" else "Shop Price List",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "आपका डिजिटल कैटलॉग सहायक" else "Your Smart Digital Catalog Assistant",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Static Version Section
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "ऐप संस्करण" else "App Version",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "v${viewModel.appVersionName}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 18.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Key Features / Changelog Details
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "हाल के अपडेट और विशेषताएं" else "Recent Updates & Features",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        val bulletPoints = if (language == AppLanguage.HINDI) {
                                            listOf(
                                                "📁 व्यापारी / फ़ोल्डर संपादन और निर्माण विकल्प",
                                                "🏷️ लाइट और डार्क मोड में आइटम की संख्या स्पष्ट दिखना",
                                                "🔄 क्लाउड बैकअप और सिंक सुविधा",
                                                "💻 मोबाइल और पीसी पर लाइव सिंक्रोनाइजेशन",
                                                "🔊 वॉयस-टाइपिंग और हिंदी वॉयस असिस्टेंट",
                                                "🗑️ हटाए गए डेटा के लिए रीसायकल बिन सपोर्ट"
                                            )
                                        } else {
                                            listOf(
                                                "📁 Custom folders rename, edit, and delete options",
                                                "🏷️ Prominent item counters in folders for both Light/Dark themes",
                                                "🔄 Google and cloud backup data-safeguard system",
                                                "💻 Dynamic live mirroring with PC or connected display",
                                                "🔊 Premium multi-lingual voice search assistant",
                                                "🗑️ Recycle bin safety for restoring items and folders"
                                            )
                                        }

                                        bulletPoints.forEach { point ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = point,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Copyright info
                                Text(
                                    text = if (language == AppLanguage.HINDI) "© २०२६ शॉप प्राइस लिस्ट। सभी अधिकार सुरक्षित।" else "© 2026 Shop Price List. All rights reserved.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer Save & Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("close_settings_footer_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (activeSubPage != null) {
                            if (language == AppLanguage.HINDI) "ठीक है" else "Done"
                        } else {
                            Translation.getString("settings_save", language)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isImportingInProgress) showImportConfirmDialog = false 
            },
            title = {
                Text(
                    text = if (importErrorMsg != null) {
                        if (language == AppLanguage.HINDI) "डेटा सत्यापन विफल" else "Data Validation Failed"
                    } else {
                        if (language == AppLanguage.HINDI) "डेटा आयात की पुष्टि करें" else "Confirm Data Import"
                    },
                    fontWeight = FontWeight.Bold,
                    color = if (importErrorMsg != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (importErrorMsg != null) {
                        Text(
                            text = importErrorMsg ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = importSummaryMsg ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isImportingInProgress) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "आयात किया जा रहा है..." else "Importing data...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (importErrorMsg != null) {
                    Button(
                        onClick = { showImportConfirmDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(text = if (language == AppLanguage.HINDI) "ठीक है" else "OK")
                    }
                } else {
                    Button(
                        enabled = !isImportingInProgress,
                        onClick = {
                            parsedProductsToImport?.let { listToImport ->
                                isImportingInProgress = true
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val existingFolders = viewModel.repository.getAllFoldersDirect()
                                        val folderMap = existingFolders.associateBy { it.name.lowercase().trim() }.toMutableMap()
                                        
                                        for (item in listToImport) {
                                            val normalizedCat = item.category.lowercase().trim()
                                            var folderId = folderMap[normalizedCat]?.id
                                            
                                            if (folderId == null) {
                                                val newFolderId = viewModel.repository.insertFolder(item.category, null)
                                                val dummyFolder = com.example.data.FolderEntity(id = newFolderId, name = item.category, parentId = null)
                                                folderMap[normalizedCat] = dummyFolder
                                                folderId = newFolderId
                                            }
                                            
                                            viewModel.repository.insertProduct(
                                                folderId = folderId,
                                                name = item.name,
                                                description = item.description,
                                                costPrice = item.costPrice,
                                                sellingPrice = item.sellingPrice,
                                                wholesalePrice = item.wholesalePrice,
                                                boughtFrom = item.boughtFrom,
                                                priceUnit = item.priceUnit
                                            )
                                        }
                                        
                                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                            isImportingInProgress = false
                                            showImportConfirmDialog = false
                                            Toast.makeText(
                                                context,
                                                if (language == AppLanguage.HINDI) 
                                                    "सफलतापूर्वक ${listToImport.size} उत्पादों को आयात किया गया!" 
                                                    else "Successfully imported ${listToImport.size} products!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                            isImportingInProgress = false
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("confirm_import_btn")
                    ) {
                        Text(text = if (language == AppLanguage.HINDI) "आयात करें" else "Import")
                    }
                }
            },
            dismissButton = {
                if (importErrorMsg == null) {
                    TextButton(
                        enabled = !isImportingInProgress,
                        onClick = { showImportConfirmDialog = false }
                    ) {
                        Text(text = if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
        }
    }
}

// Helper menu row layout for settings
@Composable
fun SettingsMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    testTag: String = "",
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        letterSpacing = 0.25.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.5.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate to sub-setting",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// End of screens

@Composable
fun TrashScreen(viewModel: PriceListViewModel) {
    val language = viewModel.currentLanguage
    val binFolders by viewModel.binFoldersFlow.collectAsState()
    val binProducts by viewModel.binProductsFlow.collectAsState()
    val context = LocalContext.current

    var showEmptyConfirm by remember { mutableStateOf(false) }
    var folderToDeleteConfirm by remember { mutableStateOf<BinFolder?>(null) }
    var productToDeleteConfirm by remember { mutableStateOf<BinProduct?>(null) }

    val bgColor = MaterialTheme.colorScheme.background
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
    val ambientGradient = remember(bgColor, primaryContainer) {
        Brush.verticalGradient(
            colors = listOf(primaryContainer, bgColor)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientGradient)
    ) {
        // Navigation header back arrow
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.currentScreen = Screen.FOLDERS },
                        modifier = Modifier.testTag("trash_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Folders"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Translation.getString("trash_bin", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (binFolders.isNotEmpty() || binProducts.isNotEmpty()) {
                    TextButton(
                        onClick = { showEmptyConfirm = true },
                        modifier = Modifier.testTag("empty_trash_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Translation.getString("empty_bin", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Auto-delete option card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("bin_auto_delete_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "60 दिनों के भीतर स्वतः हटाएं" else "Auto-delete within 60 days",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = viewModel.isBinAutoDeleteEnabled,
                        onCheckedChange = { viewModel.updateBinAutoDeleteEnabled(it) },
                        modifier = Modifier.testTag("bin_auto_delete_switch")
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (language == AppLanguage.HINDI) "चालू होने पर, रीसायकल बिन के आइटम 60 दिनों के बाद स्थायी रूप से स्वतः हट जाएंगे।" else "When enabled, items in the recycle bin are permanently auto-deleted after 60 days.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        if (binFolders.isEmpty() && binProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = Translation.getString("trash_bin_empty", language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                // Folders Section
                if (binFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = if (language == AppLanguage.HINDI) "हटाए गए फ़ोल्डर" else "Deleted Folders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(binFolders) { binFolder ->
                        val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }
                        val folderColorTheme = getFolderColorTheme(binFolder.id, isLightTheme)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = folderColorTheme.background),
                            border = BorderStroke(1.dp, folderColorTheme.borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Folder",
                                        tint = folderColorTheme.iconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = binFolder.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = folderColorTheme.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Restore Button
                                    IconButton(
                                        onClick = {
                                            viewModel.restoreFolderCascade(binFolder)
                                            Toast.makeText(context, Translation.getString("restore_success", language), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("restore_folder_${binFolder.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restore",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    // Permanent Delete Button
                                    IconButton(
                                        onClick = { folderToDeleteConfirm = binFolder },
                                        modifier = Modifier.testTag("perm_delete_folder_${binFolder.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Delete Permanently",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Products Section
                if (binProducts.isNotEmpty()) {
                    item {
                        Text(
                            text = if (language == AppLanguage.HINDI) "हटाए गए उत्पाद" else "Deleted Products",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(binProducts) { binProduct ->
                        val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }
                        val trashProductBg = if (isLightTheme) Color(0xFFFCE8E6) else Color(0xFF331518)
                        val trashProductBorder = if (isLightTheme) Color(0xFFFAD2CF) else Color(0xFFC5221F).copy(alpha = 0.4f)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = trashProductBg),
                            border = BorderStroke(1.dp, trashProductBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = binProduct.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (binProduct.boughtFrom.isNotBlank()) {
                                            Text(
                                                text = "${if (language == AppLanguage.HINDI) "खरीद तिथि" else "Purchase Date"}: ${binProduct.boughtFrom}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Restore
                                        IconButton(
                                            onClick = {
                                                viewModel.restoreProduct(binProduct)
                                                Toast.makeText(context, Translation.getString("restore_success", language), Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.testTag("restore_product_${binProduct.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = "Restore",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        // Permanent Delete
                                        IconButton(
                                            onClick = { productToDeleteConfirm = binProduct },
                                            modifier = Modifier.testTag("perm_delete_product_${binProduct.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = "Delete Permanently",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                if (binProduct.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = binProduct.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "लागत" else "Cost",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "₹${binProduct.costPrice}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "बिक्री" else "Selling",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "₹${binProduct.sellingPrice}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "थोक" else "Wholesale",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "₹${binProduct.wholesalePrice}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog: Empty Trash
    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = {
                Text(
                    text = Translation.getString("empty_bin", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = Translation.getString("bin_clear_confirm", language))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrashBin()
                        showEmptyConfirm = false
                        Toast.makeText(context, Translation.getString("permanent_delete_success", language), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_empty_trash")
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "हाँ, खाली करें" else "Yes, Empty",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text(text = if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog: Delete Folder permanently
    if (folderToDeleteConfirm != null) {
        val f = folderToDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { folderToDeleteConfirm = null },
            title = {
                Text(
                    text = Translation.getString("permanently_delete", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.HINDI)
                        "क्या आप फ़ोल्डर \"${f.name}\" और इसके सभी कंटेंट को स्थायी रूप से हटाना चाहते हैं? यह क्रिया वापस नहीं ली जा सकती।"
                    else
                        "Are you sure you want to permanently delete folder \"${f.name}\" and all of its content? This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.permanentlyDeleteFolder(f)
                        folderToDeleteConfirm = null
                        Toast.makeText(context, Translation.getString("permanent_delete_success", language), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_perm_delete_folder")
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "हाँ, हटाएँ" else "Yes, Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDeleteConfirm = null }) {
                    Text(text = if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog: Delete Product permanently
    if (productToDeleteConfirm != null) {
        val p = productToDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { productToDeleteConfirm = null },
            title = {
                Text(
                    text = Translation.getString("permanently_delete", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.HINDI)
                        "क्या आप उत्पाद \"${p.name}\" को स्थायी रूप से हटाना चाहते हैं? यह क्रिया वापस नहीं ली जा सकती।"
                    else
                        "Are you sure you want to permanently delete product \"${p.name}\"? This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.permanentlyDeleteProduct(p)
                        productToDeleteConfirm = null
                        Toast.makeText(context, Translation.getString("permanent_delete_success", language), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_perm_delete_product")
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "हाँ, हटाएँ" else "Yes, Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDeleteConfirm = null }) {
                    Text(text = if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAiScannerDialog(
    viewModel: PriceListViewModel,
    onDismiss: () -> Unit
) {
    val language = viewModel.currentLanguage
    val context = LocalContext.current
    val folders by viewModel.foldersFlow.collectAsState()
    
    // Dialog flow step: 0 = Setup & Capture, 1 = Scanning, 2 = Generated Receipt View
    var currentStep by remember { mutableStateOf(0) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Scanner, 1 = AI Chatbot & Assistant
    
    // Capture state
    var selectedBillBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Inputs
    var billDate by remember {
        mutableStateOf(
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        )
    }
    
    // Saving Target Choice
    // 0 = Save in Folder, 1 = Do Not Save
    var saveChoice by remember { mutableStateOf(0) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var isFolderDropdownExpanded by remember { mutableStateOf(false) }
    var showNewFolderInput by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    
    val isScanning = viewModel.isScanningInvoice
    val extractedProducts = viewModel.extractedProducts

    // Launchers
    // Camera Click Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBillBitmap = bitmap
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                if (bmp != null) {
                    selectedBillBitmap = bmp
                } else {
                    Toast.makeText(context, "गैलरी से फोटो लोड करने में असमर्थ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Initialize selected folder if not set
    LaunchedEffect(folders) {
        if (selectedFolderId == null) {
            selectedFolderId = viewModel.selectedFolder?.id ?: folders.firstOrNull()?.id
        }
    }
    
    // Watch scanning status transitions
    LaunchedEffect(isScanning) {
        if (!isScanning && currentStep == 1) {
            if (extractedProducts.isNotEmpty()) {
                currentStep = 2 // Move to Generated Receipt View
            } else {
                currentStep = 0 // Return to Setup/Capture if scanning failed or returned empty
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .testTag("ai_scanner_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "Gemini एआई चैट सहायक 🤖" else "Gemini AI Chat Assistant 🤖",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Chat")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))



                if (false) { // Disabled scanner
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    if (currentStep == 0) {
                        // --- STEP 0: Capture photo, input options ---
                        
                        // Help label
                        Text(
                            text = "बिल का फोटो खींचें या गैलरी से चुनें। यह आपकी रसीद को स्वचालित रूप से हिंदी में बदल देगा और चुनी गई जगह पर सुरक्षित कर देगा।",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        // Action Buttons: Camera click or Gallery
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Camera Click Button (Click photo of bill)
                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("scan_camera_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("फोटो खींचें 📸", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Choose from Gallery Button
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("scan_gallery_btn"),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("गैलरी से चुनें 🖼️", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Display selected/clicked thumbnail
                        selectedBillBitmap?.let { bmp ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Captured bill image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                    // Remove/Clear overlay button
                                    IconButton(
                                        onClick = { selectedBillBitmap = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        } ?: run {
                            // Empty state placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("कोई फोटो नहीं चुनी गई है", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Input Options Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 1. Bill Date
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("१. बिल की तारीख (Bill Date):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = billDate,
                                            onValueChange = { billDate = it },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                        )
                                        IconButton(
                                            onClick = {
                                                val calendar = java.util.Calendar.getInstance()
                                                android.app.DatePickerDialog(
                                                    context,
                                                    { _, year, month, day ->
                                                        val cal = java.util.Calendar.getInstance()
                                                        cal.set(year, month, day)
                                                        billDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cal.time)
                                                    },
                                                    calendar.get(java.util.Calendar.YEAR),
                                                    calendar.get(java.util.Calendar.MONTH),
                                                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                                ).show()
                                            },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                                                .size(48.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }

                                // 2. Where to save choice
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("२. तस्वीर कहाँ सहेजें? (Where to save?):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    
                                    // Option Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = saveChoice == 0,
                                            onClick = { saveChoice = 0 },
                                            label = { Text("फ़ोल्डर में सहेजें (Save to Folder)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = saveChoice == 1,
                                            onClick = { saveChoice = 1 },
                                            label = { Text("सहेजना नहीं चाहते (Do not save)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // 3. Folder dropdown (only if Save Choice == 0)
                                if (saveChoice == 0) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("३. इन फ़ोल्डर में सहेजें (Select Folder):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { isFolderDropdownExpanded = true }
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            val currentTargetFolder = folders.find { it.id == selectedFolderId }
                                            val currentTargetFolderIdx = remember(folders, currentTargetFolder?.id) {
                                                currentTargetFolder?.let { folders.indexOfFirst { f -> f.id == it.id } } ?: -1
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = currentTargetFolder?.let {
                                                        if (viewModel.isFolderAutoNumberingEnabled && currentTargetFolderIdx != -1) "${currentTargetFolderIdx + 1}. ${it.name}" else it.name
                                                    } ?: "कोई फ़ोल्डर नहीं चुना",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentTargetFolder != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                                )
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                            }

                                            DropdownMenu(
                                                expanded = isFolderDropdownExpanded,
                                                onDismissRequest = { isFolderDropdownExpanded = false }
                                            ) {
                                                folders.forEachIndexed { index, folder ->
                                                    DropdownMenuItem(
                                                        text = { Text(if (viewModel.isFolderAutoNumberingEnabled) "${index + 1}. ${folder.name}" else folder.name, fontWeight = FontWeight.SemiBold) },
                                                        onClick = {
                                                            selectedFolderId = folder.id
                                                            isFolderDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                    text = { Text("+ नया फ़ोल्डर बनाएं (Create New Folder)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                                    onClick = {
                                                        showNewFolderInput = true
                                                        isFolderDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }

                                        if (showNewFolderInput) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = newFolderName,
                                                    onValueChange = { newFolderName = it },
                                                    placeholder = { Text("नया फ़ोल्डर नाम", fontSize = 12.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp),
                                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                                )
                                                Button(
                                                    onClick = {
                                                        if (newFolderName.isNotBlank()) {
                                                            val name = newFolderName.trim()
                                                            viewModel.viewModelScope.launch {
                                                                viewModel.lastLocalWriteTime = System.currentTimeMillis()
                                                                val id = viewModel.repository.insertFolder(name, null)
                                                                selectedFolderId = id
                                                                showNewFolderInput = false
                                                                newFolderName = ""
                                                                viewModel.syncToCloudSilent()
                                                            }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                                ) {
                                                    Text("बनाएं", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit scan button
                        Button(
                            onClick = {
                                val bitmapToScan = selectedBillBitmap ?: viewModel.generateSampleEnglishBillBitmap()
                                if (selectedBillBitmap == null) {
                                    selectedBillBitmap = bitmapToScan
                                }
                                currentStep = 1
                                viewModel.scanInvoiceImageWithGemini(bitmapToScan)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_image_scan_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedBillBitmap != null) "स्कैन करें और हिंदी बिल इमेज बनाएं 🚀" else "सैंपल बिल से डेमो करें 🚀",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                    } else if (currentStep == 1) {
                        // --- STEP 1: Scanning / Progress animation ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "AI हिंदी बिल इमेज जनरेट कर रहा है...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "कृपया प्रतीक्षा करें। Gemini मॉडल आपके बिल का विश्लेषण कर रहा है और हिंदी में अनुवाद कर रहा है।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else if (currentStep == 2) {
                        // --- STEP 2: Scanned results / High-fidelity "Hindi Image/Receipt" ---
                        Text(
                            text = "🎉 सफलतापूर्वक जनरेट की गई रसीद इमेज (Hindi Digital Receipt):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF4CAF50)
                        )

                        // HIGH-FIDELITY DESIGNED DIGITAL RECEIPTS CARD ("automatically generates a Hindi image")
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, shape = RoundedCornerShape(16.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Tear-off edge at top
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .offset(y = (-16).dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    repeat(15) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                                        )
                                    }
                                }

                                // Receipt Header
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "डिजिटल बिल रसीद",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "=========================",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Shop Name
                                    Text(
                                        text = viewModel.scannedInvoiceSupplier,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bill Metadata (Date, Bill No)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "दिनांक: $billDate",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "रसीद नं: #AI-${System.currentTimeMillis() % 100000}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "- - - - - - - - - - - - - - - - - - - - - -",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Product Table Header
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("सामान (Item Name)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1.5f))
                                    Text("खरीद मूल्य", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text("बिक्री मूल्य", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }

                                Text(
                                    text = "- - - - - - - - - - - - - - - - - - - - - -",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Products List
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    extractedProducts.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1.5f)) {
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.Black
                                                )
                                                if (item.description.isNotBlank()) {
                                                    Text(
                                                        text = item.description,
                                                        fontSize = 11.sp,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "₹${item.costPrice}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Black,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.End
                                            )
                                            Text(
                                                text = "₹${item.sellingPrice}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Black,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "- - - - - - - - - - - - - - - - - - - - - -",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Totals Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "कुल उत्पाद: ${extractedProducts.size}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "कुल खरीद: ₹${extractedProducts.sumOf { it.costPrice }}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Barcode graphic
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val barcodePattern = listOf(2, 4, 1, 3, 2, 5, 1, 3, 4, 2, 1, 3, 2, 4, 1, 3, 2, 3)
                                    barcodePattern.forEachIndexed { i, width ->
                                        Box(
                                            modifier = Modifier
                                                .width(width.dp)
                                                .fillMaxHeight()
                                                .background(if (i % 2 == 0) Color.Black else Color.Transparent)
                                        )
                                        Spacer(modifier = Modifier.width(1.5.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Watermark Approved stamp
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .border(1.5.dp, Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .graphicsLayer(rotationZ = -10f)
                                ) {
                                    Text(
                                        text = "AI सत्यापित ✨",
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Error message if any (shown in Step 0 and Step 2)
                    viewModel.scannerErrorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                } else {
                    // Gemini AI Chatbot layout
                    ChatBotLayout(viewModel = viewModel, language = language)
                }

                if (false) { // Disabled scanner footer
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Footer action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep == 0) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(text = "रद्द करें (Cancel)", fontWeight = FontWeight.Bold)
                            }
                        } else if (currentStep == 1) {
                            TextButton(
                                onClick = {
                                    currentStep = 0
                                    viewModel.isScanningInvoice = false
                                },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(text = "वापस जाएं (Back)", fontWeight = FontWeight.Bold)
                            }
                        } else if (currentStep == 2) {
                            if (saveChoice == 0) {
                                // Save to folder
                                Button(
                                    onClick = {
                                        val destId = selectedFolderId
                                        if (destId != null) {
                                            val parsedTimestamp = try {
                                                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).parse(billDate)?.time ?: System.currentTimeMillis()
                                            } catch (e: Exception) {
                                                System.currentTimeMillis()
                                            }
                                            viewModel.viewModelScope.launch {
                                                viewModel.lastLocalWriteTime = System.currentTimeMillis()
                                                for (p in extractedProducts) {
                                                    viewModel.repository.restoreProductRaw(
                                                        ProductEntity(
                                                            folderId = destId,
                                                            name = p.name,
                                                            description = p.description ?: "",
                                                            costPrice = p.costPrice,
                                                            sellingPrice = p.sellingPrice,
                                                            wholesalePrice = p.wholesalePrice,
                                                            boughtFrom = p.boughtFrom,
                                                            priceUnit = p.priceUnit,
                                                            createdAt = parsedTimestamp
                                                        )
                                                    )
                                                }
                                                viewModel.extractedProducts = emptyList()
                                                viewModel.syncToCloudSilent()
                                            }
                                            Toast.makeText(context, "उत्पाद सफलतापूर्वक फोल्डर में सहेजे गए! ✅", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "कृपया पहले एक फ़ोल्डर चुनें!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(48.dp)
                                        .testTag("import_parsed_items_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("फ़ोल्डर में सहेजें 💾", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                // Do not save
                                Button(
                                    onClick = {
                                        viewModel.extractedProducts = emptyList()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("बिना सहेजे बंद करें ❌", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotLayout(
    viewModel: PriceListViewModel,
    language: AppLanguage
) {
    val context = LocalContext.current
    val chatMessages = viewModel.chatMessages
    val isSending = viewModel.isSendingChatMessage
    val errorMessage = viewModel.chatErrorMessage
    
    var chatInputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Auto-scroll to bottom of chat when new message arrives
    LaunchedEffect(chatMessages.size, isSending) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    // Launchers for Chat Image Attachment
    val chatCameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            attachedBitmap = bitmap
        }
    }

    val chatGalleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bmp != null) {
                    attachedBitmap = bmp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chatbot_container"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Selector row for Models & Persona
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Models Selector Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Gemini Model चुनें:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Model explanation helper label
                    Text(
                        text = when (viewModel.chatSelectedModel) {
                            "gemini-3.1-pro-preview" -> "🔥 Deep Thinking Mode (HIGH)"
                            "gemini-3.5-flash" -> "⚡ Balanced & Intelligent"
                            else -> "⚡ Fast & Light"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.chatSelectedModel == "gemini-3.1-pro-preview") {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }
                
                // Model buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val models = listOf(
                        Triple("gemini-3.1-flash-lite-preview", "Fast ⚡", "Fast & simple"),
                        Triple("gemini-3.5-flash", "General 🤖", "Balanced"),
                        Triple("gemini-3.1-pro-preview", "Deep Think 🧠", "High Reasoning")
                    )
                    
                    models.forEach { (modelId, label, _) ->
                        val isSelected = viewModel.chatSelectedModel == modelId
                        Surface(
                            onClick = { viewModel.chatSelectedModel = modelId },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                
                // Roles selector
                Text(
                    text = "AI का रोल / व्यक्तित्व चुनें:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    viewModel.chatPresetRoles.forEach { role ->
                        val isSelected = viewModel.chatSelectedRoleId == role.id
                        Surface(
                            onClick = { viewModel.chatSelectedRoleId = role.id },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    text = role.name.substringBefore(" ("),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 2. Chat messages viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
        ) {
            if (chatMessages.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "बातचीत शुरू करें! 💬",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "आप उत्पाद की कीमतें, मार्जिन या दुकान को कैसे बेहतर बनाया जाए, इस बारे में कोई भी प्रश्न पूछ सकते हैं। आप बिल या रसीद का फोटो अपलोड करके विश्लेषण भी करवा सकते हैं!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages) { msg ->
                        ChatBubbleItem(msg)
                    }
                    
                    if (isSending) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (viewModel.chatSelectedModel == "gemini-3.1-pro-preview") {
                                        "गहन सोच चल रही है (Deep thinking in progress...)"
                                    } else {
                                        "AI जवाब सोच रहा है..."
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
                
                // Clear button at top corner
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Chat", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        // 3. Error state if any
        errorMessage?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        // 4. Attached image preview (if any)
        attachedBitmap?.let { bmp ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Attached thumbnail",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Text(
                        text = "इमेज अटैच की गई 🖼️",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { attachedBitmap = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove attachment", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        // 5. Input bottom bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attachment options button
            IconButton(
                onClick = {
                    chatGalleryLauncher.launch("image/*")
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Attach from gallery", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(
                onClick = {
                    chatCameraLauncher.launch(null)
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Capture from Camera", tint = MaterialTheme.colorScheme.primary)
            }
            
            // Input field
            OutlinedTextField(
                value = chatInputText,
                onValueChange = { chatInputText = it },
                placeholder = { Text("प्रश्न पूछें...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                singleLine = false,
                maxLines = 3,
                trailingIcon = {
                    if (chatInputText.isNotBlank() || attachedBitmap != null) {
                        IconButton(
                            onClick = {
                                val textToSend = chatInputText.trim()
                                val bitmapToSend = attachedBitmap
                                chatInputText = ""
                                attachedBitmap = null
                                viewModel.sendChatMessage(textToSend, bitmapToSend)
                            },
                            enabled = !isSending
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send Message", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubbleItem(msg: com.example.ui.viewmodel.ChatBotMessage) {
    val bubbleColor = if (msg.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (msg.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
    val shape = if (msg.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            contentColor = textColor,
            shape = shape,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // If there's an image attached to user message
                msg.imageBase64?.let { base64 ->
                    val bitmap = remember(base64) {
                        try {
                            val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
                
                if (msg.text.isNotBlank()) {
                    Text(
                        text = msg.text,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        // Small timestamp label
        val timeStr = remember(msg.timestamp) {
            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
        }
        Text(
            text = timeStr,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

// ==========================================
// PREMIUM & CLASSY STRUCTURAL UI ADDITIONS
// ==========================================

@Composable
fun FloatingPremiumBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onAiScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    language: AppLanguage
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Tab 1: Catalog / Categories list
            val isCatalogActive = currentScreen == Screen.FOLDERS || currentScreen == Screen.PRODUCTS
            BottomBarItem(
                icon = if (isCatalogActive) Icons.Default.Storefront else Icons.Outlined.Storefront,
                label = if (language == AppLanguage.HINDI) "कैटलॉग" else "Catalog",
                isActive = isCatalogActive,
                testTag = "bottom_nav_catalog",
                onClick = { onNavigate(Screen.FOLDERS) }
            )

            // Tab 2: Recycle Bin / Trash
            val isTrashActive = currentScreen == Screen.TRASH
            BottomBarItem(
                icon = if (isTrashActive) Icons.Default.Delete else Icons.Default.DeleteOutline,
                label = if (language == AppLanguage.HINDI) "कचरा" else "Trash",
                isActive = isTrashActive,
                testTag = "bottom_nav_trash",
                onClick = { onNavigate(Screen.TRASH) }
            )

            // Tab 3: AI Scanner (Elevated prominent center button)
            FloatingAiScanButton(
                onClick = onAiScanClick,
                language = language
            )

            // Tab 4: Settings Dialog Open
            BottomBarItem(
                icon = Icons.Default.Settings,
                label = if (language == AppLanguage.HINDI) "सेटिंग्स" else "Settings",
                isActive = false,
                testTag = "settings_button", // Match automated tests targeting settings button!
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
fun RowScope.BottomBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
        
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(44.dp)
                .background(
                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = fontWeight,
            color = tint
        )
    }
}

@Composable
fun FloatingAiScanButton(
    onClick: () -> Unit,
    language: AppLanguage
) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("ai_scanner_button") // Match automated tests targeting AI scan!
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Chatbot",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (language == AppLanguage.HINDI) "एआई चैट 🤖" else "AI Chat 🤖",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InteractiveDashboardBanner(
    viewModel: PriceListViewModel,
    folders: List<FolderEntity>,
    allProducts: List<ProductEntity>,
    language: AppLanguage
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val totalFolders = folders.size
    val totalItems = allProducts.size
    
    val avgSellingPrice = remember(allProducts) {
        if (allProducts.isEmpty()) 0.0 else allProducts.map { it.sellingPrice }.average()
    }
    
    val avgMargin = remember(allProducts) {
        if (allProducts.isEmpty()) 0.0 else {
            val valid = allProducts.filter { it.sellingPrice >= it.costPrice }
            if (valid.isEmpty()) 0.0 else valid.map { it.sellingPrice - it.costPrice }.average()
        }
    }
    
    val topCategory = remember(folders, allProducts) {
        if (folders.isEmpty() || allProducts.isEmpty()) null
        else {
            folders.filter { it.parentId == null }.maxByOrNull { folder ->
                allProducts.count { it.folderId == folder.id }
            }
        }
    }
    val topCategoryCount = remember(topCategory, allProducts) {
        topCategory?.let { folder -> allProducts.count { it.folderId == folder.id } } ?: 0
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 4.dp, 
                shape = RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(18.dp)
        ) {
            // Main content area with AnimatedContent for smooth premium transitions
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) with 
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(220))
                },
                label = "dashboard_content"
            ) { tab ->
                when (tab) {
                    0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "शॉप ओवरव्यू" else "Shop Overview",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "कैटलॉग सारांश" else "Catalog Summary",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text(
                                            text = "$totalFolders",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "श्रेणियां" else "Categories",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "$totalItems",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "सामग्रियां" else "Items",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                    1 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "प्राइसिंग आँकड़े" else "Pricing Analytics",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "औसत मूल्य विश्लेषण" else "Average Valuation",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text(
                                            text = "₹${String.format("%.1f", avgSellingPrice)}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "औसत बिक्री" else "Avg Retail",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (viewModel.showCostAndMargins) {
                                        Column {
                                            Text(
                                                text = "₹${String.format("%.1f", avgMargin)}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Text(
                                                text = if (language == AppLanguage.HINDI) "औसत लाभ" else "Avg Margin",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        )
                                        }
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                    2 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "शीर्ष प्रदर्शन केटेगरी" else "Top Performance",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = topCategory?.name ?: (if (language == AppLanguage.HINDI) "कोई केटेगरी नहीं" else "No Categories"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text(
                                            text = "$topCategoryCount",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "सामग्रियां" else "Products",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                    Column {
                                        val pct = if (totalItems > 0) (topCategoryCount.toDouble() / totalItems * 100).toInt() else 0
                                        Text(
                                            text = "$pct%",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "कुल का हिस्सा" else "Share %",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Selector Mini-Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tabLabels = listOf(
                    if (language == AppLanguage.HINDI) "शॉप" else "Shop",
                    if (language == AppLanguage.HINDI) "वित्तीय" else "Financials",
                    if (language == AppLanguage.HINDI) "शीर्ष" else "Leader"
                )
                tabLabels.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// Package-level helper functions for CSV and PDF backup exports
fun exportPriceListToPdf(
    context: android.content.Context,
    folders: List<com.example.data.FolderEntity>,
    products: List<com.example.data.ProductEntity>,
    shopName: String,
    shopTagline: String,
    showCost: Boolean
): java.io.File? {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    
    // Set page parameters (A4 size: 595 x 842 points)
    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    
    var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    
    val paintText = android.graphics.Paint().apply {
        textSize = 10f
        isAntiAlias = true
        color = android.graphics.Color.BLACK
    }
    
    val paintBold = android.graphics.Paint().apply {
        textSize = 10f
        isFakeBoldText = true
        isAntiAlias = true
        color = android.graphics.Color.BLACK
    }
    
    val paintHeader = android.graphics.Paint().apply {
        textSize = 13f
        isFakeBoldText = true
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#1A237E") // Rich Indigo color for Category header
    }
    
    val paintTitle = android.graphics.Paint().apply {
        textSize = 20f
        isFakeBoldText = true
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#0D47A1") // Dark Blue for Shop title
    }
    
    val paintLine = android.graphics.Paint().apply {
        strokeWidth = 1f
        color = android.graphics.Color.LTGRAY
    }
    
    var y = 50f
    
    // Title/Header
    canvas.drawText(shopName.ifBlank { "Shop Price List" }, 40f, y, paintTitle)
    y += 20f
    if (shopTagline.isNotBlank()) {
        val paintTag = android.graphics.Paint().apply {
            textSize = 10f
            isAntiAlias = true
            color = android.graphics.Color.GRAY
        }
        canvas.drawText(shopTagline, 40f, y, paintTag)
        y += 15f
    }
    
    val dateText = android.text.format.DateFormat.format("dd-MM-yyyy hh:mm a", java.util.Date()).toString()
    val paintDate = android.graphics.Paint().apply {
        textSize = 9f
        isAntiAlias = true
        color = android.graphics.Color.GRAY
    }
    canvas.drawText("Backup Generated: $dateText", 40f, y, paintDate)
    y += 35f
    
    // Group products by folderId
    val productsByFolder = products.groupBy { it.folderId }
    
    for (folder in folders) {
        val folderProducts = productsByFolder[folder.id] ?: emptyList()
        if (folderProducts.isEmpty()) continue
        
        // Ensure category header fits
        if (y > 760f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
        }
        
        // Draw category header
        canvas.drawText(folder.name, 40f, y, paintHeader)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f
        
        // Table Headers
        canvas.drawText("Item Name", 40f, y, paintBold)
        if (showCost) {
            canvas.drawText("Cost", 240f, y, paintBold)
        }
        canvas.drawText("Selling", 320f, y, paintBold)
        canvas.drawText("Wholesale", 400f, y, paintBold)
        canvas.drawText("Unit", 480f, y, paintBold)
        
        y += 6f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 16f
        
        for (prod in folderProducts) {
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
                
                // Redraw table headers on new page
                canvas.drawText("${folder.name} (Continued)", 40f, y, paintHeader)
                y += 8f
                canvas.drawLine(40f, y, 555f, y, paintLine)
                y += 18f
                
                canvas.drawText("Item Name", 40f, y, paintBold)
                if (showCost) {
                    canvas.drawText("Cost", 240f, y, paintBold)
                }
                canvas.drawText("Selling", 320f, y, paintBold)
                canvas.drawText("Wholesale", 400f, y, paintBold)
                canvas.drawText("Unit", 480f, y, paintBold)
                y += 6f
                canvas.drawLine(40f, y, 555f, y, paintLine)
                y += 16f
            }
            
            // Name: wrap or truncate if too long
            val displayName = if (prod.name.length > 30) prod.name.take(27) + "..." else prod.name
            canvas.drawText(displayName, 40f, y, paintText)
            
            if (showCost) {
                canvas.drawText("₹${prod.costPrice}", 240f, y, paintText)
            }
            canvas.drawText("₹${prod.sellingPrice}", 320f, y, paintText)
            canvas.drawText("₹${prod.wholesalePrice}", 400f, y, paintText)
            canvas.drawText(prod.priceUnit, 480f, y, paintText)
            
            y += 20f
        }
        
        y += 15f // margin between folders
    }
    
    pdfDocument.finishPage(page)
    
    val cleanShopName = shopName.ifBlank { "shop" }.replace(Regex("[^a-zA-Z0-9]"), "_")
    val backupFile = java.io.File(context.cacheDir, "${cleanShopName}_price_list.pdf")
    try {
        java.io.FileOutputStream(backupFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        pdfDocument.close()
    }
    
    return backupFile
}

fun exportPriceListToCsv(
    context: android.content.Context,
    folders: List<com.example.data.FolderEntity>,
    products: List<com.example.data.ProductEntity>,
    shopName: String,
    showCost: Boolean
): java.io.File? {
    val csvBuilder = java.lang.StringBuilder()
    
    // Header
    if (showCost) {
        csvBuilder.append("\"Category\",\"Product Name\",\"Description\",\"Cost Price\",\"Selling Price\",\"Wholesale Price\",\"Bought From\",\"Price Unit\"\n")
    } else {
        csvBuilder.append("\"Category\",\"Product Name\",\"Description\",\"Selling Price\",\"Wholesale Price\",\"Price Unit\"\n")
    }
    
    val foldersMap = folders.associateBy { it.id }
    for (prod in products) {
        val folderName = foldersMap[prod.folderId]?.name ?: "Uncategorized"
        
        fun escape(s: String): String {
            return "\"" + s.replace("\"", "\"\"") + "\""
        }
        
        csvBuilder.append(escape(folderName)).append(",")
        csvBuilder.append(escape(prod.name)).append(",")
        csvBuilder.append(escape(prod.description)).append(",")
        if (showCost) {
            csvBuilder.append(prod.costPrice).append(",")
        }
        csvBuilder.append(prod.sellingPrice).append(",")
        csvBuilder.append(prod.wholesalePrice).append(",")
        if (showCost) {
            csvBuilder.append(escape(prod.boughtFrom)).append(",")
        }
        csvBuilder.append(escape(prod.priceUnit)).append("\n")
    }
    
    val cleanShopName = shopName.ifBlank { "shop" }.replace(Regex("[^a-zA-Z0-9]"), "_")
    val backupFile = java.io.File(context.cacheDir, "${cleanShopName}_price_list.csv")
    try {
        backupFile.writeText(csvBuilder.toString(), charset = Charsets.UTF_8)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
    return backupFile
}

fun shareBackupFile(context: android.content.Context, file: java.io.File, mimeType: String, title: String) {
    try {
        val uri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, title))
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

// Data class and parsing helper functions for CSV import validation
data class ParsedProduct(
    val category: String,
    val name: String,
    val description: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val wholesalePrice: Double,
    val boughtFrom: String,
    val priceUnit: String
)

fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val curVal = java.lang.StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        if (ch == '\"') {
            if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                curVal.append('\"')
                i++ // Skip the second quote
            } else {
                inQuotes = !inQuotes
            }
        } else if (ch == ',' && !inQuotes) {
            result.add(curVal.toString().trim())
            curVal.setLength(0)
        } else {
            curVal.append(ch)
        }
        i++
    }
    result.add(curVal.toString().trim())
    return result
}

fun padList(list: List<String>, size: Int): List<String> {
    if (list.size >= size) return list
    return list + List(size - list.size) { "" }
}



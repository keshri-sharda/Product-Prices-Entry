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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
        Color(0xFFFFECED), // Soft Rose Pink
        Color(0xFFE8FDF0), // Soft Mint Green
        Color(0xFFE8F4FD), // Soft Light Blue
        Color(0xFFFFF6ED), // Soft Creamy Peach
        Color(0xFFF1EAFF), // Soft Lavender Purple
        Color(0xFFFFFDEC), // Soft Butter Yellow
        Color(0xFFE8FCFF), // Soft Robin Egg Teal
        Color(0xFFFFF0FE), // Soft Delicate Violet
        Color(0xFFFFF0EB), // Soft Warm Coral
        Color(0xFFF2FDE6), // Soft Green Sage
        Color(0xFFE8FAF6), // Soft Clean Cyan
        Color(0xFFFFFAEB)  // Soft Creamy Yellow Custard
    )
    val lightBorderPastels = listOf(
        Color(0xFFFFC0C3), // Matching darker rose pink
        Color(0xFFB2EBC3), // Matching darker mint green
        Color(0xFFBBDEFB), // Matching darker light blue
        Color(0xFFFFDAB9), // Matching darker creamy peach
        Color(0xFFE1D0FF), // Matching darker lavender purple
        Color(0xFFFFF59D), // Matching darker butter yellow
        Color(0xFFB2EBF2), // Matching darker robin egg teal
        Color(0xFFF8BBD0), // Matching darker delicate violet
        Color(0xFFFFCCBC), // Matching darker warm coral
        Color(0xFFDCEDC8), // Matching darker green sage
        Color(0xFFB2DFDB), // Matching darker clean cyan
        Color(0xFFFFE082)  // Matching darker creamy yellow custard
    )
    val lightDarkerTones = listOf(
        Color(0xFFD81B60), // Rose Darker
        Color(0xFF1E88E5), // Blue Darker
        Color(0xFF3949AB), // Indigo Darker
        Color(0xFFF4511E), // Orange Darker
        Color(0xFF8E24AA), // Purple Darker
        Color(0xFFF9A825), // Yellow Darker
        Color(0xFF00ACC1), // Cyan Darker
        Color(0xFFD81B60), // Violet/Orchid Darker
        Color(0xFFE64A19), // Coral Darker
        Color(0xFF43A047), // Sage/Lime Darker
        Color(0xFF00897B), // Mint Darker
        Color(0xFF6D4C41)  // Cream/Brown Darker
    )
    val darkPastels = listOf(
        Color(0xFF422123), // Dark Muted Coral Rose
        Color(0xFF183821), // Dark Muted Forest Mint
        Color(0xFF1D3249), // Dark Muted Twilight Blue
        Color(0xFF422F1D), // Dark Muted Caramel Orange
        Color(0xFF2E1C4B), // Dark Muted Lavender Purple
        Color(0xFF3D361F), // Dark Muted Olive Gold
        Color(0xFF1D393E), // Dark Muted Dark Teal
        Color(0xFF3B1B37), // Dark Muted Plum Orchid
        Color(0xFF42241F), // Dark Muted Spice Coral
        Color(0xFF293B1F), // Dark Muted Basil Sage
        Color(0xFF1C3D34), // Dark Muted Ocean Wave
        Color(0xFF3D3A20)  // Dark Muted Honey Gold
    )
    val darkBorderPastels = listOf(
        Color(0xFF6E3A3C), 
        Color(0xFF2E5E3A), 
        Color(0xFF325475), 
        Color(0xFF684E34), 
        Color(0xFF4C3374), 
        Color(0xFF615738), 
        Color(0xFF325A62), 
        Color(0xFF5E3158), 
        Color(0xFF6E4038), 
        Color(0xFF455F37), 
        Color(0xFF336254), 
        Color(0xFF5F5B39)  
    )
    val darkLighterTones = listOf(
        Color(0xFFFFB2B7), // Pink Light Tint
        Color(0xFFA1E3AF), // Green Light Tint
        Color(0xFFABCFFF), // Blue Light Tint
        Color(0xFFFFCE9F), // Peach Light Tint
        Color(0xFFDFCDFF), // Purple Light Tint
        Color(0xFFFFF2A3), // Yellow Light Tint
        Color(0xFFA3EFF8), // Cyan Light Tint
        Color(0xFFFFA9EB), // Orchid Light Tint
        Color(0xFFFFB6A3), // Coral Light Tint
        Color(0xFFC0E5A6), // Sage Light Tint
        Color(0xFFA6E5D9), // Teal Light Tint
        Color(0xFFFFF7A7)  // Gold Light Tint
    )
    val hash = folderId.hashCode()
    val index = (hash % 12).let { if (it < 0) -it else it }
    
    return if (isLightTheme) {
        FolderColorTheme(
            background = lightPastels[index],
            primaryText = Color(0xFF2E2E2E),
            subText = lightDarkerTones[index].copy(alpha = 0.85f),
            iconTint = lightDarkerTones[index],
            borderColor = lightBorderPastels[index]
        )
    } else {
        FolderColorTheme(
            background = darkPastels[index],
            primaryText = Color(0xFFEEEEEE),
            subText = darkLighterTones[index].copy(alpha = 0.85f),
            iconTint = darkLighterTones[index],
            borderColor = darkBorderPastels[index]
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListAppScreen(viewModel: PriceListViewModel) {
    val context = LocalContext.current
    val language = viewModel.currentLanguage
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAiScannerDialog by remember { mutableStateOf(false) }

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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
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
                },
                actions = {
                    // Language Switcher Toggle button
                    Button(
                        onClick = { viewModel.toggleLanguage() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("language_switch_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Translate Icon",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.ENGLISH) "हिंदी (HI)" else "English (EN)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }



                    // AI Scanner Dialog display button
                    if (viewModel.isLoggedIn) {
                        IconButton(
                            onClick = { showAiScannerDialog = true },
                            modifier = Modifier.testTag("ai_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Smart AI Scanner",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Settings Sheet display button
                    if (viewModel.isLoggedIn) {
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }

                    // Logout/Lock Screen Switcher
                    if (viewModel.isLoggedIn) {
                        IconButton(
                            onClick = {
                                viewModel.isLoggedIn = false
                                viewModel.currentScreen = Screen.LOGIN
                            },
                            modifier = Modifier.testTag("lock_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Safe"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
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
                    }
                )
                Screen.TRASH -> TrashScreen(viewModel = viewModel)
            }
        }
    }

    if (showSettingsSheet) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showAiScannerDialog) {
        SmartAiScannerDialog(
            viewModel = viewModel,
            onDismiss = { showAiScannerDialog = false }
        )
    }

    // Custom Add Folder dialog (top-level so accessible from all screens)
    if (viewModel.showFolderDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showFolderDialog = false },
            title = {
                Text(
                    text = Translation.getString("add_folder", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Translation.getString("folder_name", language),
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
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            val active = viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "folder_name"
                            IconButton(
                                onClick = {
                                    viewModel.activeVoiceTargetField = "folder_name"
                                    val recordPermission = Manifest.permission.RECORD_AUDIO
                                    val hasPermission = ContextCompat.checkSelfPermission(context, recordPermission) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        val localeCode = if (viewModel.currentLanguage == AppLanguage.HINDI) "hi-IN" else "en-US"
                                        voiceHelper.startListening(localeCode)
                                    } else {
                                        permissionLauncher.launch(recordPermission)
                                    }
                                },
                                modifier = Modifier.testTag("folder_mic_icon_btn")
                            ) {
                                Icon(
                                    imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                    contentDescription = "Voice Input Folder Name",
                                    tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    if (viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "folder_name") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = Translation.getString("voice_typing_active", language),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Simulated Speak pre-fill buttons for quick mock/simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.activeVoiceTargetField = "folder_name"
                                val recordPermission = Manifest.permission.RECORD_AUDIO
                                val hasPermission = ContextCompat.checkSelfPermission(context, recordPermission) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    val localeCode = if (viewModel.currentLanguage == AppLanguage.HINDI) "hi-IN" else "en-US"
                                    voiceHelper.startListening(localeCode)
                                } else {
                                    permissionLauncher.launch(recordPermission)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "folder_name") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = if (viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "folder_name") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f).testTag("folder_voice_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "folder_name") Icons.Default.MicNone else Icons.Default.Mic,
                                contentDescription = "voice microphone",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewModel.isVoiceListening && viewModel.activeVoiceTargetField == "folder_name") "..." else Translation.getString("voice_typing_btn", language),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.activeVoiceTargetField = "folder_name"
                                viewModel.autoSimulateVoiceInput()
                            },
                            modifier = Modifier.weight(1.3f).testTag("folder_simulate_voice_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoMode, contentDescription = "simulate voice", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translation.getString("fill_fields", language).split(" ").first() + " (Simulate)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createFolder() },
                    modifier = Modifier.testTag("folder_submit_btn")
                ) {
                    Text(text = Translation.getString("create", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showFolderDialog = false }) {
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
fun LoginScreen(viewModel: PriceListViewModel) {
    val language = viewModel.currentLanguage
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(if (viewModel.loginMethod == "cloud") 1 else 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Beautiful Store Front Logo picture
        Card(
            modifier = Modifier
                .size(110.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
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
            text = Translation.getString("login_title", language),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = Translation.getString("login_subtitle", language),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Dual Mode Tabs: Passcode Lock vs Cloud Multi-Device Sync
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .padding(bottom = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { 
                    activeTab = 0
                    viewModel.loginMethod = "pin"
                    viewModel.saveCloudCredentials()
                },
                text = { Text(Translation.getString("tab_passcode", language), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { 
                    activeTab = 1
                    viewModel.loginMethod = "cloud"
                    viewModel.saveCloudCredentials()
                },
                text = { Text(Translation.getString("tab_cloud_sync", language), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        if (activeTab == 0) {
            // --- PASSCODE / PIN TAB ENTRY ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(4) { idx ->
                    val filled = viewModel.enteredPasscode.length > idx
                    Card(
                        modifier = Modifier
                            .size(44.dp)
                            .border(
                                width = 2.dp,
                                color = if (viewModel.loginError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (filled) {
                                if (viewModel.loginError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (filled) {
                                Text(
                                    text = "●",
                                    fontSize = 16.sp,
                                    color = if (viewModel.loginError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (viewModel.loginError) {
                Text(
                    text = Translation.getString("passcode_err", language),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Beautiful Grid Keypad for Shop Counter Entry
            Card(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val keysList = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("⌫", "0", "🔓")
                    )

                    keysList.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                IconButton(
                                    onClick = {
                                        viewModel.loginError = false
                                        if (key == "⌫") {
                                            if (viewModel.enteredPasscode.isNotEmpty()) {
                                                viewModel.enteredPasscode = viewModel.enteredPasscode.dropLast(1)
                                            }
                                        } else if (key == "🔓") {
                                            viewModel.attemptLogin()
                                        } else {
                                            if (viewModel.enteredPasscode.length < 4) {
                                                viewModel.enteredPasscode += key
                                                if (viewModel.enteredPasscode.length == 4) {
                                                    // Automatic validation upon 4th entry
                                                    viewModel.attemptLogin()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (key) {
                                                "🔓" -> MaterialTheme.colorScheme.primaryContainer
                                                "⌫" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            }
                                        )
                                        .testTag("keypad_btn_$key")
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (key) {
                                            "🔓" -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bypass security link
            TextButton(
                onClick = { viewModel.bypassLogin() },
                modifier = Modifier.testTag("bypass_login_link")
            ) {
                Icon(imageVector = Icons.Default.ArrowOutward, contentDescription = "demo link", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Translation.getString("login_bypass_hint", language),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Helper string indicator (default "1234")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translation.getString("passcode_hint", language),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // --- CLOUD SYNC TAB FOR MULTI-DEVICE SUPPORT ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Translation.getString("tab_cloud_sync", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = viewModel.cloudShopId,
                        onValueChange = { 
                            viewModel.cloudShopId = it
                            viewModel.saveCloudCredentials()
                        },
                        label = { Text(Translation.getString("shop_sync_code", language)) },
                        modifier = Modifier.fillMaxWidth().testTag("cloud_shop_id_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (viewModel.cloudShopId.isNotEmpty()) {
                                IconButton(onClick = { 
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Shop Sync Code", viewModel.cloudShopId)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, Translation.getString("sync_code_copied", language), Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {}
                                }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Shop Sync Code", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )

                    OutlinedTextField(
                        value = viewModel.cloudUsername,
                        onValueChange = { 
                            viewModel.cloudUsername = it
                            viewModel.saveCloudCredentials()
                        },
                        label = { Text(Translation.getString("worker_name", language)) },
                        modifier = Modifier.fillMaxWidth().testTag("cloud_username_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (viewModel.cloudSyncMessage.isNotEmpty()) {
                        Text(
                            text = viewModel.cloudSyncMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (viewModel.isCloudSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (viewModel.cloudShopId.isBlank()) {
                                viewModel.registerNewCloudShop { success, msg ->
                                    if (success) {
                                        viewModel.isLoggedIn = true
                                        viewModel.currentScreen = Screen.FOLDERS
                                    }
                                }
                            } else {
                                viewModel.syncFromCloud { success, msg ->
                                    if (success) {
                                        viewModel.isLoggedIn = true
                                        viewModel.currentScreen = Screen.FOLDERS
                                        Toast.makeText(context, Translation.getString("sync_success", language), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, Translation.getString("sync_error", language) + ": $msg", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cloud_connect_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (viewModel.isCloudSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (viewModel.cloudShopId.isBlank()) Translation.getString("create_new_group", language) else Translation.getString("connect_cloud", language),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (viewModel.cloudShopId.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.registerNewCloudShop { success, msg ->
                                    if (success) {
                                        viewModel.isLoggedIn = true
                                        viewModel.currentScreen = Screen.FOLDERS
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("create_new_group_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = Translation.getString("create_new_group", language), fontWeight = FontWeight.Bold)
                        }
                    }

                    // QR Code scan & display helper section
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = Translation.getString("sync_devices_title", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        text = Translation.getString("scan_qr_desc", language),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Scan QR Code Button
                    Button(
                        onClick = {
                            scanQrCode(context) { scannedCode ->
                                if (scannedCode.isNotBlank()) {
                                    viewModel.cloudShopId = scannedCode
                                    viewModel.saveCloudCredentials()
                                    // Immediately fetch and auto merge!
                                    viewModel.syncFromCloud { success, msg ->
                                        if (success) {
                                            viewModel.isLoggedIn = true
                                            viewModel.currentScreen = Screen.FOLDERS
                                            Toast.makeText(context, Translation.getString("sync_success", language), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, Translation.getString("sync_error", language) + ": $msg", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("qr_scan_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = Translation.getString("scan_qr_btn", language), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (viewModel.cloudShopId.isNotBlank()) {
                        // Display My QR Code Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = Translation.getString("show_qr_btn", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                QrCodeImage(
                                    text = viewModel.cloudShopId,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                )

                                Text(
                                    text = "${Translation.getString("shop_sync_code", language).split(" (")[0]}: ${viewModel.cloudShopId}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
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
fun FoldersScreen(
    viewModel: PriceListViewModel,
    onVoiceTypingClick: () -> Unit
) {
    val language = viewModel.currentLanguage
    val folders by viewModel.foldersFlow.collectAsState()
    val allProducts by viewModel.allProductsFlow.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SyncStatusWidget(viewModel)

            if (viewModel.customShopName.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("shop_branding_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Shop icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = viewModel.customShopName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (viewModel.customShopTagline.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.customShopTagline,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            
            // Hero Banner showing summary metrics
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Translation.getString("folders_title", language),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${folders.size} ${if (language == AppLanguage.HINDI) "केटेगरी" else "folders"} | ${allProducts.size} ${Translation.getString("total_items", language)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = "folder visual",
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            // Search Bar across folders
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = {
                    Text(
                        text = Translation.getString("search_placeholder", language),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "search")
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
                            modifier = Modifier.testTag("mic_btn_folders_search")
                        ) {
                            Icon(
                                imageVector = if (active) Icons.Default.MicNone else Icons.Default.Mic,
                                contentDescription = "Voice Input Search",
                                tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "clear")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("folders_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                            items(searchedFolders, key = { "sf_${it.id}" }) { folder ->
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
                                                text = folder.name,
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
                        items(filteredFolders, key = { it.id }) { folder ->
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
                                        onLongClick = { viewModel.showDeleteFolderConfirm = folder }
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = folderColorTheme.background
                                ),
                                border = BorderStroke(1.2.dp, folderColorTheme.borderColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isLightTheme) 3.dp else 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "Folder category icon",
                                            tint = folderColorTheme.iconTint,
                                            modifier = Modifier.size(32.dp)
                                        )

                                        Column {
                                            Text(
                                                text = folder.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = folderColorTheme.primaryText
                                            )
                                            Text(
                                                text = "$pCount ${Translation.getString("total_items", language)}",
                                                fontSize = 12.sp,
                                                color = folderColorTheme.subText,
                                                fontWeight = FontWeight.Medium
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
fun ProductsScreen(
    viewModel: PriceListViewModel,
    onVoiceTypingClick: () -> Unit,
    onStopVoiceListening: () -> Unit
) {
    val context = LocalContext.current
    val language = viewModel.currentLanguage
    val activeFolder = viewModel.selectedFolder ?: return
    val products by viewModel.filteredProductsFlow.collectAsState(initial = emptyList())
    val folders by viewModel.foldersFlow.collectAsState()
    val allProducts by viewModel.allProductsFlow.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SyncStatusWidget(viewModel)
        
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val parent = folders.find { it.id == activeFolder.parentId }
                        viewModel.selectFolder(parent)
                    },
                    modifier = Modifier.testTag("back_to_folders")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back")
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeFolder.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${products.size} ${Translation.getString("total_items", language)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Layout option selector (Card View vs Rows/Columns Spreadsheet table)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.viewMode = ViewMode.CARD },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.viewMode == ViewMode.CARD) MaterialTheme.colorScheme.primary else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Card View",
                            tint = if (viewModel.viewMode == ViewMode.CARD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.viewMode = ViewMode.TABLE },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.viewMode == ViewMode.TABLE) MaterialTheme.colorScheme.primary else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Table View",
                            tint = if (viewModel.viewMode == ViewMode.TABLE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Search Bar in category
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = {
                Text(
                    text = Translation.getString("search_placeholder", language),
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "search")
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
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "clear")
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("products_search_bar"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )

        // Subfolders inline categorizer bar list
        val subfolders = remember(folders, activeFolder.id) {
            folders.filter { it.parentId == activeFolder.id }
        }

        if (subfolders.isNotEmpty() || viewModel.searchQuery.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Translation.getString("subfolders", language),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    TextButton(
                        onClick = {
                            viewModel.folderParentId = activeFolder.id
                            viewModel.showFolderDialog = true
                        },
                        modifier = Modifier.testTag("add_subfolder_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder, 
                            contentDescription = "Add subfolder", 
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Translation.getString("add_subfolder", language), 
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (subfolders.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "कोई उप-फ़ोल्डर नहीं! नया जोड़ने के लिए ऊपर का बटन दबाएं।" else "No subfolders yet! Click the button above to add one.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subfolders, key = { it.id }) { sub ->
                            val subpCount = remember(allProducts, sub.id) {
                                allProducts.count { it.folderId == sub.id }
                            }
                            val isLightTheme = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue > 1.5f }
                            val folderColorTheme = getFolderColorTheme(sub.id, isLightTheme)

                            Card(
                                modifier = Modifier
                                    .widthIn(min = 130.dp, max = 180.dp)
                                    .testTag("subfolder_card_${sub.id}")
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { viewModel.selectFolder(sub) },
                                        onLongClick = { viewModel.showDeleteFolderConfirm = sub }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = folderColorTheme.background
                                ),
                                border = BorderStroke(1.dp, folderColorTheme.borderColor)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                    Column(modifier = Modifier.padding(end = 20.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "subfolder",
                                            tint = folderColorTheme.iconTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = sub.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = folderColorTheme.primaryText
                                        )
                                        Text(
                                            text = "$subpCount ${Translation.getString("total_items", language)}",
                                            fontSize = 11.sp,
                                            color = folderColorTheme.subText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        }

        if (products.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FormatListBulleted,
                    contentDescription = "No products",
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Translation.getString("no_products", language),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Display view according to choice
            Box(modifier = Modifier.weight(1f)) {
                if (viewModel.viewMode == ViewMode.CARD) {
                    // Card View Listing
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductCardView(viewModel, product, language)
                        }
                    }
                } else {
                    // Excel Spreadsheet Tabular multiple Rows & Columns format!
                    val horizontalScrollState = rememberScrollState()

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Table Header row (Fixed, scrolled horizontally with body items)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .horizontalScroll(horizontalScrollState)
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Translation.getString("name_col", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(130.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Translation.getString("cost_col", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(85.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Translation.getString("whole_col", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(90.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Translation.getString("sell_col", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(85.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Translation.getString("supplier_col", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(100.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Translation.getString("desc_col", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(110.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${if (language == AppLanguage.HINDI) "कार्रवाई" else "Actions"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(90.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Table Body Rows
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(products, key = { it.id }) { product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (products.indexOf(product) % 2 == 0) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                        )
                                        .horizontalScroll(horizontalScrollState)
                                        .padding(vertical = 10.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Row product descriptive content in multiple columns
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(130.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    // Cost Price
                                    Text(
                                        text = "₹${product.costPrice}/${if (product.priceUnit == "kg") "kg" else "pc"}",
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(85.dp),
                                        fontWeight = FontWeight.Medium
                                    )

                                    // Wholesale Price
                                    Text(
                                        text = "₹${product.wholesalePrice}/${if (product.priceUnit == "kg") "kg" else "pc"}",
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(90.dp),
                                        color = GoldColor,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Selling Retail Price
                                    Text(
                                        text = "₹${product.sellingPrice}/${if (product.priceUnit == "kg") "kg" else "pc"}",
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(85.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Supplier
                                    Text(
                                        text = product.boughtFrom.ifEmpty { "N/A" },
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(100.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Description
                                    Text(
                                        text = product.description.ifEmpty { "N/A" },
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(110.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    // Action buttons for spreadsheet editing with safety separation
                                    Row(
                                        modifier = Modifier.width(100.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.openEditProduct(product) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("edit_prod_${product.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit product",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(24.dp))

                                        IconButton(
                                            onClick = { viewModel.showDeleteProductConfirm = product },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("delete_prod_${product.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete product",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }

        // Add Product Floating Button
        LargeFloatingActionButton(
            onClick = { viewModel.openAddProduct() },
            modifier = Modifier
                .align(Alignment.End)
                .padding(24.dp)
                .testTag("add_product_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add Product")
                Text(
                    text = Translation.getString("add_product", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

                        // VOICE TYPING micro-assistant button inside Dialog
                        Button(
                            onClick = { onVoiceTypingClick() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isVoiceListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = if (viewModel.isVoiceListening) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("dialog_voice_assist_btn")
                        ) {
                            Icon(
                                imageVector = if (viewModel.isVoiceListening) Icons.Default.MicNone else Icons.Default.Mic,
                                contentDescription = "voice microphone",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewModel.isVoiceListening) "..." else Translation.getString("voice_typing_btn", language),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (viewModel.isVoiceListening) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = Translation.getString("voice_typing_active", language),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = Translation.getString("save", language))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCardView(viewModel: PriceListViewModel, product: ProductEntity, language: AppLanguage) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = { viewModel.showDeleteProductConfirm = product }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
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
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // Pricing unit simple, colorful pill badge
                        val isKg = product.priceUnit == "kg"
                        val unitColor = if (isKg) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        val unitText = if (isKg) Translation.getString("unit_per_kg", language) else Translation.getString("unit_per_piece", language)
                        
                        Surface(
                            color = unitColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, unitColor.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = unitText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = unitColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                // Row action items with generous safety space
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.openEditProduct(product) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("card_edit_product_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (product.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing details grids (Cost price vs Selling Retail price vs Wholesale Selling price) rearranged in Column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cost Column Card
                if (showCostAndMargins) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
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
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "₹${product.costPrice} / ${if (product.priceUnit == "kg") "kg" else "pc"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Wholesale Column Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
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
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            if (taxRate > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%.1f", wholesaleTaxInclPrice)} (${Translation.getString("tax_inclusive", language)})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "₹${product.wholesalePrice} / ${if (product.priceUnit == "kg") "kg" else "pc"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Retail Column Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
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
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (taxRate > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%.1f", retailTaxInclPrice)} (${Translation.getString("tax_inclusive", language)})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "₹${product.sellingPrice} / ${if (product.priceUnit == "kg") "kg" else "pc"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${Translation.getString("wholesale_profit", language)}: +₹$wholesaleProfitPr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusWidget(viewModel: PriceListViewModel) {
    if (viewModel.loginMethod != "cloud" || viewModel.cloudShopId.isBlank()) return
    val context = LocalContext.current
    val language = viewModel.currentLanguage

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
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
    var showThemeChooserDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("settings_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = Translation.getString("settings_title", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Settings")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Settings Content Scrollable body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Theme selection launcher card
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("theme_launcher_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        onClick = { showThemeChooserDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = Translation.getString("settings_appearance", language),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = Translation.getString("settings_appearance_desc", language),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Themes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Font Size Increase/Decrease setting card
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("settings_font_size_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Translation.getString("settings_font_size", language),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = Translation.getString("settings_font_size_desc", language),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Horizontal alignment with segmented button or buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                        .weight(1.5f)
                                        .height(38.dp)
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
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("A+", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Recycle Bin Navigation row inside Settings
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("settings_trash_button"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            onDismiss() // Close Settings dialog
                            viewModel.currentScreen = Screen.TRASH
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = Translation.getString("trash_bin", language),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "हटाए गए फ़ोल्डर्स और उत्पादों को पुनर्स्थापित करें" else "Restore or permanently delete items",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Bin",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Theme Picker Sub-dialog (Opens on launcher click)
                    if (showThemeChooserDialog) {
                        Dialog(onDismissRequest = { showThemeChooserDialog = false }) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Translation.getString("settings_appearance", language),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = { showThemeChooserDialog = false }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // App Theme Mode Title & Options
                                    Text(
                                        text = Translation.getString("settings_theme", language),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
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

                                    // Brand Color Palette Selector
                                    Text(
                                        text = Translation.getString("settings_palette", language),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val palettes = listOf(
                                        Triple("SAGE", "palette_sage", Color(0xFF5D624E)),
                                        Triple("BLUE", "palette_blue", Color(0xFF1E40AF)),
                                        Triple("CRIMSON", "palette_crimson", Color(0xFF9D174D)),
                                        Triple("TEAL", "palette_teal", Color(0xFF0F766E)),
                                        Triple("GOLDEN", "palette_golden", Color(0xFFB45309))
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { showThemeChooserDialog = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (language == AppLanguage.HINDI) "ठीक है" else "Done", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Custom Shop Profile / Branding Name
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = Translation.getString("settings_shop_branding", language),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = viewModel.customShopName,
                                onValueChange = { viewModel.updateCustomShopName(it) },
                                placeholder = { Text("E.g. Sharda General Store", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("shop_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Text(
                                text = Translation.getString("settings_shop_tagline", language),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = viewModel.customShopTagline,
                                onValueChange = { viewModel.updateCustomShopTagline(it) },
                                placeholder = { Text("E.g. Shop 42, Market Gali, Pune", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("shop_tagline_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // 3. Display Toggles
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Cost toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Translation.getString("settings_show_cost", language),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = viewModel.showCostAndMargins,
                                    onCheckedChange = { viewModel.updateShowCostAndMargins(it) },
                                    modifier = Modifier.testTag("settings_cost_switch")
                                )
                            }
                            
                            // Supplier toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Translation.getString("settings_show_supplier", language),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = viewModel.showSupplierInfo,
                                    onCheckedChange = { viewModel.updateShowSupplierInfo(it) },
                                    modifier = Modifier.testTag("settings_supplier_switch")
                                )
                            }
                        }
                    }

                    // 4. Default Tax Rate (GST/VAT Rate)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = Translation.getString("settings_tax", language),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
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

                    // 5. Text-Based Backup & Recovery (Offline Safety)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = Translation.getString("settings_backup", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = Translation.getString("settings_backup_desc", language),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            // Export Button
                            Button(
                                onClick = {
                                    scope.launch {
                                        val backupPayload = viewModel.getCloudJsonPayload()
                                        // Copy to clipboard
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("PriceList Backup", backupPayload)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, Translation.getString("sync_code_copied", language), Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("export_backup_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(Translation.getString("settings_export", language), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Import Input and Button
                            OutlinedTextField(
                                value = importText,
                                onValueChange = { importText = it },
                                placeholder = { Text(Translation.getString("settings_import_hint", language), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().height(80.dp).testTag("import_backup_input"),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 3
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
                                    Text(Translation.getString("settings_import", language), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 6. Security Passcode PIN Changing Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = Translation.getString("settings_change_passcode", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newPasscodeVal,
                                onValueChange = {
                                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                        newPasscodeVal = it
                                    }
                                },
                                placeholder = { Text(Translation.getString("settings_new_passcode_hint", language), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("change_passcode_input"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Button(
                                onClick = {
                                    if (newPasscodeVal.length == 4) {
                                        viewModel.resetPasscode(newPasscodeVal)
                                        newPasscodeVal = ""
                                        Toast.makeText(context, Translation.getString("settings_passcode_success", language), Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, Translation.getString("settings_passcode_invalid", language), Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = newPasscodeVal.length == 4,
                                modifier = Modifier.fillMaxWidth().testTag("change_passcode_save_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(Translation.getString("settings_passcode_btn", language), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 7. WhatsApp-style Multi-Device QR Code Linking Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = Translation.getString("multi_device_sync", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = Translation.getString("multi_device_desc", language),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            if (viewModel.cloudShopId.isBlank()) {
                                Button(
                                    onClick = {
                                        viewModel.registerNewCloudShop { success, msg ->
                                            if (success) {
                                                viewModel.syncToCloud()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("settings_enable_linking_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    if (viewModel.isCloudSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text(Translation.getString("enable_multi_device_btn", language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Generate the QR Image
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        QrCodeImage(
                                            text = viewModel.cloudShopId,
                                            modifier = Modifier
                                                .size(170.dp)
                                                .padding(12.dp)
                                        )
                                    }

                                    Text(
                                        text = "${Translation.getString("shop_sync_code", language).split(" (")[0]}: ${viewModel.cloudShopId}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

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
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("settings_sync_force_btn"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save footer button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("close_settings_footer_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Translation.getString("settings_save", language), fontWeight = FontWeight.Bold)
                }
            }
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

    Column(modifier = Modifier.fillMaxSize()) {
        SyncStatusWidget(viewModel)

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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
    val isScanning = viewModel.isScanningInvoice
    val extractedProducts = viewModel.extractedProducts
    
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var isFolderDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(folders) {
        if (selectedFolderId == null) {
            selectedFolderId = viewModel.selectedFolder?.id ?: folders.firstOrNull()?.id
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = Translation.getString("smart_scanner_title", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Scanner")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Help description
                    Text(
                        text = Translation.getString("scan_bill_desc", language),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target Folder Selection
                    Text(
                        text = Translation.getString("select_import_folder", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { isFolderDropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val currentTargetFolder = folders.find { it.id == selectedFolderId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentTargetFolder?.name ?: (if (language == AppLanguage.HINDI) "कोई फ़ोल्डर नहीं चुना" else "No Category Selected"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTargetFolder != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = isFolderDropdownExpanded,
                            onDismissRequest = { isFolderDropdownExpanded = false }
                        ) {
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        selectedFolderId = folder.id
                                        isFolderDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Preset Invoice Templates
                    Text(
                        text = if (language == AppLanguage.HINDI) "त्वरित परीक्षण के लिए सैंपल बिल चुनें:" else "Select Preset Sample Bill for Demo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        val templates = listOf(
                            Triple(
                                if (language == AppLanguage.HINDI) "किराना थोक विक्रेता" else "Kirana Wholesale",
                                "Kirana Wholesalers",
                                """
                                Gupta Wholesale Kirana Store
                                Supplier: Gupta Wholesalers
                                ==========================
                                1. Fortune Besan 10kg - Rs. 950
                                2. Tata Salt 1kg - Rs. 22
                                3. Maggi Noodles Box - Rs. 140
                                4. Taj Mahal Tea 500g - Rs. 280
                                """.trimIndent()
                            ),
                            Triple(
                                if (language == AppLanguage.HINDI) "इलेक्ट्रॉनिक्स बिल" else "Electronics Invoice",
                                "Sunrise Electronics",
                                """
                                Invoice - Sunrise Electronics Co
                                Supplier: Sunrise Electronics Co
                                ====================================
                                * LED Bulb 12W Pack - Rs. 1200
                                * Extension Board - Rs. 240
                                * Copper Wire Coil 100m - Rs. 850
                                """.trimIndent()
                            ),
                            Triple(
                                if (language == AppLanguage.HINDI) "स्टेशनरी हब" else "Stationery Hub",
                                "Global Stationery",
                                """
                                Global Stationery Hub Bill #9210
                                Supplier: Global Stationery Hub
                                ====================================
                                - Classmate Notebooks Pack of 6 - Rs. 180
                                - Parker Jotter Special Pen - Rs. 220
                                - Sticky Notes Pad Combo - Rs. 90
                                """.trimIndent()
                            )
                        )

                        templates.forEach { (label, name, text) ->
                            FilterChip(
                                selected = viewModel.scannedInvoiceText == text,
                                onClick = {
                                    viewModel.scannedInvoiceText = text
                                    viewModel.scannedInvoiceSupplier = name
                                },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // Input Text Area
                    OutlinedTextField(
                        value = viewModel.scannedInvoiceText,
                        onValueChange = { viewModel.scannedInvoiceText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("ai_invoice_input_field"),
                        placeholder = {
                            Text(
                                text = Translation.getString("paste_invoice_hint", language),
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Execute Button
                    if (isScanning) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Text(
                                text = Translation.getString("scanning_loader", language),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.scanInvoiceWithGemini(viewModel.scannedInvoiceText)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_scan_ai_btn"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = viewModel.scannedInvoiceText.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Translation.getString("start_scan", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Parsed Products preview panel
                    if (extractedProducts.isNotEmpty()) {
                        Text(
                            text = Translation.getString("preview_scanned_items", language),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        extractedProducts.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = item.name,
                                            onValueChange = {
                                                viewModel.updateExtractedProduct(index, item.copy(name = it))
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = { Text(Translation.getString("extracted_name", language), fontSize = 10.sp) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.removeExtractedProduct(index) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Item",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = if (item.costPrice == 0.0) "" else item.costPrice.toString(),
                                            onValueChange = {
                                                val cost = it.toDoubleOrNull() ?: 0.0
                                                val retailCalculated = Math.round(cost * 1.15 * 10.0) / 10.0
                                                val wholesaleCalculated = Math.round(cost * 1.08 * 10.0) / 10.0
                                                viewModel.updateExtractedProduct(
                                                    index,
                                                    item.copy(
                                                        costPrice = cost,
                                                        sellingPrice = retailCalculated,
                                                        wholesalePrice = wholesaleCalculated
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = { Text(Translation.getString("extracted_cost", language), fontSize = 9.sp) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                        )

                                        OutlinedTextField(
                                            value = if (item.sellingPrice == 0.0) "" else item.sellingPrice.toString(),
                                            onValueChange = {
                                                val sell = it.toDoubleOrNull() ?: 0.0
                                                viewModel.updateExtractedProduct(index, item.copy(sellingPrice = sell))
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = { Text(Translation.getString("extracted_selling", language), fontSize = 9.sp) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        )

                                        OutlinedTextField(
                                            value = if (item.wholesalePrice == 0.0) "" else item.wholesalePrice.toString(),
                                            onValueChange = {
                                                val whole = it.toDoubleOrNull() ?: 0.0
                                                viewModel.updateExtractedProduct(index, item.copy(wholesalePrice = whole))
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = { Text(Translation.getString("extracted_wholesale", language), fontSize = 9.sp) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Action buttons (Save/Dismiss)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(text = Translation.getString("cancel", language), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val destId = selectedFolderId
                            if (destId != null) {
                                viewModel.importExtractedProducts(destId)
                                Toast.makeText(context, if (language == AppLanguage.HINDI) "सफलतापूर्वक इम्पोर्ट किया गया!" else "Successfully imported ${extractedProducts.size} items!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, if (language == AppLanguage.HINDI) "कृपया पहले एक फ़ोल्डर चुनें" else "Please select a target folder first", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("import_parsed_items_btn"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = extractedProducts.isNotEmpty() && selectedFolderId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Translation.getString("import_items_btn", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

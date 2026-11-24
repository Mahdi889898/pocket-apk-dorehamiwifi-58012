package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppScreen
import com.example.ui.screens.ActiveGameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RoomChatModalSheet
import com.example.ui.screens.RoomLobbyScreen
import com.example.ui.screens.WifiHubScreen
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DorehamiTheme
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VelvetNavy
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            DorehamiTheme {
                DorehamiAppRoot()
            }
        }
    }
}

@Composable
private fun DorehamiAppRoot(
    viewModel: AppViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateRecordPermission(granted)
    }

    LaunchedEffect(Unit) {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateRecordPermission(hasAudio)
    }

    val requestMicPermission = {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Back handling for secondary screens
    BackHandler(enabled = uiState.currentScreen != AppScreen.Home) {
        when (uiState.currentScreen) {
            AppScreen.ActiveGame -> viewModel.returnToLobbyOrHome()
            AppScreen.RoomLobby -> viewModel.leaveCurrentRoom(navigateHome = true)
            AppScreen.WifiHub, AppScreen.Profile -> viewModel.navigateTo(AppScreen.Home)
            AppScreen.Home -> Unit
        }
    }

    val showTopLevelNav = uiState.currentScreen is AppScreen.Home ||
        uiState.currentScreen is AppScreen.WifiHub ||
        uiState.currentScreen is AppScreen.Profile

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBg)
    ) {
        val isExpanded = maxWidth >= 600.dp

        if (showTopLevelNav) {
            if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    NavigationRail(
                        containerColor = VelvetNavy,
                        contentColor = BrightGold
                    ) {
                        NavigationRailItem(
                            selected = uiState.currentScreen is AppScreen.Home,
                            onClick = { viewModel.navigateTo(AppScreen.Home) },
                            icon = { Icon(Icons.Rounded.SportsEsports, contentDescription = "بازی‌ها") },
                            label = { Text("بازی‌ها") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MidnightBg,
                                selectedTextColor = BrightGold,
                                indicatorColor = RoyalGold,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        NavigationRailItem(
                            selected = uiState.currentScreen is AppScreen.WifiHub,
                            onClick = { viewModel.navigateTo(AppScreen.WifiHub) },
                            icon = { Icon(Icons.Rounded.Wifi, contentDescription = "اتاق وای‌فای") },
                            label = { Text("اتاق وای‌فای") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MidnightBg,
                                selectedTextColor = BrightGold,
                                indicatorColor = RoyalGold,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        NavigationRailItem(
                            selected = uiState.currentScreen is AppScreen.Profile,
                            onClick = { viewModel.navigateTo(AppScreen.Profile) },
                            icon = { Icon(Icons.Rounded.Person, contentDescription = "پروفایل") },
                            label = { Text("پروفایل") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MidnightBg,
                                selectedTextColor = BrightGold,
                                indicatorColor = RoyalGold,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TopLevelScreenContent(uiState = uiState, viewModel = viewModel)
                    }
                }
            } else {
                Scaffold(
                    containerColor = MidnightBg,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            containerColor = VelvetNavy,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentScreen is AppScreen.Home,
                                onClick = { viewModel.navigateTo(AppScreen.Home) },
                                icon = { Icon(Icons.Rounded.SportsEsports, contentDescription = "بازی‌ها") },
                                label = { Text("بازی‌ها") },
                                modifier = Modifier.testTag("nav_home_tab"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MidnightBg,
                                    selectedTextColor = BrightGold,
                                    indicatorColor = RoyalGold,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.currentScreen is AppScreen.WifiHub,
                                onClick = { viewModel.navigateTo(AppScreen.WifiHub) },
                                icon = { Icon(Icons.Rounded.Wifi, contentDescription = "اتاق وای‌فای") },
                                label = { Text("اتاق وای‌فای") },
                                modifier = Modifier.testTag("nav_wifi_tab"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MidnightBg,
                                    selectedTextColor = BrightGold,
                                    indicatorColor = RoyalGold,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.currentScreen is AppScreen.Profile,
                                onClick = { viewModel.navigateTo(AppScreen.Profile) },
                                icon = { Icon(Icons.Rounded.Person, contentDescription = "پروفایل") },
                                label = { Text("پروفایل") },
                                modifier = Modifier.testTag("nav_profile_tab"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MidnightBg,
                                    selectedTextColor = BrightGold,
                                    indicatorColor = RoyalGold,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(innerPadding)
                    ) {
                        TopLevelScreenContent(uiState = uiState, viewModel = viewModel)
                    }
                }
            }
        } else {
            when (uiState.currentScreen) {
                AppScreen.RoomLobby -> {
                    RoomLobbyScreen(
                        uiState = uiState,
                        onSelectGame = viewModel::selectGameInLobby,
                        onAddBot = viewModel::addBotToLobby,
                        onRemoveBot = viewModel::removeBotFromLobby,
                        onLaunchGame = viewModel::launchGameFromLobby,
                        onLeaveRoom = { viewModel.leaveCurrentRoom(navigateHome = true) },
                        onRequestMicPermission = requestMicPermission,
                        onPushToTalkChange = viewModel::setPushToTalk,
                        onToggleOpenMic = viewModel::toggleOpenMic,
                        onToggleSpeakerMute = viewModel::toggleSpeakerMute,
                        onSendMessage = { viewModel.sendChatMessage(it, isReaction = false) },
                        onSendReaction = { viewModel.sendChatMessage(it, isReaction = true) },
                        modifier = Modifier.statusBarsPadding()
                    )
                }

                AppScreen.ActiveGame -> {
                    ActiveGameScreen(
                        uiState = uiState,
                        onGameAction = { json, explicitIdx -> viewModel.performGameAction(json, explicitIdx) },
                        onRestart = viewModel::restartActiveGame,
                        onBack = viewModel::returnToLobbyOrHome,
                        onOpenChatSheet = { viewModel.setChatSheetOpen(true) },
                        onRequestMicPermission = requestMicPermission,
                        onPushToTalkChange = viewModel::setPushToTalk,
                        onToggleOpenMic = viewModel::toggleOpenMic,
                        onToggleSpeakerMute = viewModel::toggleSpeakerMute
                    )
                }

                else -> Unit
            }
        }

        // In-Game Modal Voice & Text Chat Bottom Sheet
        if (uiState.isChatSheetOpen) {
            RoomChatModalSheet(
                messages = uiState.chatMessages,
                localPlayerId = uiState.profile.id,
                voiceState = uiState.voiceState,
                onDismiss = { viewModel.setChatSheetOpen(false) },
                onRequestMicPermission = requestMicPermission,
                onPushToTalkChange = viewModel::setPushToTalk,
                onToggleOpenMic = viewModel::toggleOpenMic,
                onToggleSpeakerMute = viewModel::toggleSpeakerMute,
                onSendMessage = { viewModel.sendChatMessage(it, isReaction = false) },
                onSendReaction = { viewModel.sendChatMessage(it, isReaction = true) }
            )
        }
    }
}

@Composable
private fun TopLevelScreenContent(
    uiState: com.example.viewmodel.AppUiState,
    viewModel: AppViewModel
) {
    when (uiState.currentScreen) {
        AppScreen.Home -> HomeScreen(
            uiState = uiState,
            onHostRoomWithGame = { game -> viewModel.hostWifiRoom("اتاق ${uiState.profile.name}", game) },
            onOpenWifiHub = { viewModel.navigateTo(AppScreen.WifiHub) },
            onStartLocalGame = viewModel::startQuickLocalGame
        )
        AppScreen.WifiHub -> WifiHubScreen(
            uiState = uiState,
            onHostRoom = viewModel::hostWifiRoom,
            onJoinRoom = viewModel::joinWifiRoom,
            onDismissError = viewModel::clearConnectionMessage
        )
        AppScreen.Profile -> ProfileScreen(
            uiState = uiState,
            onSaveProfile = viewModel::updateProfile
        )
        else -> Unit
    }
}

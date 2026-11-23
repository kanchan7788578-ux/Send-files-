package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shared.model.getStrings
import com.example.shared.ui.components.AppBrandHeader
import com.example.shared.ui.components.VideoPlayerScreen
import com.example.shared.ui.theme.BeamTvTheme
import com.example.shared.ui.theme.VoidNavy
import com.example.ui.MobileViewModel
import com.example.ui.screens.MobileHomeScreen
import com.example.ui.screens.QrScannerScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_QR_SCANNER = "qr_scanner"
private const val ROUTE_VIDEO_PLAYER = "video_player"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            BeamTvTheme {
                BeamTvMobileApp()
            }
        }
    }
}

@Composable
fun BeamTvMobileApp(viewModel: MobileViewModel = viewModel()) {
    val navController = rememberNavController()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val strings = remember(language) { getStrings(language) }

    val queuedItems by viewModel.selectedQueue.collectAsStateWithLifecycle()
    val discoveredTvs by viewModel.discoveredTvs.collectAsStateWithLifecycle()
    val pairedTv by viewModel.pairedTv.collectAsStateWithLifecycle()
    val sendState by viewModel.sendState.collectAsStateWithLifecycle()
    val historyRecords by viewModel.historyRecords.collectAsStateWithLifecycle()
    val wifiDirectState by viewModel.wifiDirectState.collectAsStateWithLifecycle()

    var pendingPlayUri by remember { mutableStateOf("") }
    var pendingPlayTitle by remember { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        modifier = Modifier
            .fillMaxSize()
            .background(VoidNavy)
    ) {
        composable(ROUTE_HOME) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppBrandHeader(
                    title = strings.appNameMobile,
                    subtitle = strings.offlineBadge,
                    badgeIcon = Icons.Filled.Bolt,
                    language = language,
                    onToggleLanguage = viewModel::toggleLanguage
                )

                MobileHomeScreen(
                    strings = strings,
                    queuedItems = queuedItems,
                    discoveredTvs = discoveredTvs,
                    pairedTv = pairedTv,
                    sendState = sendState,
                    historyRecords = historyRecords,
                    wifiDirectState = wifiDirectState,
                    hasWifiDirectPermission = viewModel.wifiDirectManager.hasWifiDirectPermission(),
                    requiredWifiDirectPermission = viewModel.wifiDirectManager.requiredPermission(),
                    onAddUris = viewModel::addMediaUris,
                    onRemoveQueueItem = viewModel::removeQueuedItem,
                    onClearQueue = viewModel::clearQueue,
                    onOpenQrScanner = { navController.navigate(ROUTE_QR_SCANNER) },
                    onSelectTv = viewModel::selectDiscoveredTv,
                    onConnectManualIp = viewModel::connectManualIp,
                    onDisconnectTv = viewModel::disconnectTv,
                    onSendNow = viewModel::startSendingQueue,
                    onCancelSend = viewModel::cancelSending,
                    onDismissSendState = viewModel::resetSendState,
                    onRefreshNetwork = viewModel::refreshDiscovery,
                    onScanWifiDirectPeers = viewModel::scanWifiDirectPeers,
                    onConnectWifiDirectPeer = viewModel::connectWifiDirectPeer,
                    onPlayVideo = { record ->
                        pendingPlayUri = record.filePathOrUri
                        pendingPlayTitle = record.fileName
                        val encodedUri = Uri.encode(record.filePathOrUri)
                        val encodedTitle = Uri.encode(record.fileName)
                        navController.navigate("$ROUTE_VIDEO_PLAYER?uri=$encodedUri&title=$encodedTitle")
                    },
                    onDeleteHistory = viewModel::deleteHistoryRecord,
                    onClearHistory = viewModel::clearHistory,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        composable(ROUTE_QR_SCANNER) {
            QrScannerScreen(
                strings = strings,
                onQrDetected = { raw ->
                    val paired = viewModel.pairFromQrRaw(raw)
                    if (paired) {
                        navController.popBackStack()
                    }
                    paired
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "$ROUTE_VIDEO_PLAYER?uri={uri}&title={title}",
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "Video" }
            )
        ) { backStackEntry ->
            val uriArg = backStackEntry.arguments?.getString("uri").orEmpty().ifBlank { pendingPlayUri }
            val titleArg = backStackEntry.arguments?.getString("title").orEmpty().ifBlank { pendingPlayTitle }
            VideoPlayerScreen(
                videoUriString = uriArg,
                videoTitle = titleArg,
                onClose = { navController.popBackStack() }
            )
        }
    }
}

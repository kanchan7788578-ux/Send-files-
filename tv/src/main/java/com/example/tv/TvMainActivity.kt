package com.example.tv

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
import androidx.compose.material.icons.filled.Tv
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

private const val ROUTE_TV_HUB = "tv_hub"
private const val ROUTE_TV_PLAYER = "tv_player"

class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            BeamTvTheme {
                BeamTvReceiverApp()
            }
        }
    }
}

@Composable
fun BeamTvReceiverApp(viewModel: TvViewModel = viewModel()) {
    val navController = rememberNavController()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val strings = remember(language) { getStrings(language) }
    val tvInfo by viewModel.tvServerInfo.collectAsStateWithLifecycle()
    val receiveState by viewModel.receiveState.collectAsStateWithLifecycle()
    val receivedRecords by viewModel.receivedRecords.collectAsStateWithLifecycle()

    var pendingPlayUri by remember { mutableStateOf("") }
    var pendingPlayTitle by remember { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = ROUTE_TV_HUB,
        modifier = Modifier
            .fillMaxSize()
            .background(VoidNavy)
    ) {
        composable(ROUTE_TV_HUB) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppBrandHeader(
                    title = strings.appNameTv,
                    subtitle = strings.tvOfflineBadge,
                    badgeIcon = Icons.Filled.Tv,
                    language = language,
                    onToggleLanguage = viewModel::toggleLanguage
                )

                TvReceiverScreen(
                    strings = strings,
                    tvInfo = tvInfo,
                    receiveState = receiveState,
                    receivedFiles = receivedRecords,
                    hasWifiDirectPermission = viewModel.wifiDirectManager.hasWifiDirectPermission(),
                    requiredWifiDirectPermission = viewModel.wifiDirectManager.requiredPermission(),
                    onToggleWifiDirect = viewModel::toggleWifiDirect,
                    onRefreshNetwork = viewModel::refreshNetworkIp,
                    onDismissTransfer = viewModel::resetReceiveState,
                    onPlayVideo = { record ->
                        pendingPlayUri = record.filePathOrUri
                        pendingPlayTitle = record.fileName
                        val encodedUri = Uri.encode(record.filePathOrUri)
                        val encodedTitle = Uri.encode(record.fileName)
                        navController.navigate("$ROUTE_TV_PLAYER?uri=$encodedUri&title=$encodedTitle")
                    },
                    onDeleteRecord = viewModel::deleteRecord,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        composable(
            route = "$ROUTE_TV_PLAYER?uri={uri}&title={title}",
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

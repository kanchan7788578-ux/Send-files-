package com.example.ui.screens

import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shared.R as SharedR
import com.example.shared.data.TransferRecordEntity
import com.example.shared.model.AppStrings
import com.example.shared.model.DiscoveredTvDevice
import com.example.shared.model.QueuedMediaItem
import com.example.shared.model.TransferState
import com.example.shared.transfer.NetworkUtils
import com.example.shared.transfer.TransferServer
import com.example.shared.transfer.WifiDirectState
import com.example.shared.ui.components.TransferProgressCard
import com.example.shared.ui.components.VideoThumbnailBox
import com.example.shared.ui.theme.CardBorder
import com.example.shared.ui.theme.CoralAlert
import com.example.shared.ui.theme.CyanContainer
import com.example.shared.ui.theme.DeepSurface
import com.example.shared.ui.theme.ElectricCyan
import com.example.shared.ui.theme.ElevatedCard
import com.example.shared.ui.theme.MintContainer
import com.example.shared.ui.theme.MintPulse
import com.example.shared.ui.theme.TextPrimary
import com.example.shared.ui.theme.TextSecondary
import com.example.shared.ui.theme.VoidNavy

@Composable
fun MobileHomeScreen(
    strings: AppStrings,
    queuedItems: List<QueuedMediaItem>,
    discoveredTvs: List<DiscoveredTvDevice>,
    pairedTv: DiscoveredTvDevice?,
    sendState: TransferState,
    historyRecords: List<TransferRecordEntity>,
    wifiDirectState: WifiDirectState,
    hasWifiDirectPermission: Boolean,
    requiredWifiDirectPermission: String,
    onAddUris: (List<Uri>) -> Unit,
    onRemoveQueueItem: (String) -> Unit,
    onClearQueue: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onSelectTv: (DiscoveredTvDevice) -> Unit,
    onConnectManualIp: (String, Int) -> Unit,
    onDisconnectTv: () -> Unit,
    onSendNow: () -> Unit,
    onCancelSend: () -> Unit,
    onDismissSendState: () -> Unit,
    onRefreshNetwork: () -> Unit,
    onScanWifiDirectPeers: () -> Unit,
    onConnectWifiDirectPeer: (WifiP2pDevice) -> Unit,
    onPlayVideo: (TransferRecordEntity) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showManualDialog by remember { mutableStateOf(false) }

    val pickVideosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        onAddUris(uris)
    }

    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        onAddUris(uris)
    }

    val wifiDirectPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onScanWifiDirectPeers()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VoidNavy,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = DeepSurface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = strings.tabSend
                        )
                    },
                    label = { Text(strings.tabSend) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VoidNavy,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_send")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = strings.tabHistory
                        )
                    },
                    label = { Text(strings.tabHistory) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VoidNavy,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MobileHeroCard(
                            strings = strings,
                            onScanQr = onOpenQrScanner
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    pickVideosLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = VoidNavy
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(54.dp)
                                    .testTag("pick_videos_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoLibrary,
                                    contentDescription = strings.pickVideos,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.pickVideos,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = { pickFilesLauncher.launch(arrayOf("*/*")) },
                                border = BorderStroke(1.dp, CardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(54.dp)
                                    .testTag("pick_files_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderOpen,
                                    contentDescription = strings.pickAnyFile,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.pickAnyFile,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (sendState !is TransferState.Idle) {
                        item {
                            TransferProgressCard(
                                state = sendState,
                                strings = strings,
                                onCancel = onCancelSend,
                                onDismiss = onDismissSendState
                            )
                        }
                    }

                    if (pairedTv != null) {
                        item {
                            PairedTvCard(
                                tv = pairedTv,
                                strings = strings,
                                canSend = queuedItems.isNotEmpty() && sendState !is TransferState.Transferring,
                                onSendNow = onSendNow,
                                onDisconnect = onDisconnectTv
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.selectedQueueTitle} (${queuedItems.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            if (queuedItems.isNotEmpty()) {
                                TextButton(onClick = onClearQueue) {
                                    Text(strings.clearAll, color = CoralAlert)
                                }
                            }
                        }
                    }

                    if (queuedItems.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.emptyQueueHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(queuedItems, key = { it.id }) { item ->
                            QueuedMediaRow(
                                item = item,
                                onRemove = { onRemoveQueueItem(item.id) }
                            )
                        }
                    }

                    item {
                        NearbyTvsSection(
                            strings = strings,
                            discoveredTvs = discoveredTvs,
                            wifiDirectPeers = wifiDirectState.peers,
                            pairedTv = pairedTv,
                            onSelectTv = onSelectTv,
                            onRefresh = onRefreshNetwork,
                            onOpenManual = { showManualDialog = true },
                            onScanWifiDirect = {
                                if (hasWifiDirectPermission) {
                                    onScanWifiDirectPeers()
                                } else {
                                    wifiDirectPermissionLauncher.launch(requiredWifiDirectPermission)
                                }
                            },
                            onConnectWifiDirectPeer = onConnectWifiDirectPeer
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CyanContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Tip",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = strings.hotspotTip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.historyTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            if (historyRecords.isNotEmpty()) {
                                TextButton(onClick = onClearHistory) {
                                    Text(strings.clearAll, color = CoralAlert)
                                }
                            }
                        }
                    }

                    if (historyRecords.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = strings.emptyHistoryTitle,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = strings.emptyHistoryTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = strings.emptyHistoryHint,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(historyRecords, key = { it.id }) { record ->
                            HistoryRecordCard(
                                record = record,
                                strings = strings,
                                onPlay = { onPlayVideo(record) },
                                onDelete = { onDeleteHistory(record.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualConnectDialog(
            strings = strings,
            onDismiss = { showManualDialog = false },
            onConnect = { ip, port ->
                onConnectManualIp(ip, port)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun MobileHeroCard(
    strings: AppStrings,
    onScanQr: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = SharedR.drawable.img_tv_banner_1795427812280),
                contentDescription = strings.heroTitleMobile,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                VoidNavy.copy(alpha = 0.55f),
                                DeepSurface.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.heroTitleMobile,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = strings.heroSubtitleMobile,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MintPulse,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onScanQr)
                        .testTag("scan_qr_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VoidNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = strings.scanQrTitle,
                                tint = MintPulse,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.scanQrTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = VoidNavy
                            )
                            Text(
                                text = strings.scanQrSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = VoidNavy.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairedTvCard(
    tv: DiscoveredTvDevice,
    strings: AppStrings,
    canSend: Boolean,
    onSendNow: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MintContainer.copy(alpha = 0.55f)),
        border = BorderStroke(1.5.dp, MintPulse),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MintPulse.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tv,
                        contentDescription = tv.name,
                        tint = MintPulse
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.connectedTo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MintPulse
                    )
                    Text(
                        text = tv.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${tv.ip}:${tv.port}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.testTag("disconnect_tv_btn")
                ) {
                    Text(strings.disconnect, color = TextSecondary)
                }
            }

            Button(
                onClick = onSendNow,
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintPulse,
                    contentColor = VoidNavy
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("send_to_tv_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = strings.sendNow
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.sendNow,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun QueuedMediaRow(
    item: QueuedMediaItem,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VideoThumbnailBox(
                uriString = item.uriString,
                isVideo = item.isVideo,
                contentDescription = item.name,
                modifier = Modifier.size(64.dp, 48.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = NetworkUtils.formatBytes(item.sizeBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun NearbyTvsSection(
    strings: AppStrings,
    discoveredTvs: List<DiscoveredTvDevice>,
    wifiDirectPeers: List<WifiP2pDevice>,
    pairedTv: DiscoveredTvDevice?,
    onSelectTv: (DiscoveredTvDevice) -> Unit,
    onRefresh: () -> Unit,
    onOpenManual: () -> Unit,
    onScanWifiDirect: () -> Unit,
    onConnectWifiDirectPeer: (WifiP2pDevice) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.nearbyTvsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = strings.refreshNetwork,
                            tint = ElectricCyan
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenManual,
                        border = BorderStroke(1.dp, ElectricCyan),
                        modifier = Modifier.testTag("manual_ip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = strings.manualConnect,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.manualConnect, color = ElectricCyan)
                    }
                }
            }

            if (discoveredTvs.isEmpty()) {
                Text(
                    text = strings.scanningTvs,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                discoveredTvs.forEach { tv ->
                    val isSelected = pairedTv?.ip == tv.ip && pairedTv.port == tv.port
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) CyanContainer else ElevatedCard,
                        border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectTv(tv) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tv,
                                contentDescription = tv.name,
                                tint = ElectricCyan
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tv.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${tv.ip}:${tv.port} • ${tv.source.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = strings.connectBtn,
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.wifiDirectPeersTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                TextButton(
                    onClick = onScanWifiDirect,
                    modifier = Modifier.testTag("scan_p2p_peers_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiTethering,
                        contentDescription = strings.scanPeers,
                        tint = MintPulse,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.scanPeers, color = MintPulse)
                }
            }

            wifiDirectPeers.forEach { peer ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onConnectWifiDirectPeer(peer) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = peer.deviceName ?: "Wi-Fi Direct Device",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.connectBtn,
                            style = MaterialTheme.typography.labelLarge,
                            color = MintPulse
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: TransferRecordEntity,
    strings: AppStrings,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val isSent = record.direction == "SENT"
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VideoThumbnailBox(
                uriString = record.filePathOrUri,
                isVideo = record.isVideo,
                contentDescription = record.fileName,
                modifier = Modifier.size(64.dp, 48.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${if (isSent) strings.sentLabel else strings.receivedLabel} • ${NetworkUtils.formatBytes(record.sizeBytes)} • ${NetworkUtils.formatSpeedMbps(record.averageSpeedBytesPerSec)} ${strings.speedUnit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSent) ElectricCyan else MintPulse,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (record.isVideo) {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = strings.playOnTv,
                        tint = ElectricCyan
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ManualConnectDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit
) {
    var ipText by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf(TransferServer.DEFAULT_PORT.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text(strings.manualDialogTitle, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    label = { Text(strings.ipAddressHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_ip_input")
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.portHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_port_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: TransferServer.DEFAULT_PORT
                    if (ipText.isNotBlank()) {
                        onConnect(ipText, port)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = VoidNavy
                ),
                modifier = Modifier.testTag("manual_connect_confirm_btn")
            ) {
                Text(strings.connectBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = TextSecondary)
            }
        }
    )
}

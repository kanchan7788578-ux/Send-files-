package com.example.tv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.R as SharedR
import com.example.shared.data.TransferRecordEntity
import com.example.shared.model.AppStrings
import com.example.shared.model.TransferState
import com.example.shared.model.TvServerInfo
import com.example.shared.transfer.NetworkUtils
import com.example.shared.transfer.QrCodeGenerator
import com.example.shared.ui.components.TransferProgressCard
import com.example.shared.ui.components.VideoThumbnailBox
import com.example.shared.ui.theme.CardBorder
import com.example.shared.ui.theme.CyanContainer
import com.example.shared.ui.theme.DeepSurface
import com.example.shared.ui.theme.ElectricCyan
import com.example.shared.ui.theme.ElevatedCard
import com.example.shared.ui.theme.JetBrainsMonoFamily
import com.example.shared.ui.theme.MintContainer
import com.example.shared.ui.theme.MintPulse
import com.example.shared.ui.theme.TextPrimary
import com.example.shared.ui.theme.TextSecondary
import com.example.shared.ui.theme.VoidNavy

@Composable
fun TvReceiverScreen(
    strings: AppStrings,
    tvInfo: TvServerInfo,
    receiveState: TransferState,
    receivedFiles: List<TransferRecordEntity>,
    hasWifiDirectPermission: Boolean,
    requiredWifiDirectPermission: String,
    onToggleWifiDirect: () -> Unit,
    onRefreshNetwork: () -> Unit,
    onDismissTransfer: () -> Unit,
    onPlayVideo: (TransferRecordEntity) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onToggleWifiDirect()
    }

    val handleWifiDirectClick = {
        if (hasWifiDirectPermission) {
            onToggleWifiDirect()
        } else {
            permissionLauncher.launch(requiredWifiDirectPermission)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(VoidNavy)
            .navigationBarsPadding()
    ) {
        val isWideTvLayout = maxWidth >= 680.dp

        if (isWideTvLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvQrPairingCard(
                        strings = strings,
                        tvInfo = tvInfo,
                        onToggleWifiDirect = handleWifiDirectClick,
                        onRefreshNetwork = onRefreshNetwork
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TransferProgressCard(
                        state = receiveState,
                        strings = strings,
                        onCancel = {},
                        onDismiss = onDismissTransfer
                    )

                    Text(
                        text = strings.tvReceivedTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )

                    if (receivedFiles.isEmpty()) {
                        TvEmptyLibraryCard(strings = strings)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(receivedFiles, key = { it.id }) { item ->
                                TvReceivedVideoCard(
                                    record = item,
                                    strings = strings,
                                    onPlay = { onPlayVideo(item) },
                                    onDelete = { onDeleteRecord(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TvQrPairingCard(
                        strings = strings,
                        tvInfo = tvInfo,
                        onToggleWifiDirect = handleWifiDirectClick,
                        onRefreshNetwork = onRefreshNetwork
                    )
                }

                if (receiveState !is TransferState.Idle) {
                    item {
                        TransferProgressCard(
                            state = receiveState,
                            strings = strings,
                            onCancel = {},
                            onDismiss = onDismissTransfer
                        )
                    }
                }

                item {
                    Text(
                        text = strings.tvReceivedTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }

                if (receivedFiles.isEmpty()) {
                    item { TvEmptyLibraryCard(strings = strings) }
                } else {
                    items(receivedFiles, key = { it.id }) { item ->
                        TvReceivedVideoCard(
                            record = item,
                            strings = strings,
                            onPlay = { onPlayVideo(item) },
                            onDelete = { onDeleteRecord(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvQrPairingCard(
    strings: AppStrings,
    tvInfo: TvServerInfo,
    onToggleWifiDirect: () -> Unit,
    onRefreshNetwork: () -> Unit
) {
    val qrString = remember(
        tvInfo.localIp,
        tvInfo.port,
        tvInfo.deviceName,
        tvInfo.pin,
        tvInfo.wifiDirectSsid,
        tvInfo.wifiDirectPass
    ) {
        tvInfo.toPairingPayload().toQrJson()
    }
    val qrBitmap = remember(qrString) {
        QrCodeGenerator.generateQrBitmap(qrString, 560)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                Image(
                    painter = painterResource(id = SharedR.drawable.img_tv_banner_1795427812280),
                    contentDescription = strings.tvHubTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(VoidNavy.copy(alpha = 0.25f), DeepSurface)
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.tvHubTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.tvHubSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                    }
                    IconButton(
                        onClick = onRefreshNetwork,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tv_refresh_ip_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = strings.refreshNetwork,
                            tint = TextPrimary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = strings.tvScanPrompt,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(220.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "TV Pairing QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryChip(
                        label = strings.tvIpLabel,
                        value = tvInfo.localIp,
                        modifier = Modifier.weight(1.4f)
                    )
                    TelemetryChip(
                        label = strings.tvPortLabel,
                        value = tvInfo.port.toString(),
                        modifier = Modifier.weight(0.8f)
                    )
                    TelemetryChip(
                        label = strings.tvPinLabel,
                        value = tvInfo.pin,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                var wifiFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onToggleWifiDirect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tvInfo.wifiDirectActive) MintPulse else CyanContainer,
                        contentColor = if (tvInfo.wifiDirectActive) VoidNavy else ElectricCyan
                    ),
                    border = if (wifiFocused) BorderStroke(2.dp, ElectricCyan) else null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .onFocusChanged { wifiFocused = it.isFocused }
                        .focusable()
                        .testTag("tv_wifi_direct_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiTethering,
                        contentDescription = strings.tvStartWifiDirect,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (tvInfo.wifiDirectActive) strings.tvStopWifiDirect else strings.tvStartWifiDirect,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (tvInfo.wifiDirectActive && !tvInfo.wifiDirectSsid.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MintContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = strings.tvWifiDirectReady,
                                style = MaterialTheme.typography.labelLarge,
                                color = MintPulse
                            )
                            Text(
                                text = "SSID: ${tvInfo.wifiDirectSsid}  •  Pass: ${tvInfo.wifiDirectPass ?: "—"}",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                } else {
                    val p2pError = tvInfo.wifiDirectError
                    if (!p2pError.isNullOrBlank()) {
                        Text(
                            text = p2pError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = strings.tvCompanionHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ElevatedCard,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ElectricCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvEmptyLibraryCard(strings: AppStrings) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Tv,
                contentDescription = strings.tvNoVideosYet,
                tint = ElectricCyan,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = strings.tvNoVideosYet,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.tvNoVideosHint,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TvReceivedVideoCard(
    record: TransferRecordEntity,
    strings: AppStrings,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) ElevatedCard else DeepSurface
        ),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) ElectricCyan else CardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlay)
            .testTag("tv_received_item_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VideoThumbnailBox(
                uriString = record.filePathOrUri,
                isVideo = record.isVideo,
                contentDescription = record.fileName,
                modifier = Modifier.size(84.dp, 58.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${NetworkUtils.formatBytes(record.sizeBytes)} • ${NetworkUtils.formatSpeedMbps(record.averageSpeedBytesPerSec)} ${strings.speedUnit} • ${record.peerName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (record.isVideo) {
                OutlinedButton(
                    onClick = onPlay,
                    border = BorderStroke(1.dp, ElectricCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = strings.playOnTv,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.playOnTv)
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

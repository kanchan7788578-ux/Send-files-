package com.example.shared.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.shared.model.AppLanguage
import com.example.shared.model.AppStrings
import com.example.shared.model.TransferState
import com.example.shared.transfer.NetworkUtils
import com.example.shared.ui.theme.CardBorder
import com.example.shared.ui.theme.CoralAlert
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
fun AppBrandHeader(
    title: String,
    subtitle: String,
    badgeIcon: ImageVector = Icons.Filled.Bolt,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var langFocused by remember { mutableStateOf(false) }

    Surface(
        color = DeepSurface.copy(alpha = 0.94f),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(ElectricCyan, MintPulse))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = title,
                        tint = VoidNavy,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MintPulse,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (langFocused) CyanContainer else ElevatedCard,
                border = BorderStroke(
                    width = if (langFocused) 2.dp else 1.dp,
                    color = if (langFocused) ElectricCyan else CardBorder
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .onFocusChanged { langFocused = it.isFocused }
                    .focusable()
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToggleLanguage)
                    .testTag("language_toggle_btn")
            ) {
                Text(
                    text = if (language == AppLanguage.BENGALI) "বাংলা | EN" else "EN | বাংলা",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TransferProgressCard(
    state: TransferState,
    strings: AppStrings,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is TransferState.Idle) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ElevatedCard),
        border = BorderStroke(
            1.dp,
            when (state) {
                is TransferState.Completed -> MintPulse
                is TransferState.Error -> CoralAlert
                else -> ElectricCyan
            }
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                is TransferState.Connecting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${state.targetName} (${state.targetIp})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Connecting...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ElectricCyan
                            )
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("cancel_transfer_btn")
                        ) {
                            Text(strings.cancel)
                        }
                    }
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ElectricCyan,
                        trackColor = VoidNavy
                    )
                }

                is TransferState.Transferring -> {
                    val animatedProgress by animateFloatAsState(
                        targetValue = state.progress,
                        animationSpec = tween(durationMillis = 180),
                        label = "transfer_progress"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isSending) strings.sendingProgress else strings.receivingProgress,
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = state.currentFileName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${state.currentFileIndex}/${state.totalFiles} • ${state.peerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MintContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = NetworkUtils.formatSpeedMbps(state.speedBytesPerSec),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MintPulse
                                )
                                Text(
                                    text = strings.speedUnit,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp,
                                    color = MintPulse
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MintPulse,
                        trackColor = VoidNavy
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${NetworkUtils.formatBytes(state.bytesTransferred)} / ${NetworkUtils.formatBytes(state.totalBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${(animatedProgress * 100).toInt()}% • ETA ${NetworkUtils.formatDurationSeconds(state.etaSeconds)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            if (state.isSending) {
                                OutlinedButton(
                                    onClick = onCancel,
                                    border = BorderStroke(1.dp, CoralAlert),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
                                    modifier = Modifier.testTag("cancel_transfer_btn")
                                ) {
                                    Text(strings.cancel)
                                }
                            }
                        }
                    }
                }

                is TransferState.Completed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = strings.transferComplete,
                            tint = MintPulse,
                            modifier = Modifier.size(34.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.transferComplete,
                                style = MaterialTheme.typography.titleMedium,
                                color = MintPulse
                            )
                            Text(
                                text = "${state.fileCount} ${strings.filesCountSuffix} • ${NetworkUtils.formatBytes(state.totalBytes)} • Avg ${NetworkUtils.formatSpeedMbps(state.averageSpeedBytesPerSec)} ${strings.speedUnit}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MintPulse,
                                contentColor = VoidNavy
                            ),
                            modifier = Modifier.testTag("dismiss_transfer_btn")
                        ) {
                            Text(strings.done)
                        }
                    }
                }

                is TransferState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Error",
                            tint = CoralAlert,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("dismiss_error_btn")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = strings.done)
                        }
                    }
                }

                TransferState.Idle -> Unit
            }
        }
    }
}

@Composable
fun VideoThumbnailBox(
    uriString: String,
    isVideo: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CyanContainer),
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(uriString))
                    .videoFrameMillis(1000L)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(VoidNavy.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = contentDescription,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = contentDescription,
                tint = ElectricCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

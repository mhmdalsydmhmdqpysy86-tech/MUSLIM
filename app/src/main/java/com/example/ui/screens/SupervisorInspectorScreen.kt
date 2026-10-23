package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.AiDetectionItem
import com.example.data.model.RoomTask
import com.example.ui.ScanSessionUiState
import com.example.ui.ViewportInputMode
import com.example.ui.theme.AlertRed100
import com.example.ui.theme.AlertRed50
import com.example.ui.theme.AlertRed500
import com.example.ui.theme.AlertRed600
import com.example.ui.theme.Sky400
import com.example.ui.theme.Sky600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.SuccessEmerald100
import com.example.ui.theme.SuccessEmerald50
import com.example.ui.theme.SuccessEmerald500
import com.example.ui.theme.SuccessEmerald600
import com.example.ui.theme.Teal400
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.ui.theme.WarningAmber100
import com.example.ui.theme.WarningAmber50
import com.example.ui.theme.WarningAmber500
import com.example.ui.theme.WarningAmber600
import com.example.ui.util.ShareHelper

@Composable
fun SupervisorInspectorScreen(
    scanState: ScanSessionUiState,
    rooms: List<RoomTask>,
    onSelectRoom: (RoomTask) -> Unit,
    onChangeViewportMode: (ViewportInputMode) -> Unit,
    onRunAiScan: (Bitmap?) -> Unit,
    onPhotoPicked: (Bitmap) -> Unit,
    onSendHousekeeperAlert: () -> Unit,
    onApproveRoomReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            onChangeViewportMode(ViewportInputMode.LIVE_CAMERA)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeUriToBitmap(context, uri)
            if (decoded != null) {
                onPhotoPicked(decoded)
            }
        }
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    val sampleDrawableRes = when (scanState.selectedRoomNumber) {
        "210" -> R.drawable.img_family_suite_210
        else -> R.drawable.img_royal_suite_304
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("supervisor_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Room Selector Strip
        if (rooms.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rooms.forEach { room ->
                        val isSelected = room.roomNumber == scanState.selectedRoomNumber
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectRoom(room) },
                            label = {
                                Text(
                                    text = "#${room.roomNumber} • ${room.roomTitle.substringBefore("#").trim()}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Teal600,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Slate700
                            ),
                            modifier = Modifier.testTag("room_chip_${room.roomNumber}")
                        )
                    }
                }
            }
        }

        // Room Header & Floor Badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فحص ${scanState.selectedRoomTitle}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate800
                        )
                    }
                    Text(
                        text = stringResource(R.string.inspector_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Slate200,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.floor_prefix, scanState.selectedFloor),
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate700,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val selectedTask = rooms.find { it.roomNumber == scanState.selectedRoomNumber }
                            val body = if (selectedTask != null) {
                                ShareHelper.buildSingleRoomTaskText(selectedTask)
                            } else {
                                "غرفة: ${scanState.selectedRoomTitle} (الدور ${scanState.selectedFloor})\nحالة المستشعرات: ${scanState.selectedIotStatus}"
                            }
                            ShareHelper.shareTextReport(
                                context = context,
                                subject = "حالة ${scanState.selectedRoomTitle}",
                                body = body
                            )
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Teal50,
                            contentColor = Teal600
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_share_current_room")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share_room_status),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Input Mode Switcher (Sample Room / Live CameraX / Photo Picker)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Slate200.copy(alpha = 0.65f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModeSelectorButton(
                    title = stringResource(R.string.mode_sample_room),
                    icon = Icons.Default.Visibility,
                    selected = scanState.viewportMode == ViewportInputMode.SAMPLE_ROOM,
                    onClick = { onChangeViewportMode(ViewportInputMode.SAMPLE_ROOM) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_sample_button")
                )
                ModeSelectorButton(
                    title = stringResource(R.string.mode_live_camera),
                    icon = Icons.Default.CameraAlt,
                    selected = scanState.viewportMode == ViewportInputMode.LIVE_CAMERA,
                    onClick = {
                        if (hasCameraPermission) {
                            onChangeViewportMode(ViewportInputMode.LIVE_CAMERA)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_camera_button")
                )
                ModeSelectorButton(
                    title = stringResource(R.string.mode_photo_picker),
                    icon = Icons.Default.PhotoLibrary,
                    selected = scanState.viewportMode == ViewportInputMode.PHOTO_GALLERY,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_gallery_button")
                )
            }
        }

        // AI Camera Viewport with Scanning Laser & Bounding Boxes
        item {
            AiScannerViewport(
                scanState = scanState,
                sampleDrawableRes = sampleDrawableRes,
                hasCameraPermission = hasCameraPermission,
                imageCapture = imageCapture
            )
        }

        // Primary AI Scan Action Button
        item {
            val isRescan = scanState.activeReport != null && !scanState.isScanning
            Button(
                onClick = {
                    when (scanState.viewportMode) {
                        ViewportInputMode.LIVE_CAMERA -> {
                            if (hasCameraPermission) {
                                captureFrameAndScan(
                                    context = context,
                                    imageCapture = imageCapture,
                                    onCaptured = { bmp -> onRunAiScan(bmp) },
                                    onFallback = {
                                        val sampleBmp = BitmapFactory.decodeResource(context.resources, sampleDrawableRes)
                                        onRunAiScan(sampleBmp)
                                    }
                                )
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        ViewportInputMode.PHOTO_GALLERY -> {
                            val bmp = scanState.customBitmap
                                ?: BitmapFactory.decodeResource(context.resources, sampleDrawableRes)
                            onRunAiScan(bmp)
                        }
                        ViewportInputMode.SAMPLE_ROOM -> {
                            val bmp = BitmapFactory.decodeResource(context.resources, sampleDrawableRes)
                            onRunAiScan(bmp)
                        }
                    }
                },
                enabled = !scanState.isScanning,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Slate700
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (scanState.isScanning) {
                            Brush.horizontalGradient(listOf(Slate700, Slate800))
                        } else {
                            Brush.horizontalGradient(listOf(Teal600, Sky600))
                        }
                    )
                    .testTag("btn_scan_ai")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (scanState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Teal400,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.scanning_in_progress),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = if (isRescan) Icons.Default.Refresh else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFCD34D),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRescan) {
                                stringResource(R.string.btn_rescan_ai)
                            } else if (scanState.viewportMode == ViewportInputMode.LIVE_CAMERA) {
                                stringResource(R.string.capture_and_analyze)
                            } else {
                                stringResource(R.string.btn_start_ai_scan)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // AI Inspection Audit Results Card
        item {
            AiAuditResultsCard(
                scanState = scanState,
                onSendHousekeeperAlert = onSendHousekeeperAlert,
                onApproveRoomReady = onApproveRoomReady,
                onShareScanReport = {
                    val report = scanState.activeReport ?: return@AiAuditResultsCard
                    val bitmapForShare = scanState.customBitmap
                        ?: runCatching {
                            BitmapFactory.decodeResource(context.resources, sampleDrawableRes)
                        }.getOrNull()
                    ShareHelper.shareScanReportWithOptionalBitmap(
                        context = context,
                        roomNumber = scanState.selectedRoomNumber,
                        roomTitle = scanState.selectedRoomTitle,
                        floorNumber = scanState.selectedFloor,
                        report = report,
                        bitmap = bitmapForShare
                    )
                }
            )
        }
    }
}

@Composable
private fun ModeSelectorButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) Slate900 else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) Teal400 else Slate700,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else Slate700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AiScannerViewport(
    scanState: ScanSessionUiState,
    sampleDrawableRes: Int,
    hasCameraPermission: Boolean,
    imageCapture: ImageCapture
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLaser")
    val scanRayProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanRayY"
    )

    // Use LTR inside coordinate viewport so fractional positioning stays deterministic
    CompositionLocalProviderForViewport {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(242.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Slate800, RoundedCornerShape(20.dp))
                .background(Slate900)
                .testTag("camera_viewport")
        ) {
            val viewportWidth = maxWidth
            val viewportHeight = maxHeight

            // Base Visual Layer
            when {
                scanState.viewportMode == ViewportInputMode.LIVE_CAMERA && hasCameraPermission -> {
                    CameraXPreviewContent(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                scanState.viewportMode == ViewportInputMode.PHOTO_GALLERY && scanState.customBitmap != null -> {
                    Image(
                        bitmap = scanState.customBitmap.asImageBitmap(),
                        contentDescription = scanState.selectedRoomTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = sampleDrawableRes),
                        contentDescription = scanState.selectedRoomTitle,
                        contentScale = ContentScale.Crop,
                        alpha = 0.88f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Rule-of-Thirds Camera Crosshair Grid Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = Color.White.copy(alpha = 0.14f)
                val thirdW = size.width / 3f
                val thirdH = size.height / 3f

                drawLine(gridColor, Offset(thirdW, 0f), Offset(thirdW, size.height), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, thirdH), Offset(size.width, thirdH), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f), strokeWidth = 1.dp.toPx())

                // Animated Laser Scanning Ray
                if (scanState.isScanning) {
                    val y = size.height * scanRayProgress
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Sky400.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            startY = (y - 24.dp.toPx()).coerceAtLeast(0f),
                            endY = (y + 24.dp.toPx()).coerceAtMost(size.height)
                        ),
                        topLeft = Offset(0f, (y - 24.dp.toPx()).coerceAtLeast(0f)),
                        size = androidx.compose.ui.geometry.Size(size.width, 48.dp.toPx())
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Sky400, Teal400, Sky400, Color.Transparent)
                        ),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.5.dp.toPx()
                    )
                }
            }

            // Progressive AI Bounding Boxes
            val detections = scanState.activeReport?.detections.orEmpty()
            detections.forEachIndexed { index, det ->
                val isVisible = index < scanState.visibleDetectionCount
                val leftFraction = (1f - det.rightPercent - det.widthPercent).coerceIn(0.03f, 0.68f)
                val topFraction = det.topPercent.coerceIn(0.10f, 0.65f)
                val boxW = viewportWidth * det.widthPercent.coerceIn(0.24f, 0.44f)
                val boxH = viewportHeight * det.heightPercent.coerceIn(0.20f, 0.34f)

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(260)) + scaleIn(spring(dampingRatio = 0.68f)),
                    modifier = Modifier
                        .offset(
                            x = viewportWidth * leftFraction,
                            y = viewportHeight * topFraction
                        )
                ) {
                    DetectionBoundingBoxOverlay(
                        detection = det,
                        width = boxW,
                        height = boxH
                    )
                }
            }

            // Bottom-Left Engine Badge
            Surface(
                color = Slate950.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Teal400,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (scanState.activeReport?.isLiveGeminiAnalysis == true) {
                            "Gemini 3.5 Vision Active"
                        } else {
                            stringResource(R.string.ai_engine_badge)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Top-Right Score Pill once scanned
            scanState.activeReport?.let { report ->
                if (!scanState.isScanning) {
                    val pillColor = when {
                        report.qualityScore >= 92 -> SuccessEmerald600
                        report.qualityScore >= 80 -> WarningAmber600
                        else -> AlertRed600
                    }
                    Surface(
                        color = pillColor,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "الجودة ${report.qualityScore}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompositionLocalProviderForViewport(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        content = content
    )
}

@Composable
private fun DetectionBoundingBoxOverlay(
    detection: AiDetectionItem,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    val strokeColor = when (detection.severity) {
        "ERROR" -> AlertRed500
        "WARNING" -> WarningAmber500
        else -> SuccessEmerald500
    }
    val labelBg = when (detection.severity) {
        "ERROR" -> AlertRed600
        "WARNING" -> WarningAmber600
        else -> SuccessEmerald600
    }
    val icon = when (detection.severity) {
        "ERROR" -> Icons.Default.Error
        "WARNING" -> Icons.Default.Warning
        else -> Icons.Default.CheckCircle
    }

    Column(horizontalAlignment = Alignment.End) {
        // Floating Label Pill
        Surface(
            color = labelBg,
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 4.dp
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = detection.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Dashed Bounding Rectangle
        Canvas(
            modifier = Modifier
                .width(width)
                .height(height)
        ) {
            drawRoundRect(
                color = strokeColor.copy(alpha = 0.22f),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            drawRoundRect(
                color = strokeColor,
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                )
            )
        }
    }
}

@Composable
private fun CameraXPreviewContent(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.w("CameraXPreview", "Camera bind failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun AiAuditResultsCard(
    scanState: ScanSessionUiState,
    onSendHousekeeperAlert: () -> Unit,
    onApproveRoomReady: () -> Unit,
    onShareScanReport: () -> Unit
) {
    val report = scanState.activeReport
    val actionableCount = report?.detections?.count {
        it.severity == "ERROR" || it.severity == "WARNING"
    } ?: 0

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(18.dp))
            .testTag("ai_analysis_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = null,
                        tint = Teal600,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.audit_result_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Slate800
                    )
                }

                val (badgeBg, badgeFg, badgeLabel) = when {
                    scanState.isScanning -> Triple(Teal50, Teal600, "جاري التحليل...")
                    report == null -> Triple(Slate100, Slate500, stringResource(R.string.status_waiting_scan))
                    actionableCount > 0 -> Triple(AlertRed100, AlertRed600, "$actionableCount ملاحظة تتطلب تعديل")
                    else -> Triple(SuccessEmerald100, SuccessEmerald600, "معتمدة 100%")
                }

                Surface(
                    color = badgeBg,
                    shape = CircleShape
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            if (report == null && !scanState.isScanning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.empty_scan_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (report != null) {
                report.detections.take(scanState.visibleDetectionCount).forEach { item ->
                    DetectionLogRow(item = item)
                }

                if (!scanState.isScanning) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (actionableCount > 0) {
                        Button(
                            onClick = onSendHousekeeperAlert,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Slate900,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_send_report")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Teal400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_send_housekeeper_alert),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Button(
                            onClick = onApproveRoomReady,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessEmerald600,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_approve_room")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_approve_room_ready),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onShareScanReport,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Teal50,
                            contentColor = Teal600
                        ),
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_share_scan_report")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_share_scan_report),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectionLogRow(item: AiDetectionItem) {
    val (bg, borderCol, titleCol, badgeBg, icon) = when (item.severity) {
        "ERROR" -> Quintuple(AlertRed50, AlertRed100, AlertRed600, AlertRed100, Icons.Default.Error)
        "WARNING" -> Quintuple(WarningAmber50, WarningAmber100, WarningAmber600, WarningAmber100, Icons.Default.Warning)
        else -> Quintuple(SuccessEmerald50, SuccessEmerald100, SuccessEmerald600, SuccessEmerald100, Icons.Default.CheckCircle)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleCol,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900
                )
                Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = titleCol
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            color = badgeBg,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = item.sourceBadge,
                style = MaterialTheme.typography.labelSmall,
                color = titleCol,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("SupervisorInspector", "Failed to decode gallery URI", e)
        null
    }
}

private fun captureFrameAndScan(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Bitmap) -> Unit,
    onFallback: () -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bmp = image.toBitmap()
                    image.close()
                    onCaptured(bmp)
                } catch (e: Exception) {
                    image.close()
                    onFallback()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.w("CameraCapture", "Capture fallback triggered: ${exception.message}")
                onFallback()
            }
        }
    )
}

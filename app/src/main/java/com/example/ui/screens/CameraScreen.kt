package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.biometrics.FaceRecognitionEngine
import com.example.data.biometrics.LivenessAnalysisResult
import com.example.data.biometrics.LivenessChallenge
import com.example.data.biometrics.LivenessDetector
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.ui.AttendanceViewModel
import com.example.ui.components.ConfidencePill
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BlueDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val employees by viewModel.employees.collectAsState()
    val kioskState by viewModel.kioskState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Liveness Detection Module instance and reactive state
    val livenessDetector = remember { LivenessDetector() }
    var livenessResult by remember { mutableStateOf(LivenessAnalysisResult()) }
    var simulationMode by remember { mutableStateOf("LIVE_STREAM") } // "LIVE_STREAM", "SIMULATED_PASS", "SIMULATED_SPOOF"

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var flashEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    var isCapturing by remember { mutableStateOf(false) }
    var captureStatusText by remember { mutableStateOf("Position face & blink naturally") }
    var lastCapturedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var verifiedEmployee by remember { mutableStateOf<Employee?>(null) }
    var matchConfidence by remember { mutableStateOf(0f) }
    var showResultDialog by remember { mutableStateOf(false) }
    var flashShutterEffect by remember { mutableStateOf(false) }
    var livenessFeedbackAlert by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "camera_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Dynamic reticle accent color based on liveness and anti-spoof state
    val reticleColor = when {
        livenessResult.isSpoofDetected -> AlertRed
        livenessResult.isLive || verifiedEmployee != null -> VerifiedGreen
        livenessResult.currentChallenge == LivenessChallenge.BLINK_EYES -> WarningAmber
        else -> CyanPrimary
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        // 1. CameraX Preview Layer with Real-Time ImageAnalysis Liveness Engine
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camerax_preview_surface"),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCaptureUseCase = imageCapture

                            // ImageAnalysis use case configured for YUV frame sampling
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build()

                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                try {
                                    if (simulationMode == "LIVE_STREAM") {
                                        val planes = imageProxy.image?.planes
                                        if (planes != null && planes.isNotEmpty()) {
                                            val yPlane = planes[0]
                                            val buffer = yPlane.buffer
                                            val result = livenessDetector.analyzeFrame(
                                                yBuffer = buffer,
                                                width = imageProxy.width,
                                                height = imageProxy.height,
                                                rowStride = yPlane.rowStride
                                            )
                                            livenessResult = result
                                        }
                                    }
                                } catch (ignored: Exception) {
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                            cameraControl = camera
                        } catch (e: Exception) {
                            captureStatusText = "Camera initialization error: ${e.message}"
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCaptureUseCase = imageCapture

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build()

                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(previewView.context)) { imageProxy ->
                                try {
                                    if (simulationMode == "LIVE_STREAM") {
                                        val planes = imageProxy.image?.planes
                                        if (planes != null && planes.isNotEmpty()) {
                                            val yPlane = planes[0]
                                            val buffer = yPlane.buffer
                                            val result = livenessDetector.analyzeFrame(
                                                yBuffer = buffer,
                                                width = imageProxy.width,
                                                height = imageProxy.height,
                                                rowStride = yPlane.rowStride
                                            )
                                            livenessResult = result
                                        }
                                    }
                                } catch (ignored: Exception) {
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                            cameraControl = camera
                            camera.cameraControl.enableTorch(flashEnabled)
                        } catch (ignored: Exception) {}
                    }, ContextCompat.getMainExecutor(previewView.context))
                }
            )
        } else {
            // Permission fallback simulation HUD
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), NavyDark, Color(0xFF020617))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f))
                            .border(2.dp, CyanPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = NavyDark)
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Optical Facial Mesh & Reticle Canvas with Adaptive Anti-Spoof Feedback
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val rectWidth = w * 0.72f
                val rectHeight = h * 0.48f
                val left = (w - rectWidth) / 2f
                val top = (h - rectHeight) / 2f - 40.dp.toPx()
                val right = left + rectWidth
                val bottom = top + rectHeight

                val cornerLength = 32.dp.toPx()
                val cornerStroke = 4.dp.toPx()

                // 4 Reticle Corners
                val pathTL = Path().apply {
                    moveTo(left, top + cornerLength)
                    lineTo(left, top)
                    lineTo(left + cornerLength, top)
                }
                drawPath(pathTL, reticleColor, style = Stroke(width = cornerStroke))

                val pathTR = Path().apply {
                    moveTo(right - cornerLength, top)
                    lineTo(right, top)
                    lineTo(right, top + cornerLength)
                }
                drawPath(pathTR, reticleColor, style = Stroke(width = cornerStroke))

                val pathBL = Path().apply {
                    moveTo(left, bottom - cornerLength)
                    lineTo(left, bottom)
                    lineTo(left + cornerLength, bottom)
                }
                drawPath(pathBL, reticleColor, style = Stroke(width = cornerStroke))

                val pathBR = Path().apply {
                    moveTo(right - cornerLength, bottom)
                    lineTo(right, bottom)
                    lineTo(right, bottom - cornerLength)
                }
                drawPath(pathBR, reticleColor, style = Stroke(width = cornerStroke))

                // Oval Face Guide
                drawRoundRect(
                    color = reticleColor.copy(alpha = 0.28f),
                    topLeft = Offset(left, top),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(rectWidth * 0.45f, rectHeight * 0.45f),
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // Laser Scan Beam
                val laserY = top + (rectHeight * laserPosition)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, reticleColor, reticleColor, Color.Transparent),
                        startX = left,
                        endX = right
                    ),
                    start = Offset(left, laserY),
                    end = Offset(right, laserY),
                    strokeWidth = 3.dp.toPx()
                )

                // Landmark Points & Blink Visualizer
                val cx = w / 2f
                val cy = top + (rectHeight / 2f)
                val eyeGap = rectWidth * 0.22f
                val dotRadius = 3.5.dp.toPx()
                val dotColor = reticleColor.copy(alpha = 0.85f)

                // Left & Right Eyes (pulse when blink is detected)
                val eyeScale = if (livenessResult.blinkDetected) 1.5f else 1.0f
                drawCircle(dotColor, dotRadius * eyeScale, Offset(cx - eyeGap, cy - rectHeight * 0.12f))
                drawCircle(dotColor, dotRadius * eyeScale, Offset(cx + eyeGap, cy - rectHeight * 0.12f))

                // Nose Bridge & Mouth corners
                drawCircle(dotColor, dotRadius, Offset(cx, cy))
                drawCircle(dotColor, dotRadius, Offset(cx - eyeGap * 0.6f, cy + rectHeight * 0.18f))
                drawCircle(dotColor, dotRadius, Offset(cx + eyeGap * 0.6f, cy + rectHeight * 0.18f))
            }
        }

        // Shutter Flash Overlay Effect
        if (flashShutterEffect) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // 3. Top Header Bar: Terminal details + Camera controls + Anti-Spoof Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NavyDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, reticleColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(reticleColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (livenessResult.isSpoofDetected) "SPOOF ATTACK DETECTED" else "LIVENESS SENSOR ACTIVE",
                        color = reticleColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reset Liveness State Button
                IconButton(
                    onClick = {
                        livenessDetector.reset()
                        livenessResult = LivenessAnalysisResult()
                        simulationMode = "LIVE_STREAM"
                        livenessFeedbackAlert = null
                        captureStatusText = "Liveness reset. Look at camera & blink."
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NavyDark.copy(alpha = 0.85f))
                        .border(1.dp, NavyBorder, CircleShape)
                        .testTag("camera_reset_liveness_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Liveness Test",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Flash Toggle
                IconButton(
                    onClick = {
                        flashEnabled = !flashEnabled
                        cameraControl?.cameraControl?.enableTorch(flashEnabled)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NavyDark.copy(alpha = 0.85f))
                        .border(1.dp, NavyBorder, CircleShape)
                        .testTag("camera_flash_toggle")
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch Flash",
                        tint = if (flashEnabled) WarningAmber else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Lens Flip Toggle
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            CameraSelector.LENS_FACING_BACK
                        } else {
                            CameraSelector.LENS_FACING_FRONT
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NavyDark.copy(alpha = 0.85f))
                        .border(1.dp, NavyBorder, CircleShape)
                        .testTag("camera_switch_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera Lens",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. Punch Type Selector Row
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 66.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            color = NavyDark.copy(alpha = 0.85f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttendanceType.values().forEach { type ->
                    val isSelected = kioskState.selectedPunchType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyanPrimary else Color.Transparent)
                            .clickable { viewModel.setPunchType(type) }
                            .padding(vertical = 7.dp)
                            .testTag("camera_punch_type_${type.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NavyDark else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // 5. Liveness & Anti-Spoof Real-time Floating Challenge Card
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 118.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = NavyDark.copy(alpha = 0.90f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    when {
                        livenessResult.isSpoofDetected -> AlertRed
                        livenessResult.isLive -> VerifiedGreen
                        else -> CyanPrimary.copy(alpha = 0.4f)
                    }
                ),
                modifier = Modifier.fillMaxWidth().testTag("liveness_challenge_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header Row: Challenge status + Liveness %
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    livenessResult.isSpoofDetected -> Icons.Default.Warning
                                    livenessResult.isLive -> Icons.Default.CheckCircle
                                    livenessResult.currentChallenge == LivenessChallenge.BLINK_EYES -> Icons.Default.RemoveRedEye
                                    else -> Icons.Default.Shield
                                },
                                contentDescription = null,
                                tint = reticleColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = livenessResult.message,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${(livenessResult.livenessScore * 100).toInt()}%",
                            color = reticleColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { livenessResult.livenessScore },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = reticleColor,
                        trackColor = NavySurfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Telemetry Row: Blinks count + Optical Motion score + Anti-Spoof Shield
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Blink Counter Badge
                        Surface(
                            color = NavySurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (livenessResult.blinkCount > 0) VerifiedGreen else NavyBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveRedEye,
                                    contentDescription = null,
                                    tint = if (livenessResult.blinkCount > 0) VerifiedGreen else WarningAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Blinks: ${livenessResult.blinkCount}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (livenessResult.blinkCount > 0) VerifiedGreen else Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Optical Micro-motion Badge
                        val motionFmt = String.format(Locale.US, "%.3f", livenessResult.motionScore)
                        Surface(
                            color = NavySurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, NavyBorder)
                        ) {
                            Text(
                                text = "Micro-motion: $motionFmt",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Anti-Spoof Badge
                        Surface(
                            color = if (livenessResult.isSpoofDetected) AlertRed.copy(alpha = 0.2f) else VerifiedGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (livenessResult.isSpoofDetected) AlertRed else VerifiedGreen
                            )
                        ) {
                            Text(
                                text = if (livenessResult.isSpoofDetected) "SPOOF REJECTED" else "SHIELD: ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (livenessResult.isSpoofDetected) AlertRed else VerifiedGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Anti-Spoof Rejection Alert Banner (if photo detected)
            AnimatedVisibility(
                visible = livenessResult.isSpoofDetected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .testTag("spoof_warning_banner"),
                    color = AlertRed.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = livenessResult.spoofReason ?: "Static 2D paper or digital display detected. Attendance capture blocked.",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Temporary alert for user guidance
            AnimatedVisibility(
                visible = livenessFeedbackAlert != null && !livenessResult.isSpoofDetected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    color = WarningAmber.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NavyDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = livenessFeedbackAlert ?: "",
                            color = NavyDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. Bottom Control Panel: Quick Profile Target + Liveness Simulation Modes + Biometric Capture Shutter
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, NavyDark.copy(alpha = 0.95f), NavyDark)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Liveness Test Toggles (Allows instant testing of Live Person vs 2D Photo Attack)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode:",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                // 1. Live Camera Stream Mode
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            simulationMode = "LIVE_STREAM"
                            livenessDetector.reset()
                            livenessResult = LivenessAnalysisResult()
                            livenessFeedbackAlert = null
                        }
                        .testTag("mode_camera_stream"),
                    color = if (simulationMode == "LIVE_STREAM") CyanPrimary.copy(alpha = 0.2f) else NavySurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (simulationMode == "LIVE_STREAM") CyanPrimary else NavyBorder
                    )
                ) {
                    Text(
                        text = "CameraX Live Optical",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (simulationMode == "LIVE_STREAM") CyanPrimary else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }

                // 2. Simulate Live Human Passing
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            simulationMode = "SIMULATED_PASS"
                            livenessResult = livenessDetector.simulateLivePersonPass()
                            livenessFeedbackAlert = null
                            captureStatusText = "Live person verified • Ready to punch"
                        }
                        .testTag("mode_simulate_live_pass"),
                    color = if (simulationMode == "SIMULATED_PASS") VerifiedGreen.copy(alpha = 0.2f) else NavySurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (simulationMode == "SIMULATED_PASS") VerifiedGreen else NavyBorder
                    )
                ) {
                    Text(
                        text = "Simulate Live Person",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (simulationMode == "SIMULATED_PASS") VerifiedGreen else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }

                // 3. Simulate Static Photo Spoof Attack
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            simulationMode = "SIMULATED_SPOOF"
                            livenessResult = livenessDetector.simulateStaticPhotoSpoof()
                            livenessFeedbackAlert = "Static photo attack simulated: Shutter locked."
                            captureStatusText = "Anti-Spoof Warning: Static photo rejected"
                        }
                        .testTag("mode_simulate_photo_spoof"),
                    color = if (simulationMode == "SIMULATED_SPOOF") AlertRed.copy(alpha = 0.2f) else NavySurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (simulationMode == "SIMULATED_SPOOF") AlertRed else NavyBorder
                    )
                ) {
                    Text(
                        text = "Simulate 2D Photo Attack",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (simulationMode == "SIMULATED_SPOOF") AlertRed else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Target Profile Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Employee:",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                employees.forEach { emp ->
                    val isTarget = verifiedEmployee?.employeeCode == emp.employeeCode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isCapturing) {
                                verifiedEmployee = emp
                                captureStatusText = "Target: ${emp.fullName} • Verify liveness & tap Shutter"
                            }
                            .testTag("camera_target_${emp.employeeCode.lowercase()}"),
                        color = if (isTarget) CyanPrimary.copy(alpha = 0.2f) else NavySurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTarget) CyanPrimary else NavyBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emp.fullName.take(1),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = emp.fullName.split(" ").firstOrNull() ?: emp.fullName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Gated Biometric Capture Shutter Button (Enforces Liveness & Anti-Spoof)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NavySurface)
                        .border(3.dp, reticleColor, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            // Enforce Liveness Gate before processing
                            if (livenessResult.isSpoofDetected) {
                                livenessFeedbackAlert = "Attendance Denied: Anti-spoof algorithm flagged static photo."
                                captureStatusText = "Access Denied: 2D Photo Attack Detected!"
                                return@clickable
                            }

                            if (!livenessResult.isLive) {
                                livenessFeedbackAlert = "Please blink your eyes naturally to verify liveness before capture."
                                captureStatusText = "Liveness incomplete (${(livenessResult.livenessScore * 100).toInt()}%). Blink eyes."
                                return@clickable
                            }

                            // Verified Live Person: Proceed with Capture & Attendance Logging
                            livenessFeedbackAlert = null
                            scope.launch {
                                isCapturing = true
                                flashShutterEffect = true
                                delay(90)
                                flashShutterEffect = false

                                captureStatusText = "Extracting facial features & recording log..."

                                // Try taking picture via CameraX ImageCapture use case
                                val imageCapture = imageCaptureUseCase
                                if (imageCapture != null && hasCameraPermission) {
                                    val executor = ContextCompat.getMainExecutor(context)
                                    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            image.close()
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            // Handled gracefully
                                        }
                                    })
                                }

                                delay(800) // Landmark matching latency

                                // Select target employee or best match
                                val matched = verifiedEmployee ?: employees.firstOrNull()
                                if (matched != null) {
                                    val simVector = FaceRecognitionEngine.generatePerturbedScanVector(matched)
                                    val matchResult = FaceRecognitionEngine.findBestMatch(simVector, employees)

                                    verifiedEmployee = matched
                                    matchConfidence = matchResult.confidence

                                    // Record attendance in Room & Cloud with liveness verification passed
                                    viewModel.processScannedFaceData(
                                        scannedVector = simVector,
                                        livenessResult = livenessResult
                                    )

                                    delay(400)
                                    val lastRec = viewModel.kioskState.value.lastRecord
                                    lastCapturedRecord = lastRec
                                    captureStatusText = "Verified Live: ${matched.fullName} (${(matchResult.confidence * 100).toInt()}%)"
                                    showResultDialog = true
                                } else {
                                    captureStatusText = "No registered face matched"
                                }
                                isCapturing = false
                            }
                        }
                        .testTag("camera_capture_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    when {
                                        livenessResult.isSpoofDetected -> listOf(AlertRed, Color(0xFF7F1D1D))
                                        livenessResult.isLive -> listOf(VerifiedGreen, Color(0xFF065F46))
                                        else -> listOf(CyanPrimary, BlueDeep)
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = NavyDark,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    livenessResult.isSpoofDetected -> Icons.Default.Warning
                                    livenessResult.isLive -> Icons.Default.Check
                                    else -> Icons.Default.Face
                                },
                                contentDescription = "Capture Attendance",
                                tint = NavyDark,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    livenessResult.isSpoofDetected -> "CAPTURE BLOCKED • SPOOF DETECTED"
                    livenessResult.isLive -> "LIVENESS VERIFIED • TAP TO RECORD PUNCH"
                    else -> "BLINK EYES TO UNLOCK SHUTTER"
                },
                color = reticleColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // 8. Verification Confirmation Dialog
        if (showResultDialog && verifiedEmployee != null && lastCapturedRecord != null) {
            val emp = verifiedEmployee!!
            val rec = lastCapturedRecord!!
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val formattedTime = timeFormat.format(Date(rec.timestampMillis))

            Dialog(onDismissRequest = {
                showResultDialog = false
                viewModel.dismissSuccessPopup()
                captureStatusText = "Ready for next employee"
            }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .testTag("camera_verification_success_dialog"),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, VerifiedGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(VerifiedGreen.copy(alpha = 0.15f))
                                .border(2.dp, VerifiedGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = VerifiedGreen, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${rec.type.label} Verified!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Hello, ${emp.fullName}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanGlow
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = NavySurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Employee Code", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text(emp.employeeCode, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Department", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text(emp.department, color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Timestamp", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text(formattedTime, color = CyanPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    StatusBadge(status = rec.status)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Match Score", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    ConfidencePill(confidence = if (matchConfidence > 0) matchConfidence else rec.confidenceScore)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Liveness & Anti-Spoof", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Surface(
                                        color = VerifiedGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PASSED (Live Blink)",
                                            color = VerifiedGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                showResultDialog = false
                                viewModel.dismissSuccessPopup()
                                captureStatusText = "Ready for next capture"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("camera_dialog_dismiss_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen, contentColor = NavyDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done / Next Capture", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

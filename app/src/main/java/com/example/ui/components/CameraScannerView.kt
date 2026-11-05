package com.example.ui.components

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.VerifiedGreen

@Composable
fun CameraScannerView(
    hasCameraPermission: Boolean,
    statusText: String = "Align face in frame",
    isRecognized: Boolean = false,
    confidenceText: String = "",
    onSwitchCameraClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var cameraBoundSuccessfully by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val reticlePulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticle_pulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(NavyDark)
            .border(
                width = 2.dp,
                color = if (isRecognized) VerifiedGreen else CyanPrimary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview_view"),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                            cameraBoundSuccessfully = true
                        } catch (e: Exception) {
                            cameraBoundSuccessfully = false
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
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (ignored: Exception) {}
                    }, ContextCompat.getMainExecutor(previewView.context))
                }
            )
        } else {
            // Stylized cybernetic fallback simulation view
            CyberneticFaceSimulationBackground()
        }

        // Biometric HUD Overlay Canvas
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val rectWidth = w * 0.72f * reticlePulse
                val rectHeight = h * 0.62f * reticlePulse
                val left = (w - rectWidth) / 2f
                val top = (h - rectHeight) / 2f
                val right = left + rectWidth
                val bottom = top + rectHeight

                val reticleColor = if (isRecognized) VerifiedGreen else CyanPrimary
                val cornerLength = 36.dp.toPx()
                val cornerStroke = 4.dp.toPx()

                // Draw 4 corner brackets
                // Top-Left
                val pathTL = Path().apply {
                    moveTo(left, top + cornerLength)
                    lineTo(left, top)
                    lineTo(left + cornerLength, top)
                }
                drawPath(pathTL, reticleColor, style = Stroke(width = cornerStroke))

                // Top-Right
                val pathTR = Path().apply {
                    moveTo(right - cornerLength, top)
                    lineTo(right, top)
                    lineTo(right, top + cornerLength)
                }
                drawPath(pathTR, reticleColor, style = Stroke(width = cornerStroke))

                // Bottom-Left
                val pathBL = Path().apply {
                    moveTo(left, bottom - cornerLength)
                    lineTo(left, bottom)
                    lineTo(left + cornerLength, bottom)
                }
                drawPath(pathBL, reticleColor, style = Stroke(width = cornerStroke))

                // Bottom-Right
                val pathBR = Path().apply {
                    moveTo(right - cornerLength, bottom)
                    lineTo(right, bottom)
                    lineTo(right, bottom - cornerLength)
                }
                drawPath(pathBR, reticleColor, style = Stroke(width = cornerStroke))

                // Draw central biometric oval guide
                drawRoundRect(
                    color = reticleColor.copy(alpha = 0.25f),
                    topLeft = Offset(left, top),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(rectWidth * 0.45f, rectHeight * 0.45f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Draw scanning laser line
                val laserY = top + (rectHeight * laserPosition)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            reticleColor,
                            reticleColor,
                            Color.Transparent
                        ),
                        startX = left,
                        endX = right
                    ),
                    start = Offset(left, laserY),
                    end = Offset(right, laserY),
                    strokeWidth = 3.dp.toPx()
                )

                // Simulated landmark reference dots
                val centerX = w / 2f
                val centerY = h / 2f
                val eyeDist = rectWidth * 0.22f
                val dotRadius = 3.5.dp.toPx()
                val dotColor = reticleColor.copy(alpha = 0.85f)

                // Eyes
                drawCircle(dotColor, dotRadius, Offset(centerX - eyeDist, centerY - rectHeight * 0.12f))
                drawCircle(dotColor, dotRadius, Offset(centerX + eyeDist, centerY - rectHeight * 0.12f))
                // Nose
                drawCircle(dotColor, dotRadius, Offset(centerX, centerY))
                // Mouth corners
                drawCircle(dotColor, dotRadius, Offset(centerX - eyeDist * 0.65f, centerY + rectHeight * 0.18f))
                drawCircle(dotColor, dotRadius, Offset(centerX + eyeDist * 0.65f, centerY + rectHeight * 0.18f))
            }
        }

        // Top Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NavyDark.copy(alpha = 0.8f),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isRecognized) VerifiedGreen else CyanPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecognized) "LIVENESS VERIFIED" else "AI FACIAL DETECTION",
                        color = if (isRecognized) VerifiedGreen else CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasCameraPermission) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                            onSwitchCameraClicked?.invoke()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NavyDark.copy(alpha = 0.8f))
                            .border(1.dp, CyanGlow.copy(alpha = 0.4f), CircleShape)
                            .testTag("switch_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Bottom Feedback Banner
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            color = NavyDark.copy(alpha = 0.88f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isRecognized) VerifiedGreen.copy(alpha = 0.7f) else CyanPrimary.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = statusText,
                    color = if (isRecognized) VerifiedGreen else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (confidenceText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = confidenceText,
                        color = CyanGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberneticFaceSimulationBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF090D16),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(CyanPrimary.copy(alpha = 0.1f))
                    .border(2.dp, CyanPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(46.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Biometric Vision Active",
                color = CyanPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "CameraX Optical Sensor & Neural Mesh Engine",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
    }
}

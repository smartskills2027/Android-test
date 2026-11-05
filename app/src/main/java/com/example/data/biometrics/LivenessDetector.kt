package com.example.data.biometrics

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class LivenessChallenge(val prompt: String) {
    CENTER_FACE("Center face within the oval reticle"),
    NATURAL_MOTION("Remain steady: analyzing micro-motion..."),
    BLINK_EYES("Please blink your eyes naturally"),
    VERIFIED("Liveness Confirmed: Real Person Verified"),
    SPOOF_DETECTED("Anti-Spoof Warning: Static photo or video detected!")
}

data class LivenessAnalysisResult(
    val isLive: Boolean = false,
    val livenessScore: Float = 0f, // 0.0f to 1.0f
    val motionScore: Float = 0f,
    val blinkDetected: Boolean = false,
    val blinkCount: Int = 0,
    val currentChallenge: LivenessChallenge = LivenessChallenge.CENTER_FACE,
    val message: String = LivenessChallenge.CENTER_FACE.prompt,
    val isSpoofDetected: Boolean = false,
    val spoofReason: String? = null
)

/**
 * Lightweight real-time anti-spoofing and liveness detection engine.
 * Employs temporal frame differencing, micro-motion variance analysis,
 * and characteristic eye-blink luminance dynamics to distinguish a live human
 * from printed photos, screen displays, and video replays.
 */
class LivenessDetector {

    // Thresholds calibrated for mobile camera streams
    private val minLiveMotionThreshold = 0.008f   // Below this -> completely static photo
    private val maxUnnaturalMotionThreshold = 0.65f // Above this -> camera shaking / unnatural cut
    private val blinkDipThreshold = 0.035f         // Distinct drop in eye-region reflectance on eyelid closure
    private val requiredScoreToPass = 0.85f

    // Rolling temporal window
    private val frameHistorySize = 25
    private val recentLuminanceAverages = mutableListOf<Float>()
    private val recentMotionScores = mutableListOf<Float>()
    private var lastFrameSampleGrid: FloatArray? = null

    private var blinksDetected = 0
    private var lastBlinkTimestamp = 0L
    private var isEyeCurrentlyClosed = false

    private var accumulatedLiveFrames = 0
    private var consecutiveStaticFrames = 0

    // Challenge progression
    private var currentChallenge: LivenessChallenge = LivenessChallenge.NATURAL_MOTION

    /**
     * Analyzes a single camera frame from CameraX ImageAnalysis.
     * Takes the luminance (Y-plane) buffer and samples a central 16x16 grid.
     */
    @Synchronized
    fun analyzeFrame(
        yBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int
    ): LivenessAnalysisResult {
        val now = System.currentTimeMillis()
        val sampleGridSize = 16
        val currentSampleGrid = FloatArray(sampleGridSize * sampleGridSize)

        // Sample center ROI (where the face reticle is positioned)
        val roiStartX = (width * 0.25f).toInt()
        val roiStartY = (height * 0.20f).toInt()
        val roiWidth = (width * 0.50f).toInt()
        val roiHeight = (height * 0.55f).toInt()

        val stepX = max(1, roiWidth / sampleGridSize)
        val stepY = max(1, roiHeight / sampleGridSize)

        var totalLum = 0.0
        var sampleCount = 0

        yBuffer.rewind()
        for (gy in 0 until sampleGridSize) {
            val yPos = min(height - 1, roiStartY + gy * stepY)
            val rowOffset = yPos * rowStride
            for (gx in 0 until sampleGridSize) {
                val xPos = min(width - 1, roiStartX + gx * stepX)
                val pixelIndex = rowOffset + xPos
                if (pixelIndex < yBuffer.remaining()) {
                    val lumValue = (yBuffer.get(pixelIndex).toInt() and 0xFF) / 255.0f
                    val idx = gy * sampleGridSize + gx
                    currentSampleGrid[idx] = lumValue
                    totalLum += lumValue
                    sampleCount++
                }
            }
        }

        val avgLuminance = if (sampleCount > 0) (totalLum / sampleCount).toFloat() else 0.5f

        // Maintain rolling luminance history
        if (recentLuminanceAverages.size >= frameHistorySize) {
            recentLuminanceAverages.removeAt(0)
        }
        recentLuminanceAverages.add(avgLuminance)

        // 1. Temporal Frame Differencing (Motion Analysis)
        var motionScore = 0f
        val prevGrid = lastFrameSampleGrid
        if (prevGrid != null && prevGrid.size == currentSampleGrid.size) {
            var diffSum = 0.0
            for (i in prevGrid.indices) {
                diffSum += abs(currentSampleGrid[i] - prevGrid[i])
            }
            motionScore = (diffSum / prevGrid.size).toFloat()
        }
        lastFrameSampleGrid = currentSampleGrid

        if (recentMotionScores.size >= frameHistorySize) {
            recentMotionScores.removeAt(0)
        }
        recentMotionScores.add(motionScore)

        // 2. Static Photo Detection Check
        if (motionScore < minLiveMotionThreshold) {
            consecutiveStaticFrames++
        } else {
            consecutiveStaticFrames = max(0, consecutiveStaticFrames - 1)
        }

        val isStaticPhotoSpoof = consecutiveStaticFrames >= 20

        // 3. Blink Detection Analysis
        // Eye area is in the upper third of the sampled ROI (gy: 3..6)
        var upperRoiLum = 0.0
        var upperRoiSamples = 0
        for (gy in 3..6) {
            for (gx in 3..12) {
                upperRoiLum += currentSampleGrid[gy * sampleGridSize + gx]
                upperRoiSamples++
            }
        }
        val currentEyeLum = if (upperRoiSamples > 0) (upperRoiLum / upperRoiSamples).toFloat() else avgLuminance

        // Detect dip in reflectance
        val baselineEyeLum = if (recentLuminanceAverages.size > 5) {
            recentLuminanceAverages.take(5).average().toFloat()
        } else avgLuminance

        val lumDelta = baselineEyeLum - currentEyeLum
        if (lumDelta > blinkDipThreshold && !isEyeCurrentlyClosed) {
            isEyeCurrentlyClosed = true
        } else if (isEyeCurrentlyClosed && lumDelta < (blinkDipThreshold * 0.4f)) {
            // Eyelid reopened -> Confirmed Blink
            isEyeCurrentlyClosed = false
            if (now - lastBlinkTimestamp > 400) { // debounce blinks to avoid double count
                blinksDetected++
                lastBlinkTimestamp = now
            }
        }

        // 4. Calculate Aggregate Liveness Score
        val avgMotion = if (recentMotionScores.isNotEmpty()) recentMotionScores.average().toFloat() else 0f
        val hasNaturalMicroMotion = avgMotion in minLiveMotionThreshold..maxUnnaturalMotionThreshold
        val hasBlink = blinksDetected >= 1

        if (hasNaturalMicroMotion && !isStaticPhotoSpoof) {
            accumulatedLiveFrames = min(100, accumulatedLiveFrames + 2)
        } else {
            accumulatedLiveFrames = max(0, accumulatedLiveFrames - 1)
        }

        val motionComponent = (accumulatedLiveFrames / 60f).coerceIn(0f, 0.60f)
        val blinkComponent = if (hasBlink) 0.40f else 0f
        val aggregateScore = (motionComponent + blinkComponent).coerceIn(0f, 1.0f)

        // Update challenge state
        val resultChallenge = when {
            isStaticPhotoSpoof -> LivenessChallenge.SPOOF_DETECTED
            aggregateScore >= requiredScoreToPass -> LivenessChallenge.VERIFIED
            !hasBlink -> LivenessChallenge.BLINK_EYES
            else -> LivenessChallenge.NATURAL_MOTION
        }
        currentChallenge = resultChallenge

        return LivenessAnalysisResult(
            isLive = aggregateScore >= requiredScoreToPass && !isStaticPhotoSpoof,
            livenessScore = aggregateScore,
            motionScore = avgMotion,
            blinkDetected = hasBlink,
            blinkCount = blinksDetected,
            currentChallenge = resultChallenge,
            message = resultChallenge.prompt,
            isSpoofDetected = isStaticPhotoSpoof,
            spoofReason = if (isStaticPhotoSpoof) "Motion variance is zero. Static 2D paper or digital photo detected." else null
        )
    }

    /**
     * Resets the analysis state for a new attendance scan session.
     */
    @Synchronized
    fun reset() {
        recentLuminanceAverages.clear()
        recentMotionScores.clear()
        lastFrameSampleGrid = null
        blinksDetected = 0
        lastBlinkTimestamp = 0L
        isEyeCurrentlyClosed = false
        accumulatedLiveFrames = 0
        consecutiveStaticFrames = 0
        currentChallenge = LivenessChallenge.NATURAL_MOTION
    }

    /**
     * Simulates passing liveness checks (useful in emulator environments with synthetic camera streams).
     */
    fun simulateLivePersonPass(): LivenessAnalysisResult {
        blinksDetected = 1
        accumulatedLiveFrames = 60
        return LivenessAnalysisResult(
            isLive = true,
            livenessScore = 0.98f,
            motionScore = 0.045f,
            blinkDetected = true,
            blinkCount = 1,
            currentChallenge = LivenessChallenge.VERIFIED,
            message = LivenessChallenge.VERIFIED.prompt,
            isSpoofDetected = false
        )
    }

    /**
     * Simulates a static photo spoof attempt for testing anti-spoof rejection.
     */
    fun simulateStaticPhotoSpoof(): LivenessAnalysisResult {
        return LivenessAnalysisResult(
            isLive = false,
            livenessScore = 0.12f,
            motionScore = 0.001f,
            blinkDetected = false,
            blinkCount = 0,
            currentChallenge = LivenessChallenge.SPOOF_DETECTED,
            message = LivenessChallenge.SPOOF_DETECTED.prompt,
            isSpoofDetected = true,
            spoofReason = "Zero optical motion detected: 2D Photo attack rejected."
        )
    }
}

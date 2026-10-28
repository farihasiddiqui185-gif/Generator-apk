package com.example.robloxshorts.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import com.example.robloxshorts.data.model.RobloxScene
import com.example.robloxshorts.data.model.RobloxScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.sin

class RobloxVideoRenderer(private val context: Context) {
    private val width = 720
    private val height = 1280
    private val frameRate = 30
    private val bitRate = 3_000_000 // 3 Mbps
    private val iFrameInterval = 1 // 1 sec

    suspend fun renderVideo(
        script: RobloxScript,
        audioWavFile: File,
        onProgress: (Float, String) -> Unit
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "videos").apply { mkdirs() }
        val videoId = System.currentTimeMillis()
        val videoFile = File(outputDir, "roblox_short_$videoId.mp4")
        val thumbFile = File(outputDir, "thumb_$videoId.jpg")

        val totalDurationSec = script.estimatedDurationSec
        val totalFrames = totalDurationSec * frameRate

        // 1. Setup Video MediaFormat
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        }

        val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = videoEncoder.createInputSurface()
        videoEncoder.start()

        val muxer = MediaMuxer(videoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var muxerStarted = false

        // Also setup Audio Encoder for the synthesized WAV
        val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioEncoder.start()

        val videoBufferInfo = MediaCodec.BufferInfo()
        val audioBufferInfo = MediaCodec.BufferInfo()

        // Extract audio PCM
        val audioBytes = audioWavFile.readBytes().drop(44).toByteArray() // skip 44-byte WAV header
        var audioBytesSent = 0
        var audioEOS = false

        var thumbnailSaved = false
        val midFrame = totalFrames / 2

        try {
            onProgress(0.05f, "Initializing Roblox 3D Canvas Engine...")

            // Frame rendering loop
            for (frame in 0 until totalFrames) {
                val currentTimeSec = frame.toFloat() / frameRate
                val ptsUs = (frame * 1_000_000L) / frameRate

                // Find active scene
                var accumulatedTime = 0f
                var activeScene = script.scenes.first()
                for (scene in script.scenes) {
                    if (currentTimeSec >= accumulatedTime && currentTimeSec < accumulatedTime + scene.durationSec) {
                        activeScene = scene
                        break
                    }
                    accumulatedTime += scene.durationSec
                }
                val sceneLocalTime = currentTimeSec - accumulatedTime

                // Lock Canvas and Draw Frame
                val canvas = inputSurface.lockCanvas(null)
                drawRobloxShortFrame(
                    canvas = canvas,
                    script = script,
                    scene = activeScene,
                    currentTimeSec = currentTimeSec,
                    sceneLocalTime = sceneLocalTime,
                    totalDurationSec = totalDurationSec
                )

                // Capture middle frame for thumbnail
                if (frame == midFrame && !thumbnailSaved) {
                    val thumbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val thumbCanvas = Canvas(thumbBitmap)
                    drawRobloxShortFrame(
                        canvas = thumbCanvas,
                        script = script,
                        scene = activeScene,
                        currentTimeSec = currentTimeSec,
                        sceneLocalTime = sceneLocalTime,
                        totalDurationSec = totalDurationSec
                    )
                    FileOutputStream(thumbFile).use { out ->
                        thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    thumbnailSaved = true
                }

                inputSurface.unlockCanvasAndPost(canvas)

                // Feed audio samples incrementally
                if (!audioEOS) {
                    val inputBufIndex = audioEncoder.dequeueInputBuffer(0)
                    if (inputBufIndex >= 0) {
                        val inputBuf = audioEncoder.getInputBuffer(inputBufIndex)!!
                        inputBuf.clear()
                        val bytesRemaining = audioBytes.size - audioBytesSent
                        val bytesToCopy = minOf(inputBuf.remaining(), bytesRemaining)
                        if (bytesToCopy > 0) {
                            inputBuf.put(audioBytes, audioBytesSent, bytesToCopy)
                            val audioPtsUs = (audioBytesSent / 4L * 1_000_000L) / 44100
                            audioEncoder.queueInputBuffer(inputBufIndex, 0, bytesToCopy, audioPtsUs, 0)
                            audioBytesSent += bytesToCopy
                        } else {
                            audioEncoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            audioEOS = true
                        }
                    }
                }

                // Drain video encoder
                drainEncoder(
                    encoder = videoEncoder,
                    bufferInfo = videoBufferInfo,
                    muxer = muxer,
                    trackIndexProvider = { videoTrackIndex },
                    onFormatChanged = { newFormat ->
                        videoTrackIndex = muxer.addTrack(newFormat)
                        if (audioTrackIndex != -1 && !muxerStarted) {
                            muxer.start()
                            muxerStarted = true
                        }
                    },
                    isMuxerStarted = { muxerStarted }
                )

                // Drain audio encoder
                drainEncoder(
                    encoder = audioEncoder,
                    bufferInfo = audioBufferInfo,
                    muxer = muxer,
                    trackIndexProvider = { audioTrackIndex },
                    onFormatChanged = { newFormat ->
                        audioTrackIndex = muxer.addTrack(newFormat)
                        if (videoTrackIndex != -1 && !muxerStarted) {
                            muxer.start()
                            muxerStarted = true
                        }
                    },
                    isMuxerStarted = { muxerStarted }
                )

                if (frame % 30 == 0) {
                    val progressFraction = 0.10f + (frame.toFloat() / totalFrames) * 0.80f
                    onProgress(progressFraction, "Rendering Scene ${activeScene.sceneNumber}: ${activeScene.title} (${frame}/$totalFrames frames)")
                }
            }

            // Signal end of video stream
            videoEncoder.signalEndOfInputStream()

            // Final drain until EOS
            drainAllEOS(
                videoEncoder = videoEncoder,
                audioEncoder = audioEncoder,
                videoBufferInfo = videoBufferInfo,
                audioBufferInfo = audioBufferInfo,
                muxer = muxer,
                videoTrackIndex = videoTrackIndex,
                audioTrackIndex = audioTrackIndex,
                isMuxerStarted = { muxerStarted }
            )

            onProgress(0.95f, "Finalizing MP4 container...")

        } finally {
            try {
                videoEncoder.stop()
                videoEncoder.release()
            } catch (e: Exception) {
                Log.e("RobloxRenderer", "Error stopping videoEncoder", e)
            }
            try {
                audioEncoder.stop()
                audioEncoder.release()
            } catch (e: Exception) {
                Log.e("RobloxRenderer", "Error stopping audioEncoder", e)
            }
            try {
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (e: Exception) {
                Log.e("RobloxRenderer", "Error stopping muxer", e)
            }
            inputSurface.release()
        }

        // Fallback thumbnail if somehow not saved
        if (!thumbFile.exists() || thumbFile.length() == 0L) {
            val fallbackBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val fallbackCanvas = Canvas(fallbackBitmap)
            drawRobloxShortFrame(
                canvas = fallbackCanvas,
                script = script,
                scene = script.scenes.first(),
                currentTimeSec = 1f,
                sceneLocalTime = 1f,
                totalDurationSec = totalDurationSec
            )
            FileOutputStream(thumbFile).use { out ->
                fallbackBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        }

        onProgress(1.0f, "Video Complete!")
        Pair(videoFile, thumbFile)
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        trackIndexProvider: () -> Int,
        onFormatChanged: (MediaFormat) -> Unit,
        isMuxerStarted: () -> Boolean
    ) {
        while (true) {
            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                onFormatChanged(encoder.outputFormat)
            } else if (encoderStatus >= 0) {
                val encodedData = encoder.getOutputBuffer(encoderStatus) ?: continue
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0 && isMuxerStarted()) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    val trackIndex = trackIndexProvider()
                    if (trackIndex >= 0) {
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(encoderStatus, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun drainAllEOS(
        videoEncoder: MediaCodec,
        audioEncoder: MediaCodec,
        videoBufferInfo: MediaCodec.BufferInfo,
        audioBufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        videoTrackIndex: Int,
        audioTrackIndex: Int,
        isMuxerStarted: () -> Boolean
    ) {
        var videoEOS = false
        var audioEOS = false
        var loopCount = 0

        while ((!videoEOS || !audioEOS) && loopCount < 50) {
            loopCount++
            if (!videoEOS) {
                val status = videoEncoder.dequeueOutputBuffer(videoBufferInfo, 10000)
                if (status >= 0) {
                    val encodedData = videoEncoder.getOutputBuffer(status)!!
                    if (videoBufferInfo.size > 0 && isMuxerStarted() && videoTrackIndex >= 0) {
                        encodedData.position(videoBufferInfo.offset)
                        encodedData.limit(videoBufferInfo.offset + videoBufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, videoBufferInfo)
                    }
                    videoEncoder.releaseOutputBuffer(status, false)
                    if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        videoEOS = true
                    }
                } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    videoEOS = true
                }
            }

            if (!audioEOS) {
                val status = audioEncoder.dequeueOutputBuffer(audioBufferInfo, 10000)
                if (status >= 0) {
                    val encodedData = audioEncoder.getOutputBuffer(status)!!
                    if (audioBufferInfo.size > 0 && isMuxerStarted() && audioTrackIndex >= 0) {
                        encodedData.position(audioBufferInfo.offset)
                        encodedData.limit(audioBufferInfo.offset + audioBufferInfo.size)
                        muxer.writeSampleData(audioTrackIndex, encodedData, audioBufferInfo)
                    }
                    audioEncoder.releaseOutputBuffer(status, false)
                    if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        audioEOS = true
                    }
                } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    audioEOS = true
                }
            }
        }
    }

    private fun drawRobloxShortFrame(
        canvas: Canvas,
        script: RobloxScript,
        scene: RobloxScene,
        currentTimeSec: Float,
        sceneLocalTime: Float,
        totalDurationSec: Int
    ) {
        val t = currentTimeSec
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Camera Motion Calculation
        canvas.save()
        when (scene.cameraMotion) {
            "ZOOM_IN" -> {
                val zoom = 1.0f + (sceneLocalTime / scene.durationSec) * 0.12f
                canvas.scale(zoom, zoom, width / 2f, height / 2f)
            }
            "PAN_RIGHT" -> {
                val panX = (sceneLocalTime / scene.durationSec) * 35f - 17f
                canvas.translate(-panX, 0f)
            }
            "SCREEN_SHAKE" -> {
                if (sceneLocalTime < 1.2f) {
                    val shake = sin(sceneLocalTime * 35.0).toFloat() * 12f * (1.2f - sceneLocalTime)
                    canvas.translate(shake, shake * 0.7f)
                }
            }
            "ORBIT" -> {
                val angle = sin(sceneLocalTime * 2.0).toFloat() * 3.5f
                canvas.rotate(angle, width / 2f, height / 2f)
            }
        }

        // 1. Draw Environment Backdrop
        drawBackdrop(canvas, scene.environment, t)

        // 2. Draw Roblox 3D Character
        drawRobloxCharacter(canvas, scene.characterType, scene.characterExpression, t)

        // 3. Draw Roblox Chat Bubble if Dialogue exists
        scene.characterDialogue?.let { dialogue ->
            drawRobloxChatBubble(canvas, dialogue, t)
        }

        // Restore camera
        canvas.restore()

        // 4. Draw HUD: Health bar & Roblox UI
        drawRobloxHUD(canvas, t)

        // 5. Draw Dynamic Animated Subtitles (YouTube Shorts viral style)
        drawAnimatedSubtitles(canvas, scene, sceneLocalTime)

        // 6. Draw Top Brand Bar & Progress Bar
        drawShortsTopBar(canvas, script.title, currentTimeSec, totalDurationSec)
    }

    private fun drawBackdrop(canvas: Canvas, environment: String, t: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (environment) {
            "SPOOKY_CORRIDOR" -> {
                // Dark spooky hallway with creepy purple / deep crimson gradient
                paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.rgb(18, 5, 30), Color.rgb(8, 2, 12), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Wall perspective lines
                paint.color = Color.argb(40, 160, 32, 240)
                paint.strokeWidth = 3f
                canvas.drawLine(0f, 0f, width * 0.25f, height * 0.45f, paint)
                canvas.drawLine(width.toFloat(), 0f, width * 0.75f, height * 0.45f, paint)
                canvas.drawLine(0f, height.toFloat(), width * 0.25f, height * 0.75f, paint)
                canvas.drawLine(width.toFloat(), height.toFloat(), width * 0.75f, height * 0.75f, paint)

                // Creepy red glowing doorway at end of hall
                paint.color = Color.argb((90 + 30 * sin(t * 4.0)).toInt(), 255, 20, 50)
                canvas.drawRect(width * 0.38f, height * 0.45f, width * 0.62f, height * 0.75f, paint)
            }
            "LAVA_OBBY" -> {
                // Red/orange fiery sky with boiling lava
                paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.rgb(40, 10, 0), Color.rgb(120, 20, 0), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Boiling lava floor at bottom
                paint.shader = LinearGradient(0f, height * 0.75f, 0f, height.toFloat(), Color.rgb(255, 140, 0), Color.rgb(255, 30, 0), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, height * 0.75f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Floating Obby stone platforms
                paint.color = Color.rgb(50, 50, 60)
                canvas.drawRoundRect(RectF(120f, height * 0.65f, 320f, height * 0.70f), 8f, 8f, paint)
                canvas.drawRoundRect(RectF(400f, height * 0.58f, 600f, height * 0.63f), 8f, 8f, paint)
            }
            "ROBLOX_SCHOOL" -> {
                // School hallway / lockers
                paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.rgb(24, 30, 54), Color.rgb(15, 18, 35), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Cyan lockers along sides
                paint.color = Color.rgb(30, 60, 110)
                canvas.drawRect(0f, height * 0.3f, 140f, height * 0.8f, paint)
                canvas.drawRect(width - 140f, height * 0.3f, width.toFloat(), height * 0.8f, paint)

                // Floor grid tiles
                paint.color = Color.argb(40, 255, 255, 255)
                paint.strokeWidth = 2f
                for (y in (height * 0.75f).toInt() until height step 50) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                }
            }
            "SURVIVAL_ISLAND" -> {
                // Stormy ocean and meteor island
                paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.rgb(10, 25, 45), Color.rgb(5, 15, 25), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Ocean waves
                paint.color = Color.rgb(15, 60, 100)
                canvas.drawRect(0f, height * 0.72f, width.toFloat(), height.toFloat(), paint)

                // Falling meteors with trail
                val meteorY = ((t * 280f) % (height * 0.8f))
                val meteorX = ((t * 180f) % width)
                paint.color = Color.rgb(255, 120, 0)
                canvas.drawCircle(meteorX, meteorY, 16f, paint)
                paint.color = Color.argb(120, 255, 200, 0)
                canvas.drawLine(meteorX, meteorY, meteorX - 50f, meteorY - 70f, paint)
            }
            "VAULT_ROOM" -> {
                // Golden glittering vault
                paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.rgb(35, 28, 5), Color.rgb(12, 10, 2), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Golden vault door
                paint.color = Color.rgb(212, 175, 55)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 14f
                canvas.drawCircle(width / 2f, height * 0.5f, 220f, paint)
                paint.style = Paint.Style.FILL
            }
            else -> { // NEON_CITY
                paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.rgb(15, 7, 36), Color.rgb(5, 2, 18), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Neon synthwave grid floor
                paint.color = Color.argb(90, 0, 240, 255)
                paint.strokeWidth = 3f
                val horizonY = height * 0.70f
                canvas.drawLine(0f, horizonY, width.toFloat(), horizonY, paint)

                for (x in 0..width step 70) {
                    canvas.drawLine(x.toFloat(), horizonY, (x - width / 2f) * 2.5f + width / 2f, height.toFloat(), paint)
                }
            }
        }
    }

    private fun drawRobloxCharacter(canvas: Canvas, characterType: String, expression: String, t: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Character position and natural breathing / walking bounce
        val bounceY = sin(t * 7.0).toFloat() * 12f
        val legSwing = sin(t * 7.0).toFloat() * 14f
        val armSwing = sin(t * 7.0).toFloat() * 18f

        val centerX = width / 2f
        val baseY = height * 0.52f + bounceY

        // Colors per character type
        val (skinColor, torsoColor, legColor) = when (characterType) {
            "BACON_HAIR" -> Triple(Color.rgb(255, 219, 172), Color.rgb(41, 128, 185), Color.rgb(52, 73, 94))
            "PRO_GAMER" -> Triple(Color.rgb(255, 224, 189), Color.rgb(30, 30, 30), Color.rgb(20, 20, 20))
            "ENTITY_666" -> Triple(Color.rgb(20, 5, 10), Color.rgb(10, 0, 0), Color.rgb(0, 0, 0))
            "DOMINUS_RICH" -> Triple(Color.rgb(255, 224, 189), Color.rgb(106, 27, 154), Color.rgb(74, 20, 140))
            "STUDENT_GIRL" -> Triple(Color.rgb(255, 224, 189), Color.rgb(233, 30, 99), Color.rgb(63, 81, 181))
            else -> Triple(Color.rgb(255, 219, 172), Color.rgb(41, 128, 185), Color.rgb(52, 73, 94))
        }

        // Draw Shadows
        paint.color = Color.argb(80, 0, 0, 0)
        canvas.drawOval(RectF(centerX - 130f, height * 0.72f, centerX + 130f, height * 0.76f), paint)

        // --- 1. LEGS (Blocky) ---
        paint.color = legColor
        // Left Leg
        canvas.drawRoundRect(RectF(centerX - 85f, baseY + 90f + legSwing, centerX - 15f, baseY + 240f + legSwing), 10f, 10f, paint)
        // Right Leg
        canvas.drawRoundRect(RectF(centerX + 15f, baseY + 90f - legSwing, centerX + 85f, baseY + 240f - legSwing), 10f, 10f, paint)

        // --- 2. TORSO (Blocky) ---
        paint.color = torsoColor
        canvas.drawRoundRect(RectF(centerX - 95f, baseY - 80f, centerX + 95f, baseY + 95f), 12f, 12f, paint)

        // Dominus gold necklace / logo or Roblox "R" logo on chest
        if (characterType == "DOMINUS_RICH") {
            paint.color = Color.rgb(255, 215, 0)
            canvas.drawCircle(centerX, baseY, 22f, paint)
        } else if (characterType == "PRO_GAMER") {
            paint.color = Color.rgb(0, 240, 255)
            canvas.drawRect(centerX - 25f, baseY - 20f, centerX + 25f, baseY + 20f, paint)
        }

        // --- 3. ARMS (Blocky) ---
        paint.color = if (characterType == "ENTITY_666") Color.rgb(180, 0, 0) else skinColor
        // Left Arm
        canvas.drawRoundRect(RectF(centerX - 165f, baseY - 70f + armSwing, centerX - 105f, baseY + 80f + armSwing), 10f, 10f, paint)
        // Right Arm
        canvas.drawRoundRect(RectF(centerX + 105f, baseY - 70f - armSwing, centerX + 165f, baseY + 80f - armSwing), 10f, 10f, paint)

        // --- 4. HEAD (Classic Blocky Roblox Head with Studs) ---
        paint.color = skinColor
        val headLeft = centerX - 65f
        val headTop = baseY - 210f
        val headRight = centerX + 65f
        val headBottom = baseY - 85f
        canvas.drawRoundRect(RectF(headLeft, headTop, headRight, headBottom), 16f, 16f, paint)

        // Head Top Stud
        canvas.drawRoundRect(RectF(centerX - 24f, headTop - 14f, centerX + 24f, headTop), 6f, 6f, paint)

        // --- 5. ROBLOX FACE ---
        drawRobloxFace(canvas, centerX, baseY - 150f, expression, characterType)

        // --- 6. ACCESSORIES ---
        when (characterType) {
            "BACON_HAIR" -> {
                // Classic Bacon Hair pieces
                paint.color = Color.rgb(110, 50, 20) // brown
                canvas.drawRoundRect(RectF(headLeft - 10f, headTop - 8f, headRight + 10f, headTop + 25f), 8f, 8f, paint)
                // Bacon strips hanging
                paint.color = Color.rgb(205, 133, 63)
                canvas.drawRoundRect(RectF(headLeft - 8f, headTop + 15f, headLeft + 16f, headTop + 75f), 6f, 6f, paint)
                canvas.drawRoundRect(RectF(headRight - 16f, headTop + 15f, headRight + 8f, headTop + 75f), 6f, 6f, paint)
            }
            "PRO_GAMER" -> {
                // Neon Gaming Headset
                paint.color = Color.rgb(0, 240, 255)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 12f
                canvas.drawArc(RectF(headLeft - 15f, headTop - 25f, headRight + 15f, headBottom - 20f), 180f, 180f, false, paint)
                paint.style = Paint.Style.FILL
                // Earcups
                canvas.drawRoundRect(RectF(headLeft - 22f, headTop + 20f, headLeft - 5f, headTop + 70f), 8f, 8f, paint)
                canvas.drawRoundRect(RectF(headRight + 5f, headTop + 20f, headRight + 22f, headTop + 70f), 8f, 8f, paint)
            }
            "DOMINUS_RICH" -> {
                // Dominus Hood and Golden Horns
                paint.color = Color.rgb(74, 20, 140)
                canvas.drawRoundRect(RectF(headLeft - 18f, headTop - 20f, headRight + 18f, headBottom + 10f), 20f, 20f, paint)
                // Glowing eyes inside dark void of hood
                paint.color = Color.rgb(255, 215, 0)
                canvas.drawCircle(centerX - 25f, baseY - 150f, 9f, paint)
                canvas.drawCircle(centerX + 25f, baseY - 150f, 9f, paint)
            }
            "ENTITY_666" -> {
                // Red glowing smoke aura
                paint.color = Color.argb(160, 255, 0, 50)
                canvas.drawCircle(centerX - 24f, baseY - 150f, 14f, paint)
                canvas.drawCircle(centerX + 24f, baseY - 150f, 14f, paint)
            }
        }
    }

    private fun drawRobloxFace(canvas: Canvas, cx: Float, cy: Float, expression: String, characterType: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (characterType == "DOMINUS_RICH") return // Eyes handled in hood

        when (expression) {
            "SHOCKED" -> {
                // Huge wide round eyes
                paint.color = Color.WHITE
                canvas.drawCircle(cx - 24f, cy - 8f, 16f, paint)
                canvas.drawCircle(cx + 24f, cy - 8f, 16f, paint)
                // Black pupils
                paint.color = Color.BLACK
                canvas.drawCircle(cx - 24f, cy - 8f, 7f, paint)
                canvas.drawCircle(cx + 24f, cy - 8f, 7f, paint)
                // Huge open 'O' mouth
                canvas.drawOval(RectF(cx - 15f, cy + 18f, cx + 15f, cy + 45f), paint)
            }
            "LAUGHING" -> {
                // Happy curved eyes ^ ^
                paint.color = Color.BLACK
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                val leftPath = Path().apply {
                    moveTo(cx - 36f, cy - 2f)
                    lineTo(cx - 24f, cy - 14f)
                    lineTo(cx - 12f, cy - 2f)
                }
                canvas.drawPath(leftPath, paint)
                val rightPath = Path().apply {
                    moveTo(cx + 12f, cy - 2f)
                    lineTo(cx + 24f, cy - 14f)
                    lineTo(cx + 36f, cy - 2f)
                }
                canvas.drawPath(rightPath, paint)
                // Big grinning mouth
                paint.style = Paint.Style.FILL
                canvas.drawArc(RectF(cx - 26f, cy + 8f, cx + 26f, cy + 36f), 0f, 180f, true, paint)
            }
            "TERRIFIED" -> {
                paint.color = Color.WHITE
                canvas.drawCircle(cx - 24f, cy - 6f, 14f, paint)
                canvas.drawCircle(cx + 24f, cy - 6f, 14f, paint)
                paint.color = Color.BLACK
                canvas.drawCircle(cx - 24f, cy - 6f, 5f, paint)
                canvas.drawCircle(cx + 24f, cy - 6f, 5f, paint)
                // Wobbly mouth
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                val mouth = Path().apply {
                    moveTo(cx - 20f, cy + 22f)
                    quadTo(cx - 10f, cy + 30f, cx, cy + 22f)
                    quadTo(cx + 10f, cy + 14f, cx + 20f, cy + 22f)
                }
                canvas.drawPath(mouth, paint)
                paint.style = Paint.Style.FILL
                // Blue sweat drop
                paint.color = Color.rgb(0, 191, 255)
                canvas.drawCircle(cx + 42f, cy - 15f, 6f, paint)
            }
            "ANGRY" -> {
                paint.color = Color.BLACK
                paint.strokeWidth = 6f
                // Slanted eyebrows
                canvas.drawLine(cx - 38f, cy - 22f, cx - 12f, cy - 10f, paint)
                canvas.drawLine(cx + 38f, cy - 22f, cx + 12f, cy - 10f, paint)
                canvas.drawCircle(cx - 22f, cy - 6f, 7f, paint)
                canvas.drawCircle(cx + 22f, cy - 6f, 7f, paint)
                // Grimace teeth line
                canvas.drawLine(cx - 24f, cy + 22f, cx + 24f, cy + 22f, paint)
            }
            else -> { // COOL / SMILE
                paint.color = Color.BLACK
                canvas.drawCircle(cx - 22f, cy - 8f, 7f, paint)
                canvas.drawCircle(cx + 22f, cy - 8f, 7f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                canvas.drawArc(RectF(cx - 18f, cy + 6f, cx + 18f, cy + 26f), 0f, 180f, false, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    private fun drawRobloxChatBubble(canvas: Canvas, text: String, t: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Bubble coordinates above character
        val bubbleW = minOf(580f, maxOf(260f, text.length * 15f + 40f))
        val bubbleH = 80f
        val bubbleLeft = (width - bubbleW) / 2f
        val bubbleTop = height * 0.25f + sin(t * 5.0).toFloat() * 6f

        // Bubble background (White with rounded corners & shadow)
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(bubbleLeft, bubbleTop, bubbleLeft + bubbleW, bubbleTop + bubbleH), 20f, 20f, paint)

        // Little speech pointer triangle
        val pointer = Path().apply {
            moveTo(width / 2f - 14f, bubbleTop + bubbleH)
            lineTo(width / 2f + 14f, bubbleTop + bubbleH)
            lineTo(width / 2f, bubbleTop + bubbleH + 18f)
            close()
        }
        canvas.drawPath(pointer, paint)

        // Dialogue text
        paint.color = Color.rgb(20, 20, 20)
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text.take(35), width / 2f, bubbleTop + 48f, paint)
    }

    private fun drawRobloxHUD(canvas: Canvas, t: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top-Left Classic Roblox Health Bar
        val hudLeft = 32f
        val hudTop = 110f

        // Dark background plate
        paint.color = Color.argb(190, 15, 15, 20)
        canvas.drawRoundRect(RectF(hudLeft, hudTop, hudLeft + 220f, hudTop + 36f), 8f, 8f, paint)

        // Green health bar fill (flashing red if low)
        paint.color = Color.rgb(50, 205, 50)
        canvas.drawRoundRect(RectF(hudLeft + 4f, hudTop + 4f, hudLeft + 200f, hudTop + 32f), 6f, 6f, paint)

        // Heart icon
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("❤ 100/100", hudLeft + 35f, hudTop + 26f, paint)
    }

    private fun drawAnimatedSubtitles(canvas: Canvas, scene: RobloxScene, sceneLocalTime: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (scene.subtitles.isEmpty()) {
            if (scene.narration.isNotBlank()) {
                drawSubtitleBox(canvas, scene.narration, isHighlight = true)
            }
            return
        }

        // Find active word in scene
        val activeWord = scene.subtitles.firstOrNull {
            sceneLocalTime >= it.startSec && sceneLocalTime <= it.endSec
        } ?: scene.subtitles.firstOrNull()

        activeWord?.let {
            drawSubtitleBox(canvas, it.word, it.isHighlighted)
        }
    }

    private fun drawSubtitleBox(canvas: Canvas, text: String, isHighlight: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val boxY = height * 0.82f
        val displayText = text.uppercase()

        paint.textSize = if (isHighlight) 52f else 46f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        val textWidth = paint.measureText(displayText)
        val padX = 36f
        val padY = 24f

        // Glowing pill background
        paint.color = Color.argb(220, 10, 5, 25)
        canvas.drawRoundRect(
            RectF(width / 2f - textWidth / 2f - padX, boxY - 45f, width / 2f + textWidth / 2f + padX, boxY + padY),
            24f, 24f, paint
        )

        // Neon border on pill
        paint.color = if (isHighlight) Color.rgb(0, 240, 255) else Color.rgb(250, 204, 21)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawRoundRect(
            RectF(width / 2f - textWidth / 2f - padX, boxY - 45f, width / 2f + textWidth / 2f + padX, boxY + padY),
            24f, 24f, paint
        )

        // Text Outline (thick black stroke)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 14f
        paint.color = Color.BLACK
        canvas.drawText(displayText, width / 2f, boxY, paint)

        // Text Fill (Bright yellow or cyan)
        paint.style = Paint.Style.FILL
        paint.color = if (isHighlight) Color.rgb(0, 240, 255) else Color.rgb(255, 235, 59)
        canvas.drawText(displayText, width / 2f, boxY, paint)
    }

    private fun drawShortsTopBar(canvas: Canvas, title: String, currentTimeSec: Float, totalSec: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top Gradient overlay
        paint.shader = LinearGradient(0f, 0f, 0f, 140f, Color.argb(220, 12, 7, 30), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), 140f, paint)
        paint.shader = null

        // Brand Badge: ROBLOX AI SHORTS
        paint.color = Color.rgb(168, 85, 247)
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("⚡ ROBLOX SHORTS FACTORY", 36f, 60f, paint)

        // Title preview
        paint.color = Color.WHITE
        paint.textSize = 28f
        canvas.drawText(title.take(32), 36f, 96f, paint)

        // Bottom Progress Bar (Instagram / TikTok style timeline)
        val progressFrac = (currentTimeSec / totalSec).coerceIn(0f, 1f)
        paint.color = Color.argb(100, 255, 255, 255)
        canvas.drawRect(0f, height - 12f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.rgb(0, 240, 255)
        canvas.drawRect(0f, height - 12f, width * progressFrac, height.toFloat(), paint)
    }
}

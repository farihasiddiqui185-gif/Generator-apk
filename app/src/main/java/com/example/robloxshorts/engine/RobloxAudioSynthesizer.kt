package com.example.robloxshorts.engine

import android.content.Context
import com.example.robloxshorts.data.model.RobloxScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class RobloxAudioSynthesizer(private val context: Context) {
    private val sampleRate = 44100

    suspend fun synthesizeAudio(script: RobloxScript, outputFile: File): File = withContext(Dispatchers.IO) {
        val totalDurationSec = script.estimatedDurationSec
        val totalSamples = totalDurationSec * sampleRate
        val pcmData = ShortArray(totalSamples * 2) // Stereo

        // 1. Synthesize background music style
        synthesizeBackgroundMusic(script.musicStyle, totalDurationSec, pcmData)

        // 2. Mix in sound effects at scene boundaries
        var currentOffsetSec = 0f
        script.scenes.forEach { scene ->
            scene.soundFx?.let { fx ->
                mixSoundEffect(fx, (currentOffsetSec * sampleRate).toInt(), pcmData)
            }
            currentOffsetSec += scene.durationSec
        }

        // 3. Write PCM data as valid 16-bit stereo WAV file
        writeWavFile(outputFile, pcmData, sampleRate, 2)
        outputFile
    }

    private fun synthesizeBackgroundMusic(musicStyle: String, durationSec: Int, outPcm: ShortArray) {
        val bpm = when (musicStyle) {
            "UPBEAT_GAMING" -> 132
            "SPOOKY_SYNTH" -> 90
            "DRAMATIC_PIANO" -> 78
            else -> 120
        }
        val beatDurationSamples = (60.0 / bpm * sampleRate).toInt()

        // Upbeat gaming chords / spooky chords
        val chordFreqs = when (musicStyle) {
            "SPOOKY_SYNTH" -> listOf(
                doubleArrayOf(220.0, 261.63, 311.13), // A minor diminished
                doubleArrayOf(207.65, 246.94, 311.13), // G# minor
                doubleArrayOf(196.0, 233.08, 293.66), // G minor
                doubleArrayOf(185.0, 220.0, 277.18)  // F# minor
            )
            "DRAMATIC_PIANO" -> listOf(
                doubleArrayOf(261.63, 329.63, 392.0), // C major
                doubleArrayOf(220.0, 261.63, 329.63), // A minor
                doubleArrayOf(174.61, 220.0, 261.63), // F major
                doubleArrayOf(196.0, 246.94, 293.66)  // G major
            )
            else -> listOf( // UPBEAT_GAMING
                doubleArrayOf(293.66, 369.99, 440.0), // D major
                doubleArrayOf(329.63, 392.0, 493.88), // E minor
                doubleArrayOf(392.0, 493.88, 587.33), // G major
                doubleArrayOf(440.0, 554.37, 659.25)  // A major
            )
        }

        val totalBeats = (durationSec * (bpm / 60.0)).toInt()
        for (beat in 0 until totalBeats) {
            val startSample = beat * beatDurationSamples
            val chord = chordFreqs[(beat / 4) % chordFreqs.size]
            val isKickBeat = (beat % 2 == 0)
            val isSnareBeat = (beat % 2 == 1)

            for (i in 0 until beatDurationSamples) {
                val sampleIdx = startSample + i
                if (sampleIdx >= outPcm.size / 2) break

                val t = i.toDouble() / sampleRate
                val globalT = sampleIdx.toDouble() / sampleRate

                var synthSample = 0.0

                // Arpeggio note
                val arpIndex = (i / (beatDurationSamples / 4)) % chord.size
                val noteFreq = chord[arpIndex] * (if (musicStyle == "UPBEAT_GAMING") 2.0 else 1.0)
                val noteEnv = exp(-4.0 * ((i % (beatDurationSamples / 4)).toDouble() / (beatDurationSamples / 4)))

                // Square / sine wave with gentle chorus
                val voice1 = sin(2.0 * PI * noteFreq * t)
                val voice2 = sin(2.0 * PI * (noteFreq * 1.005) * t)
                synthSample += (voice1 + voice2) * 0.18 * noteEnv

                // Bass note
                val bassFreq = chord[0] * 0.5
                val bass = sin(2.0 * PI * bassFreq * t) * 0.22 * exp(-2.0 * t)
                synthSample += bass

                // Percussion (Kick drum on downbeats, snare on backbeats)
                if (isKickBeat && i < 6000) {
                    val kickT = i.toDouble() / sampleRate
                    val kickFreq = 140.0 * exp(-25.0 * kickT) + 40.0
                    val kickEnv = exp(-18.0 * kickT)
                    synthSample += sin(2.0 * PI * kickFreq * kickT) * 0.35 * kickEnv
                }
                if (isSnareBeat && i < 8000) {
                    val snareT = i.toDouble() / sampleRate
                    val noise = (Math.random() * 2.0 - 1.0)
                    val snareEnv = exp(-15.0 * snareT)
                    synthSample += noise * 0.20 * snareEnv
                }

                val finalShort = (synthSample.coerceIn(-0.95, 0.95) * 32767).toInt().toShort()
                outPcm[sampleIdx * 2] = finalShort // Left
                outPcm[sampleIdx * 2 + 1] = finalShort // Right
            }
        }
    }

    private fun mixSoundEffect(fx: String, startSample: Int, outPcm: ShortArray) {
        val totalChannels = 2
        val maxLen = (sampleRate * 1.5).toInt() // 1.5s max effect

        for (i in 0 until maxLen) {
            val sampleIdx = startSample + i
            if (sampleIdx >= outPcm.size / totalChannels) break

            val t = i.toDouble() / sampleRate
            var sampleVal = 0.0

            when (fx) {
                "OOF" -> {
                    // Roblox OOF: pitched downward boing
                    val freq = (650.0 - 380.0 * (t / 0.35)).coerceAtLeast(150.0)
                    val env = exp(-8.0 * t)
                    sampleVal = (sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)) * env * 0.65
                }
                "JUMPSCARE_BOOM" -> {
                    val sub = sin(2.0 * PI * (80.0 * exp(-6.0 * t)) * t) * 0.5
                    val noise = (Math.random() * 2.0 - 1.0) * exp(-12.0 * t) * 0.4
                    sampleVal = (sub + noise) * 0.8
                }
                "DRAMATIC_RISER" -> {
                    val riserFreq = 120.0 + (t / 1.2) * (t / 1.2) * 1100.0
                    val env = (t / 1.2).coerceIn(0.0, 1.0)
                    sampleVal = sin(2.0 * PI * riserFreq * t) * env * 0.45
                }
                "CHA_CHING" -> {
                    // High bell chimes
                    val tone1 = sin(2.0 * PI * 2093.0 * t) * exp(-7.0 * t)
                    val tone2 = if (t > 0.12) sin(2.0 * PI * 2637.0 * (t - 0.12)) * exp(-7.0 * (t - 0.12)) else 0.0
                    sampleVal = (tone1 + tone2) * 0.55
                }
                "CHIME" -> {
                    val chime1 = sin(2.0 * PI * 1318.5 * t) * exp(-5.0 * t)
                    val chime2 = if (t > 0.08) sin(2.0 * PI * 1567.98 * (t - 0.08)) * exp(-5.0 * (t - 0.08)) else 0.0
                    val chime3 = if (t > 0.16) sin(2.0 * PI * 2093.0 * (t - 0.16)) * exp(-5.0 * (t - 0.16)) else 0.0
                    sampleVal = (chime1 + chime2 + chime3) * 0.40
                }
                else -> { // WHOOSH
                    val noise = (Math.random() * 2.0 - 1.0)
                    val env = sin(PI * (t / 0.45).coerceIn(0.0, 1.0))
                    sampleVal = noise * env * 0.35
                }
            }

            val existingL = outPcm[sampleIdx * 2].toDouble() / 32767.0
            val existingR = outPcm[sampleIdx * 2 + 1].toDouble() / 32767.0

            val mixedL = (existingL + sampleVal).coerceIn(-1.0, 1.0)
            val mixedR = (existingR + sampleVal).coerceIn(-1.0, 1.0)

            outPcm[sampleIdx * 2] = (mixedL * 32767).toInt().toShort()
            outPcm[sampleIdx * 2 + 1] = (mixedR * 32767).toInt().toShort()
        }
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int, channels: Int) {
        val byteData = ByteArray(pcmData.size * 2)
        ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmData)

        val totalDataLen = byteData.size + 36
        val bitRate = sampleRate * channels * 16 / 8

        FileOutputStream(file).use { out ->
            val header = ByteArray(44)
            // "RIFF"
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            // "WAVE"
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            // "fmt "
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // format chunk length
            header[20] = 1; header[21] = 0 // PCM format
            header[22] = channels.toByte(); header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (bitRate and 0xff).toByte()
            header[29] = ((bitRate shr 8) and 0xff).toByte()
            header[30] = ((bitRate shr 16) and 0xff).toByte()
            header[31] = ((bitRate shr 24) and 0xff).toByte()
            header[32] = (channels * 16 / 8).toByte(); header[33] = 0
            header[34] = 16; header[35] = 0 // bits per sample
            // "data"
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (byteData.size and 0xff).toByte()
            header[41] = ((byteData.size shr 8) and 0xff).toByte()
            header[42] = ((byteData.size shr 16) and 0xff).toByte()
            header[43] = ((byteData.size shr 24) and 0xff).toByte()

            out.write(header)
            out.write(byteData)
        }
    }
}

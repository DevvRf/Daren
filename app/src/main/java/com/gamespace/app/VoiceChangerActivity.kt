package com.gamespace.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.concurrent.thread

/**
 * HONEST SCOPE — read before assuming this is a live voice-chat changer:
 *
 * Android does not let a normal app intercept or replace the microphone
 * input that ANOTHER app (e.g. a game's in-game voice chat) sends over
 * the network. There is no public, non-root API for universal "voice
 * changer for any game's mic". Apps that claim this either (a) only
 * change audio inside their own app, or (b) require you to manually pick
 * their virtual microphone as the system input device, which most game
 * voice-chat implementations don't let you choose either.
 *
 * What THIS screen actually does, for real: records a short clip from
 * YOUR mic, applies a real pitch-shift (resampling-based) effect, and
 * plays it back so you can hear the result. That's it — a preview tool,
 * not a live cross-app voice changer. If you want this piped into a
 * specific game's voice chat, that game would need to explicitly support
 * picking a different input source, which is outside what any app
 * (rooted or not) can force on another app's audio pipeline.
 */
class VoiceChangerActivity : AppCompatActivity() {

    private val sampleRate = 16000
    private var recordedPcm: ShortArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_changer)

        val effectGroup = findViewById<RadioGroup>(R.id.effectGroup)
        val txtStatus = findViewById<TextView>(R.id.txtVoiceStatus)
        val btnRecord = findViewById<Button>(R.id.btnRecord)
        val btnPlay = findViewById<Button>(R.id.btnPlayWithEffect)

        btnRecord.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 42)
                return@setOnClickListener
            }
            txtStatus.text = "Merekam 3 detik…"
            thread {
                recordedPcm = recordClip(3000)
                runOnUiThread { txtStatus.text = "Rekaman selesai. Pilih efek lalu Putar." }
            }
        }

        btnPlay.setOnClickListener {
            val pcm = recordedPcm
            if (pcm == null) {
                Toast.makeText(this, "Rekam dulu suaranya", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val pitchFactor = when (effectGroup.checkedRadioButtonId) {
                R.id.radioPitchUp -> 1.4
                R.id.radioPitchDown -> 0.75
                R.id.radioRobot -> 1.0 // robot handled separately below
                R.id.radioDeep -> 0.6
                else -> 1.0
            }
            val isRobot = effectGroup.checkedRadioButtonId == R.id.radioRobot
            thread {
                val processed = if (isRobot) applyRobotEffect(pcm) else pitchShift(pcm, pitchFactor)
                playClip(processed)
            }
        }
    }

    /** Real microphone recording of this app's own input only. */
    private fun recordClip(durationMs: Int): ShortArray {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
        )
        val totalSamples = sampleRate * durationMs / 1000
        val buffer = ShortArray(totalSamples)
        recorder.startRecording()
        var read = 0
        while (read < totalSamples) {
            val n = recorder.read(buffer, read, totalSamples - read)
            if (n <= 0) break
            read += n
        }
        recorder.stop()
        recorder.release()
        return buffer
    }

    /** Real resampling-based pitch shift — changes perceived pitch and speed together (simple but genuine). */
    private fun pitchShift(input: ShortArray, factor: Double): ShortArray {
        val outLength = (input.size / factor).toInt().coerceAtLeast(1)
        val output = ShortArray(outLength)
        for (i in output.indices) {
            val srcIndex = (i * factor).toInt().coerceIn(0, input.size - 1)
            output[i] = input[srcIndex]
        }
        return output
    }

    /** Real ring-modulation-style "robot" effect: multiplies the signal by a carrier tone. */
    private fun applyRobotEffect(input: ShortArray): ShortArray {
        val carrierFreq = 80.0
        val output = ShortArray(input.size)
        for (i in input.indices) {
            val t = i.toDouble() / sampleRate
            val carrier = Math.sin(2.0 * Math.PI * carrierFreq * t)
            output[i] = (input[i] * carrier).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    private fun playClip(pcm: ShortArray) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .build()
        track.play()
        track.write(pcm, 0, pcm.size)
        track.stop()
        track.release()
    }
}

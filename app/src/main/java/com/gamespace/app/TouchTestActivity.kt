package com.gamespace.app

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Runs an 8-second real touch-capability test: TouchTestView records the
 * actual max simultaneous pointers and the actual average interval between
 * touch samples. The "score" shown is a documented heuristic computed from
 * those two real numbers — never a random or pre-set value.
 */
class TouchTestActivity : AppCompatActivity() {

    private lateinit var touchTestView: TouchTestView
    private lateinit var scoreRing: ScoreRingView
    private lateinit var txtResult: TextView
    private lateinit var btnStartTest: Button
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_touch_test)

        touchTestView = findViewById(R.id.touchTestView)
        scoreRing = findViewById(R.id.scoreRing)
        txtResult = findViewById(R.id.txtResult)
        btnStartTest = findViewById(R.id.btnStartTest)

        btnStartTest.setOnClickListener { startTest() }
    }

    private fun startTest() {
        touchTestView.reset()
        scoreRing.setScore(null)
        btnStartTest.isEnabled = false
        txtResult.text = "Testing… touch the area below now."

        timer?.cancel()
        timer = object : CountDownTimer(8000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                txtResult.text = "Testing… ${millisUntilFinished / 1000 + 1}s left — " +
                    "touch with as many fingers as you can."
            }

            override fun onFinish() {
                finishTest()
            }
        }.start()
    }

    private fun finishTest() {
        val maxPointers = touchTestView.maxSimultaneousPointers
        val avgIntervalMs = touchTestView.averageSampleIntervalMs()

        // Heuristic, computed from the two real measurements above:
        // up to 50 points for multi-touch breadth (capped at 10 fingers),
        // up to 50 points for sampling responsiveness (10ms interval ~ 100Hz
        // report rate scores full; 30ms+ scores zero).
        val pointerScore = (maxPointers.coerceAtMost(10) / 10.0 * 50.0)
        val latencyScore = avgIntervalMs?.let {
            val clamped = it.coerceIn(10.0, 30.0)
            50.0 * (1.0 - (clamped - 10.0) / 20.0)
        } ?: 0.0
        val score = (pointerScore + latencyScore).toInt().coerceIn(0, 100)

        scoreRing.setScore(score)
        txtResult.text = buildString {
            append("Max simultaneous touches detected: $maxPointers\n")
            append("Avg. touch sample interval: ")
            append(avgIntervalMs?.let { "%.1f ms (~%.0f Hz)".format(it, 1000.0 / it) } ?: "n/a")
            append("\nScore: $score / 100")
        }
        btnStartTest.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}

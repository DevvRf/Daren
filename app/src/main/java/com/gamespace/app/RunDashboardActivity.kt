package com.gamespace.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RunDashboardActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GAME_ID = "game_id"
        private const val HOLD_DURATION_MS = 1500L
    }

    private lateinit var store: GameProfileStore
    private lateinit var profile: GameProfile
    private lateinit var statsReader: SystemStatsReader
    private val pingTracker = PingTracker()
    private val handler = Handler(Looper.getMainLooper())
    private var holdTimer: CountDownTimer? = null

    private lateinit var txtCpu: TextView
    private lateinit var txtTemp: TextView
    private lateinit var txtPing: TextView
    private lateinit var txtHoldProgress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_dashboard)

        store = GameProfileStore(this)
        val gameId = intent.getStringExtra(EXTRA_GAME_ID)
        profile = store.loadAll().firstOrNull { it.id == gameId } ?: run { finish(); return }
        statsReader = SystemStatsReader(this)

        findViewById<TextView>(R.id.txtGameName).text = profile.displayName
        txtCpu = findViewById(R.id.txtCpu)
        txtTemp = findViewById(R.id.txtTemp)
        txtPing = findViewById(R.id.txtPing)
        txtHoldProgress = findViewById(R.id.txtHoldProgress)

        findViewById<Button>(R.id.btnGameSuite).setOnClickListener {
            val i = Intent(this, GameCustomizeActivity::class.java)
            i.putExtra(GameCustomizeActivity.EXTRA_GAME_ID, profile.id)
            startActivity(i)
            overridePendingTransition(R.anim.futuristic_enter, R.anim.futuristic_exit)
        }

        setupHoldToPlay()
        pingTracker.start()
        startStatsRefresh()
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupHoldToPlay() {
        val btn = findViewById<Button>(R.id.btnHoldToPlay)
        btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startHold()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelHold()
                    true
                }
                else -> false
            }
        }
    }

    private fun startHold() {
        holdTimer?.cancel()
        holdTimer = object : CountDownTimer(HOLD_DURATION_MS, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val percent = 100 - (millisUntilFinished * 100 / HOLD_DURATION_MS)
                txtHoldProgress.text = "$percent%"
            }
            override fun onFinish() {
                txtHoldProgress.text = "Memulai..."
                launchGameWithProfile()
            }
        }.start()
    }

    private fun cancelHold() {
        holdTimer?.cancel()
        txtHoldProgress.text = ""
    }

    private fun launchGameWithProfile() {
        GameSessionManager.applyProfile(this, profile)
        val pkg = profile.packageName
        if (pkg != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Game belum terinstall", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startStatsRefresh() {
        val runnable = object : Runnable {
            override fun run() {
                val snap = statsReader.snapshot(null)
                txtCpu.text = snap.cpuUsagePercent?.let { "%.0f%%".format(it) } ?: "n/a"
                txtTemp.text = (snap.cpuTempC ?: snap.batteryTempC)?.let { "%.1f°C".format(it) } ?: "n/a"

                val ping = pingTracker.latest()
                txtPing.text = ping.ms?.let { "$it ms" } ?: "n/a"
                val color = when (ping.quality) {
                    PingMonitor.PingQuality.GOOD -> android.graphics.Color.parseColor("#5CF25C")
                    PingMonitor.PingQuality.OK -> android.graphics.Color.parseColor("#F2C85C")
                    PingMonitor.PingQuality.BAD -> android.graphics.Color.parseColor("#F25C5C")
                    PingMonitor.PingQuality.UNKNOWN -> resources.getColor(R.color.accent, theme)
                }
                txtPing.setTextColor(color)

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        pingTracker.stop()
        holdTimer?.cancel()
    }
}

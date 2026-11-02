package com.bossxor.seoul2033help.ui.overlay

import android.app.Activity
import android.app.Notification
import android.media.projection.MediaProjectionManager
import android.view.View
import androidx.core.content.IntentCompat
import com.bossxor.seoul2033help.Seoul2033HelpApp
import com.bossxor.seoul2033help.domain.EncounterMatcher
import com.bossxor.seoul2033help.domain.MatchOutcome
import com.bossxor.seoul2033help.ocr.OcrCropper
import com.bossxor.seoul2033help.ocr.ScreenCapturer
import com.bossxor.seoul2033help.ocr.ScreenOcrEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bossxor.seoul2033help.MainActivity
import com.bossxor.seoul2033help.ai.AiDecoder
import com.bossxor.seoul2033help.data.SettingsRepository
import com.bossxor.seoul2033help.data.UserSettings
import com.bossxor.seoul2033help.ui.theme.Seoul2033Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val ssc = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = ssc.savedStateRegistry

    private lateinit var wm: WindowManager
    private var view: ComposeView? = null
    private val state = MutableStateFlow<OverlayState>(OverlayState.Idle)
    private lateinit var settingsRepo: SettingsRepository
    private var capturer: ScreenCapturer? = null
    private val ocr = ScreenOcrEngine()
    private var busy = false
    private var autoJob: Job? = null
    private var autoCollectorStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ssc.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
        settingsRepo = SettingsRepository(applicationContext)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        registry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (capturer != null) return START_NOT_STICKY
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val data = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_DATA, Intent::class.java) }
        startAsForeground()
        if (code != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val mp = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data)
        capturer = ScreenCapturer(applicationContext, mp)
        showPanel()
        startAutoCollector()
        return START_NOT_STICKY
    }

    private fun startAutoCollector() {
        if (autoCollectorStarted) return
        autoCollectorStarted = true
        lifecycleScope.launch {
            settingsRepo.settings.collect { rescheduleAuto(it) }
        }
    }

    private fun rescheduleAuto(s: UserSettings) {
        autoJob?.cancel()
        if (!s.autoOcr) return
        autoJob = lifecycleScope.launch {
            while (isActive) {
                delay(s.autoOcrIntervalSec * 1000L)
                recognize()
            }
        }
    }

    /** Manual 인식: 대기 중인 auto tick 취소 후 즉시 실행, 이후 간격 재시작. */
    private fun recognizeManual() {
        autoJob?.cancel()
        recognize()
        lifecycleScope.launch { rescheduleAuto(settingsRepo.settings.first()) }
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "오버레이", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Seoul 2033 Help 실행 중").build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else startForeground(1, n)
    }

    private fun recognize() {
        val cap = capturer ?: return
        if (busy) return
        busy = true
        state.value = OverlayState.Idle
        lifecycleScope.launch {
            try {
                // hide panel so it isn't captured
                view?.visibility = View.INVISIBLE
                delay(200)
                val full = cap.grab()
                view?.visibility = View.VISIBLE
                if (full == null) {
                    state.value = OverlayState.OcrError("인식 실패: 화면을 캡처하지 못했어요")
                    return@launch
                }
                val frac = settingsRepo.settings.first().cropBottomFraction
                val crop = OcrCropper.cropBottom(full, frac)
                val text = ocr.recognize(crop)
                crop.recycle(); full.recycle()
                state.value = if (text.isBlank()) {
                    OverlayState.OcrError("인식 실패: 글자를 읽지 못했어요. ‘검색’을 이용해 보세요")
                } else when (val o = EncounterMatcher((application as Seoul2033HelpApp).store.all()).match(text)) {
                    is MatchOutcome.Clear -> OverlayState.Matched(o.node, o.score)
                    is MatchOutcome.Ambiguous -> OverlayState.Ambiguous(o.candidates)
                    MatchOutcome.Miss -> OverlayState.Miss(text)
                }
            } finally {
                view?.visibility = View.VISIBLE
                busy = false
            }
        }
    }

    private fun aiDecode() {
        val miss = state.value as? OverlayState.Miss ?: return
        if (busy) return
        busy = true
        state.value = OverlayState.Decoding
        lifecycleScope.launch {
            try {
                val s = settingsRepo.settings.first()
                if (!s.aiEnabled) {
                    state.value = miss
                    Toast.makeText(this@OverlayService, "API 키가 없어요", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val result = withContext(Dispatchers.IO) {
                    AiDecoder(s.aiApiKey, s.aiBaseUrl, s.aiModel).decodeOcr(miss.ocrText)
                }
                result.fold(
                    onSuccess = { node ->
                        val saved = (application as Seoul2033HelpApp).store.appendCache(node)
                        state.value = OverlayState.Matched(saved, 1.0)
                    },
                    onFailure = { e ->
                        state.value = miss
                        Toast.makeText(
                            this@OverlayService,
                            "AI 실패: ${e.message?.take(80) ?: "오류"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } finally {
                busy = false
            }
        }
    }

    private fun showPanel() {
        val actions = OverlayActions(
            onRecognize = ::recognizeManual,
            onSearch = {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            onToggleRecommend = { lifecycleScope.launch { settingsRepo.update { it.copy(showRecommend = !it.showRecommend) } } },
            onAlpha = { v -> lifecycleScope.launch { settingsRepo.update { it.copy(overlayAlpha = v) } } },
            onQuit = ::quitApp,
            onPick = { n -> state.value = OverlayState.Matched(n, 1.0) },
            onAiDecode = ::aiDecode
        )
        val cv = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                Seoul2033Theme {
                    val s by settingsRepo.settings.collectAsState(initial = UserSettings())
                    val st by state.collectAsState()
                    OverlayPanel(st, s, actions)
                }
            }
        }
        val w = (resources.displayMetrics.density * 340).toInt()
        val lp = WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; y = 80 }
        wm.addView(cv, lp)
        view = cv
    }

    fun quitApp() {
        autoJob?.cancel()
        removePanel()
        capturer?.release(); capturer = null
        ocr.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Process.killProcess(Process.myPid())
    }

    private fun removePanel() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }

    override fun onDestroy() {
        autoJob?.cancel()
        removePanel()
        capturer?.release(); capturer = null
        registry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    companion object {
        private const val CH = "overlay"
        private const val EXTRA_CODE = "code"
        private const val EXTRA_DATA = "data"
        /** [data] = result Intent from MediaProjectionManager.createScreenCaptureIntent(). */
        fun start(ctx: Context, resultCode: Int, data: Intent) =
            ctx.startForegroundService(
                Intent(ctx, OverlayService::class.java).putExtra(EXTRA_CODE, resultCode).putExtra(EXTRA_DATA, data)
            )
    }
}

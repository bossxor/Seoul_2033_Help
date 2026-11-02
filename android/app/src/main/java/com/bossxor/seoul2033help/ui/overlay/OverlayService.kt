package com.bossxor.seoul2033help.ui.overlay

import android.app.Notification
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
import com.bossxor.seoul2033help.data.SettingsRepository
import com.bossxor.seoul2033help.data.UserSettings
import com.bossxor.seoul2033help.ui.theme.Seoul2033Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val ssc = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = ssc.savedStateRegistry

    private lateinit var wm: WindowManager
    private var view: ComposeView? = null
    private val state = MutableStateFlow<OverlayState>(OverlayState.Idle)
    private lateinit var settingsRepo: SettingsRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ssc.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
        settingsRepo = SettingsRepository(applicationContext)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
        showPanel()
        registry.currentState = Lifecycle.State.RESUMED
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "오버레이", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Seoul 2033 Help 실행 중").build()
        if (Build.VERSION.SDK_INT >= 34) {
            // Task 8: switch to mediaProjection type once projection token exists.
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(1, n)
    }

    private fun showPanel() {
        val actions = OverlayActions(
            onRecognize = { state.value = OverlayState.OcrError("OCR 준비 중 (Task 8)") },
            onSearch = {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            onToggleRecommend = { lifecycleScope.launch { settingsRepo.update { it.copy(showRecommend = !it.showRecommend) } } },
            onAlpha = { v -> lifecycleScope.launch { settingsRepo.update { it.copy(overlayAlpha = v) } } },
            onQuit = ::quitApp,
            onPick = { n -> state.value = OverlayState.Matched(n, 1.0) }
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
        removePanel()
        // Task 8: stop MediaProjection here first.
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Process.killProcess(Process.myPid())
    }

    private fun removePanel() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }

    override fun onDestroy() {
        removePanel()
        registry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    companion object {
        private const val CH = "overlay"
        fun start(ctx: Context) =
            ctx.startForegroundService(Intent(ctx, OverlayService::class.java))
    }
}

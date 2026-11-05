package com.bossxor.seoul2033help.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bossxor.seoul2033help.Seoul2033HelpApp
import com.bossxor.seoul2033help.data.EncounterStore
import com.bossxor.seoul2033help.data.SettingsRepository
import com.bossxor.seoul2033help.ui.detail.NodeDetailScreen
import com.bossxor.seoul2033help.ui.overlay.OverlayService
import com.bossxor.seoul2033help.ui.search.SearchScreen
import com.bossxor.seoul2033help.ui.settings.SettingsScreen

@Composable
fun AppNav(store: EncounterStore) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val settingsRepo = remember { SettingsRepository(ctx.applicationContext) }
    val projLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val d = r.data
        if (r.resultCode == Activity.RESULT_OK && d != null) OverlayService.start(ctx, r.resultCode, d)
        else Toast.makeText(ctx, "권한 필요: 화면 캡처를 허용해 주세요", Toast.LENGTH_SHORT).show()
    }
    val startOverlay = {
        if (Settings.canDrawOverlays(ctx)) projLauncher.launch(
            ctx.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()
        )
        else ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
        )
    }
    NavHost(nav, startDestination = "search") {
        composable("search") {
            SearchScreen(
                store, settingsRepo,
                onSettings = { nav.navigate("settings") },
                onStartOverlay = startOverlay
            ) {
                nav.navigate("detail/${Uri.encode(it)}")
            }
        }
        composable(
            "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val node = entry.arguments?.getString("id")?.let(store::get)
            if (node != null) NodeDetailScreen(
                node,
                onOpen = { nav.navigate("detail/${Uri.encode(it)}") }
            ) { nav.popBackStack() }
            else Text("not found")
        }
        composable("settings") {
            val app = ctx.applicationContext as Seoul2033HelpApp
            SettingsScreen(
                repo = settingsRepo,
                onBack = { nav.popBackStack() },
                onExportCache = {
                    val json = app.store.exportCacheJson()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_SUBJECT, "local_encounters.json")
                        putExtra(Intent.EXTRA_TEXT, json)
                    }
                    ctx.startActivity(Intent.createChooser(send, "AI 캐시 export"))
                }
            )
        }
    }
}

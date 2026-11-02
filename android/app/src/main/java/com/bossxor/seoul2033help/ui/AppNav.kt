package com.bossxor.seoul2033help.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.Settings
import com.bossxor.seoul2033help.ui.overlay.OverlayService
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bossxor.seoul2033help.data.SettingsRepository
import com.bossxor.seoul2033help.ui.settings.SettingsScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bossxor.seoul2033help.data.EncounterRepository
import com.bossxor.seoul2033help.ui.detail.NodeDetailScreen
import com.bossxor.seoul2033help.ui.search.SearchScreen

@Composable
fun AppNav(repo: EncounterRepository) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "search") {
        composable("search") {
            SearchScreen(repo, onSettings = { nav.navigate("settings") }) {
                nav.navigate("detail/${android.net.Uri.encode(it)}")
            }
        }
        composable(
            "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val node = entry.arguments?.getString("id")?.let(repo::get)
            if (node != null) NodeDetailScreen(node) { nav.popBackStack() }
            else Text("not found")
        }
        composable("settings") {
            val activity = LocalContext.current
            val ctx = activity.applicationContext
            val settingsRepo = remember { SettingsRepository(ctx) }
            val projLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
                val d = r.data
                if (r.resultCode == Activity.RESULT_OK && d != null) OverlayService.start(ctx, r.resultCode, d)
                else Toast.makeText(ctx, "권한 필요: 화면 캡처를 허용해 주세요", Toast.LENGTH_SHORT).show()
            }
            SettingsScreen(settingsRepo, onBack = { nav.popBackStack() }, onStartOverlay = {
                if (Settings.canDrawOverlays(ctx)) projLauncher.launch(
                    ctx.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()
                )
                else activity.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                )
            })
        }
    }
}
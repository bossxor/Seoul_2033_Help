package com.bossxor.seoul2033help.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            SearchScreen(repo) { nav.navigate("detail/${android.net.Uri.encode(it)}") }
        }
        composable(
            "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val node = entry.arguments?.getString("id")?.let(repo::get)
            if (node != null) NodeDetailScreen(node) { nav.popBackStack() }
            else Text("not found")
        }
        composable("settings") { Text("설정") }
    }
}
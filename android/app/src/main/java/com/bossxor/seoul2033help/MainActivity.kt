package com.bossxor.seoul2033help

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bossxor.seoul2033help.ui.AppNav
import com.bossxor.seoul2033help.ui.theme.Seoul2033Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as Seoul2033HelpApp).repository
        setContent { Seoul2033Theme { AppNav(repo) } }
    }
}
package com.zachm.employeelogin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.zachm.employeelogin.ui.screens.HomeScreen

class HomeActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        viewModel.login.observe(this) { onLoginSelected(it) }

        setContent {
            HomeScreen()
        }
    }

    private fun onLoginSelected(login: Boolean) {
        if(login) {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }
}
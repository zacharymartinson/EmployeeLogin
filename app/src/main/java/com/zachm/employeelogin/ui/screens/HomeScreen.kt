package com.zachm.employeelogin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zachm.employeelogin.HomeViewModel
import com.zachm.employeelogin.R
import com.zachm.employeelogin.ui.theme.HomeBackground

@Composable
fun HomeScreen() {

    val viewModel: HomeViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {

        Spacer(modifier = Modifier.weight(1.3f))

        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
        ) {

            Text(
                text = "Employee Login",
                color = Color.White.copy(alpha=0.7f),
                fontSize = 25.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )

            Icon(
                painter = painterResource(id = R.drawable.account_circle),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(325.dp)
                    .clickable {viewModel.login.value = true}
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun display() {
    HomeScreen()
}
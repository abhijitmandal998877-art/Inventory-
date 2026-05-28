package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.BillingAppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BillingViewModel

class MainActivity : ComponentActivity() {
    private val billingViewModel: BillingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BillingAppNavigation(viewModel = billingViewModel)
            }
        }
    }
}

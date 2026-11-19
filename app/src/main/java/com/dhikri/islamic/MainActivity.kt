package com.dhikri.islamic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dhikri.islamic.ui.navigation.DhikriAppShell
import com.dhikri.islamic.ui.theme.DhikriTheme
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DhikriApplication
        val factory = DhikriViewModel.Factory(app.repository, app.settingsRepository)

        setContent {
            val vm: DhikriViewModel = viewModel(factory = factory)
            val settings by vm.settings.collectAsState()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DhikriTheme(darkTheme = settings.darkMode) {
                    DhikriAppShell(viewModel = vm)
                }
            }
        }
    }
}

package com.dhikri.islamic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dhikri.islamic.data.update.UpdateCheckWorker
import com.dhikri.islamic.ui.navigation.DhikriAppShell
import com.dhikri.islamic.ui.theme.DhikriTheme
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

class MainActivity : ComponentActivity() {

    private val autoUpdateRequested = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getBooleanExtra(UpdateCheckWorker.EXTRA_AUTO_UPDATE, false) == true) {
            autoUpdateRequested.value = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val app = application as DhikriApplication
        val factory = DhikriViewModel.Factory(app.repository, app.settingsRepository)

        setContent {
            val vm: DhikriViewModel = viewModel(factory = factory)
            val settings by vm.settings.collectAsState()
            val shouldAutoUpdate by autoUpdateRequested

            LaunchedEffect(Unit) {
                vm.checkForAppUpdateWithNotify(this@MainActivity)
            }

            LaunchedEffect(shouldAutoUpdate) {
                if (shouldAutoUpdate) {
                    autoUpdateRequested.value = false
                    vm.triggerAutoUpdateFromNotification(this@MainActivity)
                }
            }

            LaunchedEffect(settings.darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    navigationBarStyle = if (settings.darkMode) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DhikriTheme(darkTheme = settings.darkMode) {
                    DhikriAppShell(viewModel = vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(UpdateCheckWorker.EXTRA_AUTO_UPDATE, false)) {
            autoUpdateRequested.value = true
        }
    }
}

package net.softavis.healthhub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.softavis.healthhub.ui.HealthHubApp
import net.softavis.healthhub.ui.HealthHubViewModel
import net.softavis.healthhub.ui.theme.HealthHubTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            // No additional UI action is required.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            HealthHubTheme {
                val viewModel: HealthHubViewModel =
                    viewModel()

                HealthHubApp(
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /*
         * Health Connect permissions are refreshed by the Compose UI
         * after returning from the permissions screen.
         */
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }
}
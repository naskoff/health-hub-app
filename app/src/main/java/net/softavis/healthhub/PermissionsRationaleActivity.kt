package net.softavis.healthhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.softavis.healthhub.ui.theme.HealthHubTheme

class PermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HealthHubTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Health Connect access",
                        style =
                            MaterialTheme.typography.headlineMedium,
                    )

                    Text(
                        text = """
                            Health Hub reads your health and fitness data so it can synchronize it with your private Health Hub account.

                            The application does not modify or delete Health Connect data.
                        """.trimIndent(),
                    )
                }
            }
        }
    }
}
package com.quantum_prof.phantalandwaittimes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quantum_prof.phantalandwaittimes.notification.NotificationService
import com.quantum_prof.phantalandwaittimes.ui.WaitTimeScreen
import com.quantum_prof.phantalandwaittimes.ui.theme.PhantasialandWaitTimesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Attraction the user arrived at through a notification, consumed once the list has scrolled
     * to it. Held as Compose state so a tap while the app is already open re-triggers the scroll.
     */
    private var deepLinkAttractionCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draws behind the system bars and picks the right light/dark bar icons, replacing the
        // deprecated Window#statusBarColor handling the theme used to do in a SideEffect.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        deepLinkAttractionCode = intent.attractionCode()

        setContent {
            PhantasialandWaitTimesTheme {
                WaitTimeScreen(
                    deepLinkAttractionCode = deepLinkAttractionCode,
                    onDeepLinkHandled = { deepLinkAttractionCode = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // launchMode is singleTop, so tapping a notification while the app is open lands here
        // rather than creating a second activity.
        setIntent(intent)
        deepLinkAttractionCode = intent.attractionCode()
    }

    private fun Intent.attractionCode(): String? =
        getStringExtra(NotificationService.EXTRA_ATTRACTION_CODE)?.takeIf { it.isNotBlank() }
}

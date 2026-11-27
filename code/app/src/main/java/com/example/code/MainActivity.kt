package com.example.code

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import com.example.code.classes.AppState
import com.example.code.ui.theme.CodeTheme

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    companion object {
        var isTestMode = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!isTestMode) {
            setContent {
                CodeTheme {
                    val appState = remember { AppState() }
                    ForegroundLotteryDrawEffect(appState, lifecycle)
                    LotteryNotificationEffect(appState)
                    ReservationHistoryEffect(appState)
                    CampsiteNavController(appState)
                }
            }
        }
    }
}

package com.sunodo.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sunodo.app.ui.SunoDoScreen
import com.sunodo.app.ui.theme.SunoDoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SunoDoTheme {
                SunoDoScreen(
                    onPacketAction = {
                        Toast.makeText(
                            this,
                            "Real Calendar / Reminder / Reply intents arrive in Stage 4 (OSActionBridge)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

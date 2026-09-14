package com.sunodo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sunodo.app.actions.PacketActionHandler
import com.sunodo.app.ui.SunoDoScreen
import com.sunodo.app.ui.theme.SunoDoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SunoDoTheme {
                SunoDoScreen(
                    onPacketAction = { packet -> PacketActionHandler.handle(this, packet) }
                )
            }
        }
    }
}

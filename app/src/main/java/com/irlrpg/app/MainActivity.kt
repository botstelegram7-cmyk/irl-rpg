package com.irlrpg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.irlrpg.app.ui.IrlRpgRoot
import com.irlrpg.app.ui.theme.IrlRpgTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            IrlRpgTheme {
                IrlRpgRoot()
            }
        }
    }
}

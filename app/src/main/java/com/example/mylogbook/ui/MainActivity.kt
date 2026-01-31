package com.example.mylogbook.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mylogbook.LogbookApp as LogbookApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        setContent { LogbookApp() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIncomingIntent(intent)
        }
    }

    private fun handleIncomingIntent(intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? android.net.Uri
            if (uri != null) {
                (application as LogbookApplication).setSharedImport(uri)
                intent.removeExtra(Intent.EXTRA_STREAM)
            }
        } else if (action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                (application as LogbookApplication).setSharedImport(uri)
                intent.data = null
            }
        }
    }
}

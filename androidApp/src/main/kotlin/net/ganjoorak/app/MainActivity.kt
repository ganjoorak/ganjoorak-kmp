package net.ganjoorak.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.ganjoorak.app.daily.poemRefOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handlePoemIntent(intent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePoemIntent(intent)
    }

    /** A widget / notification tap carries the poem to open; the shell consumes it once. */
    private fun handlePoemIntent(intent: Intent?) {
        intent?.poemRefOrNull()?.let { PendingDeepLink.open(it) }
    }
}

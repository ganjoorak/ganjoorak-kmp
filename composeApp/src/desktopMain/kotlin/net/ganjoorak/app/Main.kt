package net.ganjoorak.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.ganjoorak.app.resources.Res
import net.ganjoorak.app.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() {
    val override = System.getProperty("ganjoorak.accountApi")?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv("GANJOORAK_ACCOUNT_API")?.trim()?.takeIf { it.isNotEmpty() }
    if (override != null) AppGraph.accountApiBaseUrl = override.trimEnd('/')
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "گنجورک",
            icon = painterResource(Res.drawable.app_icon),
            state = rememberWindowState(width = 1100.dp, height = 820.dp),
        ) {
            App()
        }
    }
}

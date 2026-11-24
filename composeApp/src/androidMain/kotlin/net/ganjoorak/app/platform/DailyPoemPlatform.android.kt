package net.ganjoorak.app.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.daily.DailyPoemScheduler
import net.ganjoorak.app.daily.updateDailyPoemWidgets

actual fun refreshDailyPoemWidget() {
    runCatching { updateDailyPoemWidgets(AndroidPlatform.context) }
}

actual fun rescheduleDailyPoemWork() {
    runCatching {
        DailyPoemScheduler.enqueueNext(
            AndroidPlatform.context,
            AppGraph.instance.settings.settings.value,
            ExistingWorkPolicy.REPLACE,
        )
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

package corsica.universita.voxwave.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class PermissionState {
    NotAsked,
    Granted,
    DeniedOnce,
    DeniedForever,
}

/**
 * TD A7 (slide 101) : PermissionFlow.
 * Gère le cycle complet d'une permission runtime jusqu'au refus définitif.
 * Permet d'expliquer pourquoi la permission est requise (rationale)
 * et de renvoyer proprement vers les Réglages Android en cas de refus définitif.
 */
class PermissionFlow(
    private val activity: Activity,
    private val permissions: List<String>,
    private val requireAll: Boolean = true,
    private val askedBefore: () -> Boolean,
    private val setAsked: () -> Unit,
) {
    var state by mutableStateOf(compute(afterResult = false))
        private set

    private var launcher: ActivityResultLauncher<Array<String>>? = null

    fun attach(launcher: ActivityResultLauncher<Array<String>>) {
        this.launcher = launcher
    }

    fun request() {
        setAsked()
        launcher?.launch(permissions.toTypedArray())
    }

    fun onResult(result: Map<String, Boolean>) {
        state = compute(afterResult = true)
    }

    fun refresh() {
        state = compute(afterResult = false)
    }

    fun openSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null),
        )
        activity.startActivity(intent)
    }

    private fun granted(): Boolean {
        return if (requireAll) {
            permissions.all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
        } else {
            permissions.any { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
        }
    }

    private fun rationale(): Boolean {
        return permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }

    private fun compute(afterResult: Boolean): PermissionState = when {
        granted() -> PermissionState.Granted
        rationale() -> PermissionState.DeniedOnce
        askedBefore() || afterResult -> PermissionState.DeniedForever
        else -> PermissionState.NotAsked
    }
}

@Composable
fun rememberPermissionFlow(
    vararg permissions: String,
    requireAll: Boolean = true,
): PermissionFlow {
    val context = LocalContext.current
    val activity = context as? Activity ?: error("rememberPermissionFlow doit être appelé dans une Activity")
    var asked by rememberSaveable { mutableStateOf(false) }

    val flow = remember {
        PermissionFlow(
            activity = activity,
            permissions = permissions.toList(),
            requireAll = requireAll,
            askedBefore = { asked },
            setAsked = { asked = true },
        )
    }

    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { result ->
        flow.onResult(result)
    }
    flow.attach(launcher)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                flow.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return flow
}

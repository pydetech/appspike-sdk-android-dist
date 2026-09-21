package dev.appspike.sample

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import dev.appspike.AppSpike
import dev.appspike.InitResult
import dev.appspike.remoteconfig.AppSpikeRemoteConfig

/**
 * The sample's in-app defaults. They serve until a fetched template is activated, and as a
 * fallback for keys the template does not define.
 *
 * Registered at startup *and* re-applied by Reset, so the all-values screen after a reset
 * shows this set with `(default)` sources rather than an empty list.
 */
fun applySampleDefaults() {
    AppSpikeRemoteConfig.setDefaults(
        mapOf(
            "welcome_message" to "Hello from defaults",
            "feature_enabled" to false,
            "max_retries" to 3L,
            "price_multiplier" to 1.0,
            // Platform-specific extra: Android/KMP accept ByteArray defaults, which the SDK
            // preserves byte for byte and hands back via getValue().asByteArray().
            "binary_payload" to byteArrayOf(0x41, 0x53, 0x01, 0x02),
        ),
    )
}

/**
 * Setup state the UI renders, so an unreplaced API key or a failed initialize is visible on
 * screen instead of only in logcat.
 */
object SampleSetup {
    val status = mutableStateOf("Initializing…")
}

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Defaults go in before initialize so the first frame already has values.
        applySampleDefaults()

        if (APPSPIKE_API_KEY == API_KEY_PLACEHOLDER) {
            val message = "Not initialized — set your API key in $API_KEY_LOCATION"
            SampleSetup.status.value = message
            Log.w("AppSpike", message)
            return
        }

        AppSpike.initialize(
            context = this,
            apiKey = APPSPIKE_API_KEY,
            modules = listOf(AppSpikeRemoteConfig),
        ) { result ->
            when (result) {
                is InitResult.Success -> {
                    SampleSetup.status.value = "Initialized"
                    Log.d("AppSpike", "Initialized")
                }

                is InitResult.Error -> {
                    SampleSetup.status.value = "Init failed: ${result.message}"
                    Log.e("AppSpike", "Init failed: ${result.message}")
                }
            }
        }
    }
}

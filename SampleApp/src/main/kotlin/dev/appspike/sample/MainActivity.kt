package dev.appspike.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.appspike.remoteconfig.AppSpikeRemoteConfig
import dev.appspike.remoteconfig.CustomSignals
import dev.appspike.remoteconfig.RemoteConfigSettings
import dev.appspike.remoteconfig.RemoteConfigThrottledException
import dev.appspike.remoteconfig.ValueSource
import kotlinx.coroutines.launch

/**
 * Source labels are rendered lowercase in parentheses — `(remote)`, `(default)`, `(static)` —
 * rather than the raw enum name, so every AppSpike sample reads the same way.
 */
private fun sourceLabel(source: ValueSource): String = "(${source.name.lowercase()})"

/** Reset clears SDK state and then re-applies the sample defaults (see [applySampleDefaults]). */
private fun resetToSampleDefaults() {
    AppSpikeRemoteConfig.reset()
    applySampleDefaults()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold { padding ->
                    var showAllValues by remember { mutableStateOf(false) }
                    if (showAllValues) {
                        AllValuesScreen(
                            onBack = { showAllValues = false },
                            modifier = Modifier.padding(padding),
                        )
                    } else {
                        SampleScreen(
                            onShowAllValues = { showAllValues = true },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleScreen(
    onShowAllValues: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var fetchStatus by remember { mutableStateOf("") }
    var info by remember { mutableStateOf(AppSpikeRemoteConfig.getInfo()) }
    val setupStatus by SampleSetup.status

    var stringKey by remember { mutableStateOf("") }
    var booleanKey by remember { mutableStateOf("") }
    var longKey by remember { mutableStateOf("") }
    var doubleKey by remember { mutableStateOf("") }
    var inspectKey by remember { mutableStateOf("") }

    var intervalText by remember { mutableStateOf(info.configSettings.minimumFetchIntervalSeconds.toString()) }
    var timeoutText by remember { mutableStateOf(info.configSettings.fetchTimeoutSeconds.toString()) }
    var signalKey by remember { mutableStateOf("") }
    var signalValue by remember { mutableStateOf("") }
    var signalsStatus by remember { mutableStateOf("") }

    // Waits until the persisted config (defaults + last activated values) is loaded,
    // then reports the real fetch state instead of the pre-warm-up placeholder.
    LaunchedEffect(Unit) {
        info = AppSpikeRemoteConfig.ensureInitialized()
        intervalText = info.configSettings.minimumFetchIntervalSeconds.toString()
        timeoutText = info.configSettings.fetchTimeoutSeconds.toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Keeps the focused field above the keyboard (with adjustResize in the manifest).
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("AppSpike Sample", style = MaterialTheme.typography.headlineMedium)

        // Setup: the API key is a build-time constant, so an unset key has to be reported
        // here — there is deliberately no key-entry field.
        Text(
            "Setup: $setupStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                fetchStatus = "Fetching & activating..."
                scope.launch {
                    try {
                        val changed = AppSpikeRemoteConfig.fetchAndActivate()
                        fetchStatus = if (changed) "Fetched from remote" else "Using cached data"
                    } catch (exception: Exception) {
                        fetchStatus = "Failed: ${exception.message}"
                    }
                    info = AppSpikeRemoteConfig.getInfo()
                }
            }) { Text("Fetch & Activate") }

            Button(onClick = {
                fetchStatus = "Fetching (respects cache)..."
                scope.launch {
                    try {
                        // No interval argument: the configured minimum fetch interval applies,
                        // so this often completes from cache without a server hit.
                        AppSpikeRemoteConfig.fetch()
                        fetchStatus = "Fetch done — call Activate to apply"
                    } catch (throttled: RemoteConfigThrottledException) {
                        fetchStatus = "Throttled: ${throttled.message}"
                    } catch (exception: Exception) {
                        fetchStatus = "Failed: ${exception.message}"
                    }
                    info = AppSpikeRemoteConfig.getInfo()
                }
            }) { Text("Fetch") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                fetchStatus = "Fetching (cache bypassed)..."
                scope.launch {
                    try {
                        // Interval 0 ignores the minimum fetch interval: always hits the server.
                        AppSpikeRemoteConfig.fetch(minimumFetchIntervalSeconds = 0)
                        val changed = AppSpikeRemoteConfig.activate()
                        fetchStatus = if (changed) "Fresh config activated" else "No changes to activate"
                    } catch (exception: Exception) {
                        fetchStatus = "Failed: ${exception.message}"
                    }
                    info = AppSpikeRemoteConfig.getInfo()
                }
            }) { Text("Bypass Cache & Activate") }

            OutlinedButton(onClick = {
                scope.launch {
                    val changed = AppSpikeRemoteConfig.activate()
                    fetchStatus = if (changed) "Activated fetched config" else "Nothing new to activate"
                }
            }) { Text("Activate") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onShowAllValues) { Text("Show All Values") }
            OutlinedButton(onClick = {
                resetToSampleDefaults()
                info = AppSpikeRemoteConfig.getInfo()
                intervalText = info.configSettings.minimumFetchIntervalSeconds.toString()
                timeoutText = info.configSettings.fetchTimeoutSeconds.toString()
                fetchStatus = "Reset — sample defaults re-applied"
            }) { Text("Reset") }
        }

        if (fetchStatus.isNotEmpty()) {
            Text(fetchStatus, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "getInfo: ${info.lastFetchStatus}, lastFetchTimeMillis=${info.lastFetchTimeMillis}",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()
        Text("Config settings", style = MaterialTheme.typography.titleMedium)
        Text(
            "Current: minimum fetch interval = " +
                "${info.configSettings.minimumFetchIntervalSeconds}s, fetch timeout = " +
                "${info.configSettings.fetchTimeoutSeconds}s",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = intervalText,
                onValueChange = { intervalText = it },
                label = { Text("Min interval (s)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = timeoutText,
                onValueChange = { timeoutText = it },
                label = { Text("Timeout (s)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val interval = intervalText.toLongOrNull()
                val timeout = timeoutText.toLongOrNull()
                if (interval != null && timeout != null) {
                    AppSpikeRemoteConfig.setConfigSettings(
                        RemoteConfigSettings(
                            minimumFetchIntervalSeconds = interval,
                            fetchTimeoutSeconds = timeout,
                        ),
                    )
                    info = AppSpikeRemoteConfig.getInfo()
                    fetchStatus = "Settings applied"
                } else {
                    fetchStatus = "Settings must be numbers"
                }
            }) { Text("Apply Settings") }

            OutlinedButton(onClick = {
                // The no-argument constructor carries the SDK defaults (43200s / 60s).
                AppSpikeRemoteConfig.setConfigSettings(RemoteConfigSettings())
                info = AppSpikeRemoteConfig.getInfo()
                intervalText = info.configSettings.minimumFetchIntervalSeconds.toString()
                timeoutText = info.configSettings.fetchTimeoutSeconds.toString()
                fetchStatus = "Settings restored to defaults"
            }) { Text("Restore Defaults") }
        }

        HorizontalDivider()
        Text("Custom signals", style = MaterialTheme.typography.titleMedium)
        Text(
            "Targeting attributes for custom_signal conditions, evaluated on-device and " +
                "never transmitted.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = signalKey,
                onValueChange = { signalKey = it },
                label = { Text("Signal key") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = signalValue,
                onValueChange = { signalValue = it },
                label = { Text("Signal value") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val key = signalKey.trim()
                if (key.isEmpty()) {
                    signalsStatus = "Enter a signal key"
                } else {
                    signalsStatus = "Applying $key..."
                    scope.launch {
                        try {
                            AppSpikeRemoteConfig.setCustomSignals(
                                CustomSignals.Builder().put(key, signalValue).build(),
                            )
                            // Signals change targeting, so re-evaluate straight away:
                            // bypass the cache, then activate what came back.
                            AppSpikeRemoteConfig.fetch(minimumFetchIntervalSeconds = 0)
                            AppSpikeRemoteConfig.activate()
                            signalsStatus = "Applied $key=$signalValue, fetched and activated"
                        } catch (exception: Exception) {
                            signalsStatus = "Failed: ${exception.message}"
                        }
                        info = AppSpikeRemoteConfig.getInfo()
                    }
                }
            }) { Text("Apply Signal") }

            OutlinedButton(onClick = {
                val key = signalKey.trim()
                if (key.isEmpty()) {
                    signalsStatus = "Enter a signal key"
                } else {
                    scope.launch {
                        try {
                            // A null value removes the signal.
                            AppSpikeRemoteConfig.setCustomSignals(
                                CustomSignals.Builder().put(key, null as String?).build(),
                            )
                            signalsStatus = "Removed '$key' signal"
                        } catch (exception: Exception) {
                            signalsStatus = "Failed: ${exception.message}"
                        }
                    }
                }
            }) { Text("Remove Signal") }
        }
        // Convenience preset, in addition to the free-form entry above: the builder accepts
        // String, Long, Int, and Double values.
        OutlinedButton(onClick = {
            scope.launch {
                try {
                    AppSpikeRemoteConfig.setCustomSignals(
                        CustomSignals.Builder()
                            .put("tier", "gold")
                            .put("session_count", 12L)
                            .put("level", 7)
                            .put("spend", 9.99)
                            .build(),
                    )
                    signalsStatus = "Preset set: tier, session_count, level, spend"
                } catch (exception: Exception) {
                    signalsStatus = "Failed: ${exception.message}"
                }
            }
        }) { Text("Preset: tier=gold") }
        if (signalsStatus.isNotEmpty()) {
            Text(signalsStatus, style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()
        Text("Values", style = MaterialTheme.typography.titleMedium)
        Text(
            "Typed getters fall back per accessor: an activated value that does not convert " +
                "falls through to the setDefaults value, then the static zero.",
            style = MaterialTheme.typography.bodySmall,
        )

        ValueRow("getString", stringKey, { stringKey = it }) {
            AppSpikeRemoteConfig.getString(it)
        }
        ValueRow("getBoolean", booleanKey, { booleanKey = it }) {
            AppSpikeRemoteConfig.getBoolean(it).toString()
        }
        ValueRow("getLong", longKey, { longKey = it }) {
            AppSpikeRemoteConfig.getLong(it).toString()
        }
        ValueRow("getDouble", doubleKey, { doubleKey = it }) {
            AppSpikeRemoteConfig.getDouble(it).toString()
        }

        HorizontalDivider()
        Text("Value inspector (getValue)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = inspectKey,
            onValueChange = { inspectKey = it },
            label = { Text("Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (inspectKey.isNotEmpty()) {
            val value = AppSpikeRemoteConfig.getValue(inspectKey)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("asString = ${value.asString()}", style = MaterialTheme.typography.bodySmall)
                Text("asBoolean = ${value.asBoolean()}", style = MaterialTheme.typography.bodySmall)
                Text("asLong = ${value.asLong()}", style = MaterialTheme.typography.bodySmall)
                Text("asDouble = ${value.asDouble()}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "asByteArray = ${value.asByteArray().size} bytes",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "getSource = ${sourceLabel(value.getSource())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AllValuesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var prefix by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var resetStatus by remember { mutableStateOf("") }

    val allValues = remember(refresh, prefix) {
        val all = AppSpikeRemoteConfig.getAll().toSortedMap()
        if (prefix.isEmpty()) {
            all
        } else {
            val keys = AppSpikeRemoteConfig.getKeysByPrefix(prefix)
            all.filterKeys { key -> key in keys }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("All Key/Values", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            OutlinedButton(onClick = {
                // Clears activated values, defaults, custom signals, and settings — then puts
                // the sample defaults back, so this screen shows them with (default) sources.
                resetToSampleDefaults()
                resetStatus = "Reset — sample defaults re-applied"
                refresh++
            }) { Text("Reset") }
        }

        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            label = { Text("Filter by key prefix") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (resetStatus.isNotEmpty()) {
            Text(resetStatus, style = MaterialTheme.typography.bodySmall)
        }

        if (allValues.isEmpty()) {
            Text(
                "No values yet — set defaults or fetch and activate first.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        allValues.forEach { (key, value) ->
            Column {
                Text(key, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${value.asString()}  ${sourceLabel(value.getSource())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ValueRow(
    label: String,
    key: String,
    onKeyChange: (String) -> Unit,
    readValue: (String) -> String,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = key,
                onValueChange = onKeyChange,
                label = { Text("Key") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            if (key.isNotEmpty()) {
                Text(
                    readValue(key),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

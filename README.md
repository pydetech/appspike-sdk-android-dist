# AppSpike SDK for Android

**The free Firebase Remote Config alternative.**

> **Firebase Remote Config is going paid.** Google's usage-based pricing took effect on
> September 1, 2026. Existing free-plan (Spark) projects hit enforcement on
> **December 1, 2026**: past 100K daily fetches they get a 30-day grace period and are
> then throttled. Existing Blaze projects are billed automatically from
> **February 1, 2027**. The dates come from
> [Firebase's own pricing schedule](https://firebase.google.com/docs/remote-config/pricing).
> The [migration schedule below](#when-to-migrate) fits inside that window.


Android SDK for [AppSpike Remote Config](https://appspike.dev/remote-config). **Free** remote configuration, feature flags, and staged rollouts, with every condition evaluated **locally on-device**. AppSpike Remote Config is also a drop-in replacement for Firebase Remote Config: same fetch/activate lifecycle, same typed accessors, one-line dependency swap, with no fetch limits and no usage fees.

[Product](https://appspike.dev/remote-config) · [Docs](https://appspike.dev/docs/remote-config)

## Installation

### Gradle

Add the dependency. Artifacts are published on Maven Central:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("dev.appspike:remote-config:1.4.5")
}
```

## Quick Start

**1. Register your app.** Create your app at [console.appspike.dev](https://console.appspike.dev) and copy its `pk_live_…` API key.

**2. Initialize the SDK.** Pass Remote Config as a module.

```kotlin
import dev.appspike.AppSpike
import dev.appspike.InitResult
import dev.appspike.remoteconfig.AppSpikeRemoteConfig
import dev.appspike.remoteconfig.RemoteConfigFetchException

AppSpike.initialize(
    context = applicationContext,
    apiKey = "your-api-key",
    modules = listOf(AppSpikeRemoteConfig),
) { result ->
    when (result) {
        is InitResult.Success -> Log.d("AppSpike", "Ready")
        is InitResult.Error -> Log.e("AppSpike", result.message)
    }
}
```

**3. Set defaults.** These are served until a fetched config is activated.

```kotlin
AppSpikeRemoteConfig.setDefaults(mapOf(
    "welcome_message" to "Hello!",
    "feature_enabled" to false,
    "max_retries" to 3L,
    "price" to 9.99,
))

// Or from XML resources
AppSpikeRemoteConfig.setDefaults(context, R.xml.remote_config_defaults)
```

**4. Fetch and activate.** This is a suspend function, so call it from a coroutine.

```kotlin
lifecycleScope.launch {
    try {
        val changed = AppSpikeRemoteConfig.fetchAndActivate()
        Log.d("AppSpike", "Config changed: $changed")
    } catch (e: RemoteConfigFetchException) {
        // A failed fetch is an ordinary outcome — offline, or a backoff window.
        // Your defaults (or the last activated config) stay in place.
        Log.w("AppSpike", "Fetch skipped: ${e.message}")
    }
}
```

**5. Read values.** Typed accessors, with in-app defaults as the fallback.

```kotlin
val message = AppSpikeRemoteConfig.getString("welcome_message")
val enabled = AppSpikeRemoteConfig.getBoolean("feature_enabled")
val retries = AppSpikeRemoteConfig.getLong("max_retries")
val price = AppSpikeRemoteConfig.getDouble("price")

// Or use RemoteConfigValue for source info
val value = AppSpikeRemoteConfig.getValue("welcome_message")
val source = value.getSource() // REMOTE, DEFAULT, or STATIC
```

**6. Listen for updates.** Get notified when activated keys change.

```kotlin
val registration = AppSpikeRemoteConfig.addOnConfigUpdateListener { configUpdate ->
    Log.d("AppSpike", "Keys changed: ${configUpdate.updatedKeys}")
}
// Later: registration.remove()
```

**7. Custom signals for targeting.** These are evaluated on-device.

```kotlin
lifecycleScope.launch {
    AppSpikeRemoteConfig.setCustomSignals(
        CustomSignals.Builder()
            .put("tier", "gold")
            .put("level", 5)
            .build()
    )
}
```

## Why AppSpike Remote Config?

**A free, direct replacement for Firebase Remote Config.** Same fetch/activate lifecycle, same typed accessors, and no fetch metering or usage fees. Firebase Remote Config is free up to 100K fetches per day, then bills $0.06 per 10K. AppSpike Remote Config stays free at any scale.

**Your targeting data stays on the device.** Firebase Remote Config sends custom signals to Google's servers with every fetch and evaluates conditions there. AppSpike Remote Config downloads the template once and evaluates every condition locally. User tier, level, or any signal you set is never transmitted anywhere. If your privacy policy or DPA review has ever flagged Remote Config, this is the difference that closes the ticket.

**Works offline.** The last activated config keeps serving with no network, and changed signals or crossed time boundaries take effect on the next fetch cycle even offline.

**Migration is mechanical.** The API mirrors Firebase Remote Config's method-for-method. The [migration guide](#migrating-from-firebase-remote-config) below is mostly a find-and-replace, and there's an [AI prompt](#ai-assisted-migration) that does it for you.

**Battle tested.** It already serves millions of users in PokeRaid and PokeTrade.

What Firebase Remote Config still does that we don't: Google Analytics audience targeting (use custom signals instead) and managed A/B experiment dashboards (run A/B tests with percentage conditions). If Analytics audiences are load-bearing for you today, the two SDKs coexist in one app so you can migrate everything else first. Everything else is covered:

## Migrating from Firebase Remote Config

The API is designed to be a drop-in replacement. Most changes are dependency and initialization. The value access API maps directly.

### Feature comparison

| Feature | Firebase Remote Config | AppSpike Remote Config |
|---------|----------------------|----------------------|
| Fetch & activate lifecycle | ✅ | ✅ |
| Typed value access (string, bool, long, double, byte array) | ✅ | ✅ |
| In-app defaults (Map) | ✅ | ✅ |
| XML resource defaults | ✅ | ✅ |
| Custom signals / targeting | ✅ | ✅ |
| Percent rollout | ✅ | ✅ |
| Country / language targeting | ✅ | ✅ |
| App version / build targeting | ✅ | ✅ |
| Date/time conditions | ✅ | ✅ |
| Regex matching | ✅ | ✅ |
| Config update listeners | ✅ | ✅ |
| `getKeysByPrefix` | ✅ | ✅ |
| `reset()` | ✅ | ✅ |
| `ensureInitialized()` | ✅ | ✅ |
| Minimum fetch interval | ✅ | ✅ |
| Exponential backoff on failure | ✅ | ✅ |
| Price at scale | 100K fetches/day free, then $0.06 per 10K | Free, no fetch metering |
| Config import | ❌ No import path from other providers | ✅ One-click import from Firebase |
| Version history & rollback | ✅ | ✅ |
| Real-time config updates | ✅ Real-time Remote Config | ✅ (push setup required) |
| A/B testing | ✅ Firebase A/B Testing | ✅ Via percentage conditions |
| Analytics audience targeting | ✅ Google Analytics audiences | ❌ Use custom signals instead |
| Device targeting identity | Google Installation ID | AppSpike device ID |
| Custom signals stay on-device | ❌ (sent for server-side evaluation) | ✅ (never transmitted) |

### When to migrate

The two SDKs run side by side in the same app, so nothing forces a single cutover day. Two dates bound the plan: existing Spark projects face throttling enforcement from December 1, 2026, and existing Blaze projects are billed from February 1, 2027.

1. **Today.** Register your app at [console.appspike.dev](https://console.appspike.dev), import your Firebase Remote Config template, and publish. Nothing in your app changes yet.
2. **Next development cycle.** Make the code changes below in a branch. Debug builds can run both SDKs together and compare values.
3. **Before the cutover release.** Finish any in-flight percentage rollouts and experiments on Firebase Remote Config. Rollout groups are re-randomized on AppSpike, so a mid-rollout user can change groups. If your template changed since step 1, import it again.
4. **The cutover release.** Ship the swap as a normal app release. Keep your in-app defaults registered. They cover every device that has not fetched yet.
5. **After the rollout.** Once the release has reached most of your fleet, remove the `com.google.firebase:firebase-config` dependency.

### Step-by-step

**1. Move your config template.** In the [AppSpike console](https://console.appspike.dev), register your app, import your Firebase Remote Config template (Firebase export upload is supported), review it, and publish. Your parameters and conditions exist on the AppSpike side before the app code changes.

**2. Replace the dependency**

```kotlin
// Remove (or firebase-config-ktx in projects predating Firebase BoM 33)
implementation("com.google.firebase:firebase-config")

// Add
implementation("dev.appspike:remote-config:1.4.5")
```

**3. Update imports**

```kotlin
// Before
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

// After
import dev.appspike.AppSpike
import dev.appspike.InitResult
import dev.appspike.remoteconfig.AppSpikeRemoteConfig
import dev.appspike.remoteconfig.RemoteConfigSettings
import dev.appspike.remoteconfig.RemoteConfigFetchException
```

**4. Add initialization.** Firebase Remote Config initialized itself through the `google-services` plugin, so there is no call to replace: this line is *added*, once, at app startup. After it, the `AppSpikeRemoteConfig` singleton takes the place of every `FirebaseRemoteConfig.getInstance()` / `Firebase.remoteConfig` receiver.

```kotlin
AppSpike.initialize(
    context = applicationContext,
    apiKey = "your-api-key",
    modules = listOf(AppSpikeRemoteConfig),
) { result -> /* ready */ }
```

**5. Defaults**

```kotlin
// Before
Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
Firebase.remoteConfig.setDefaultsAsync(mapOf("key" to "value"))

// After
AppSpikeRemoteConfig.setDefaults(context, R.xml.remote_config_defaults)
AppSpikeRemoteConfig.setDefaults(mapOf("key" to "value"))
```

**6. Settings**

```kotlin
// Before
val settings = FirebaseRemoteConfigSettings.Builder()
    .setMinimumFetchIntervalInSeconds(0)
    .setFetchTimeoutInSeconds(60)
    .build()
Firebase.remoteConfig.setConfigSettingsAsync(settings)

// After
AppSpikeRemoteConfig.setConfigSettings(
    RemoteConfigSettings(
        minimumFetchIntervalSeconds = 0,
        fetchTimeoutSeconds = 60,
    )
)
```

**7. Fetch / activate.** Same pattern, suspend instead of Task.

```kotlin
// Before
Firebase.remoteConfig.fetchAndActivate().addOnSuccessListener { changed -> }

// After (suspend function — it throws instead of reporting failure to a listener,
// so catch it: offline and backoff are ordinary outcomes, not crashes)
lifecycleScope.launch {
    try {
        val changed = AppSpikeRemoteConfig.fetchAndActivate()
    } catch (e: RemoteConfigFetchException) {
        // keep serving the current values
    }
}
```

**8. Read values.** Replace the receiver, keep the calls.

```kotlin
// Before
Firebase.remoteConfig.getString("key")
Firebase.remoteConfig.getBoolean("key")
Firebase.remoteConfig.getLong("key")
Firebase.remoteConfig.getDouble("key")
Firebase.remoteConfig.getValue("key").asString()

// After
AppSpikeRemoteConfig.getString("key")
AppSpikeRemoteConfig.getBoolean("key")
AppSpikeRemoteConfig.getLong("key")
AppSpikeRemoteConfig.getDouble("key")
AppSpikeRemoteConfig.getValue("key").asString()
```

**9. Custom signals.** The Firebase call compiles verbatim. It becomes a suspend function.

```kotlin
// Before
Firebase.remoteConfig.setCustomSignals(customSignals { put("tier", "gold") })

// After — same builder, same put; suspend, so call it from a coroutine
AppSpikeRemoteConfig.setCustomSignals(customSignals { put("tier", "gold") })
```

### API mapping reference

AppSpike keeps compile-compatible aliases for the member spellings Firebase uses, so once the **types** are renamed most call sites compile unchanged. The aliases delegate to the canonical AppSpike names. Rows marked "Same" need no edit beyond the receiver.

#### Firebase Remote Config → AppSpike

Covers `com.google.firebase:firebase-config` used from Kotlin, including the KTX idioms (`remoteConfigSettings { }`, `customSignals { }`, `configUpdates`):

| Firebase | AppSpike | Change |
|----------|----------|--------|
| `FirebaseRemoteConfig.getInstance()`, `Firebase.remoteConfig` | `AppSpikeRemoteConfig` | Object singleton |
| `Firebase.remoteConfig.getString("key")` | `AppSpikeRemoteConfig.getString("key")` | Replace receiver |
| `Firebase.remoteConfig.getBoolean("key")` | `AppSpikeRemoteConfig.getBoolean("key")` | Replace receiver |
| `Firebase.remoteConfig.getLong("key")` | `AppSpikeRemoteConfig.getLong("key")` | Replace receiver |
| `Firebase.remoteConfig.getDouble("key")` | `AppSpikeRemoteConfig.getDouble("key")` | Replace receiver |
| `Firebase.remoteConfig.getValue("key")` | `AppSpikeRemoteConfig.getValue("key")` | Replace receiver |
| `value.asString()` | `value.asString()` | Same |
| `value.asBoolean()` | `value.asBoolean()` | Same (returns false, not throws) |
| `value.asLong()` | `value.asLong()` | Same (returns 0, not throws) |
| `value.asDouble()` | `value.asDouble()` | Same |
| `value.asByteArray()` | `value.asByteArray()` | Same |
| `value.source` | `value.source` | Same spelling. The type is the `ValueSource` enum, not an `Int` |
| `VALUE_SOURCE_*`, `LAST_FETCH_STATUS_*` | `ValueSource.*`, `FetchStatus.*` | **Edit required.** Enums, not `Int` constants |
| `Firebase.remoteConfig.getAll()`, `config.all` | `getAll()`, `all` | Same |
| `Firebase.remoteConfig.getKeysByPrefix("p")` | `AppSpikeRemoteConfig.getKeysByPrefix("p")` | Same |
| `Firebase.remoteConfig.getInfo()`, `config.info` | `getInfo()`, `info` | Same |
| `info.fetchTimeMillis` | `info.fetchTimeMillis` | Same (`lastFetchTimeMillis` is the canonical spelling) |
| `info.lastFetchStatus`, `info.configSettings` | same | Same |
| `remoteConfigSettings { minimumFetchIntervalInSeconds = … }` | same | Same builder function and property names |
| `FirebaseRemoteConfigSettings.Builder().setFetchTimeoutInSeconds(…)` | `RemoteConfigSettings.Builder()…` | Same setter names |
| `Firebase.remoteConfig.setConfigSettingsAsync(s)` | `setConfigSettingsAsync(s)` | Returns `Unit`, not `Task<Void>` |
| `Firebase.remoteConfig.setDefaultsAsync(map)` | `setDefaultsAsync(map)` | Returns `Unit`, not `Task<Void>` |
| `Firebase.remoteConfig.setDefaultsAsync(resId)` | `AppSpikeRemoteConfig.setDefaults(ctx, resId)` | **Edit required.** Needs a `Context` |
| `customSignals { put(k, v) }` | same | Same builder function |
| `Firebase.remoteConfig.setCustomSignals(s)` | `AppSpikeRemoteConfig.setCustomSignals(s)` | Suspend |
| `Firebase.remoteConfig.fetch()`, `config.fetch(seconds)` | `AppSpikeRemoteConfig.fetch()` | Suspend instead of Task |
| `Firebase.remoteConfig.activate()` | `AppSpikeRemoteConfig.activate()` | Suspend instead of Task |
| `Firebase.remoteConfig.fetchAndActivate()` | `AppSpikeRemoteConfig.fetchAndActivate()` | Suspend instead of Task |
| `Firebase.remoteConfig.addOnConfigUpdateListener(l)` | `AppSpikeRemoteConfig.addOnConfigUpdateListener(l)` | Same. `ConfigUpdateListener` has the same two methods |
| `ConfigUpdateListener.onUpdate(ConfigUpdate)` | same | `configUpdate.updatedKeys` |
| `ConfigUpdateListener.onError(e)` | `onError(error: RemoteConfigException)` | Same shape |
| `registration.remove()` | same | Same |
| `Firebase.remoteConfig.configUpdates` | same | `Flow<ConfigUpdate>` |
| `config[key]` (KTX) | `config[key]` | Resolves where the expected type is known (`val v: RemoteConfigValue = config["k"]`). For the chained form use `getValue("k").asString()` |
| `Firebase.remoteConfig.reset()` | `AppSpikeRemoteConfig.reset()` | Synchronous |
| `Firebase.remoteConfig.ensureInitialized()` | `AppSpikeRemoteConfig.ensureInitialized()` | Suspend instead of Task |
| `FirebaseRemoteConfigException` | `RemoteConfigException` | Base type |
| `FirebaseRemoteConfigClientException` / `ServerException` | `RemoteConfigFetchException` | One fetch-failure type |
| `FirebaseRemoteConfigFetchThrottledException` | `RemoteConfigThrottledException` | `e.throttleEndTimeMillis`, same spelling |
| `task.addOnCompleteListener { }`, `task.await()` | N/A | **Not supported.** AppSpike never returns a Play Services `Task`, so use the suspend functions |
| `FirebaseRemoteConfigException.Code`, `e.code` | N/A | Not implemented |

### AI-Assisted Migration

Copy the prompt below into your AI coding assistant (Claude, Cursor, Copilot, etc.) to migrate automatically:

<details>
<summary>Migration prompt</summary>

```
Migrate this Android project from Firebase Remote Config to AppSpike Remote Config.

Rules:
1. Replace the Firebase Remote Config dependency with:
   implementation("dev.appspike:remote-config:1.4.5")

2. Replace all Firebase Remote Config imports:
   - com.google.firebase.remoteconfig.* → dev.appspike.remoteconfig.*
   - Add: import dev.appspike.AppSpike and import dev.appspike.InitResult

3. Replace FirebaseRemoteConfig.getInstance() with AppSpikeRemoteConfig (object singleton)

4. Add AppSpike initialization before any Remote Config usage:
   AppSpike.initialize(
       context = applicationContext,
       apiKey = "YOUR_API_KEY",
       modules = listOf(AppSpikeRemoteConfig),
   ) { result -> }

5. Replace Task-based async calls with suspend functions. A suspend fetch throws where the
   Task reported failure to a listener, so wrap it — offline and backoff are ordinary:
   - config.fetch().addOnSuccessListener { } →
     lifecycleScope.launch { try { AppSpikeRemoteConfig.fetch() } catch (e: RemoteConfigFetchException) { } }
   - config.activate().addOnSuccessListener { changed -> } → val changed = AppSpikeRemoteConfig.activate()
   - config.fetchAndActivate().addOnSuccessListener { } → AppSpikeRemoteConfig.fetchAndActivate() (same try/catch)

6. Replace settings builder:
   - FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()
   → RemoteConfigSettings(minimumFetchIntervalSeconds = 0)
   - config.setConfigSettingsAsync(s) → AppSpikeRemoteConfig.setConfigSettings(s)

7. Replace defaults:
   - config.setDefaultsAsync(R.xml.defaults) → AppSpikeRemoteConfig.setDefaults(context, R.xml.defaults)
   - config.setDefaultsAsync(map) → AppSpikeRemoteConfig.setDefaults(map)

8. The following value access APIs are IDENTICAL and need only receiver changes:
   getString, getBoolean, getLong, getDouble, getValue, getAll, getKeysByPrefix
   value.asString(), value.asBoolean(), value.asLong(), value.asDouble(), value.asByteArray()

9. Replace error handling:
   - FirebaseRemoteConfigException → RemoteConfigException (the base type)
   - FirebaseRemoteConfigFetchThrottledException → RemoteConfigThrottledException
     (e.throttleEndTimeMillis keeps the same spelling)
   - FirebaseRemoteConfigClientException / ServerException → RemoteConfigFetchException
   - General fetch errors → catch (e: RemoteConfigFetchException)

Apply these changes to every file in the project. After migrating, verify the project builds.
```

</details>

## Requirements

- Android API 23+
- Kotlin 2.0+

## Modules

| Module | Coordinates | Description |
|--------|-------------|-------------|
| SDK Core | `dev.appspike:sdk-core` | Session management, authentication, device context |
| Remote Config | `dev.appspike:remote-config` | Remote config with local evaluation of all condition types |

## Sample App

1. Set `APPSPIKE_API_KEY` in `SampleApp/src/main/kotlin/dev/appspike/sample/ApiKey.kt`.
2. Build and install on a connected device or emulator:

```bash
cd SampleApp
./gradlew installDebug
```

## License

Copyright (c) 2026 Pyde Technologies LTD. All rights reserved.

The AppSpike SDK is proprietary software, free to use with AppSpike services. Redistribution, modification, and reverse engineering are not permitted. See [LICENSE](LICENSE) for the full terms, or contact info@pyde.tech.

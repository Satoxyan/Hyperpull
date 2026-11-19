# HyperPull

Make every normal notification on HyperOS 4 pullable into HyperOS's floating
(small) window, not just the ones Xiaomi whitelists.

HyperOS already ships the gesture: a thin `mini_window_bar` at the bottom of a
notification row. SystemUI decides per notification whether that bar can slide,
and apps outside Xiaomi's whitelist (for example Telegram) lose the gesture even
though the app itself supports freeform. HyperPull hooks
`ExpandableNotificationRowInjector` and forces `canSlide`/`miniBarVisible` true
for every notification row.

This is an Xposed module for LSPosed (libxposed API 102). It only touches normal
notifications — the Super Island / focus notification path is not modified.

## Requirements

- HyperOS 4 (Android 17) with SystemUI component 18.x
- Root + LSPosed (or another manager supporting libxposed API 102)
- JDK 21 and Android SDK 37.2 to build

## Build

```bash
cd android
./gradlew :app:assembleDebug
```

If the SDK is not found, create `android/local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

APK output: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Install

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

1. Open LSPosed, enable **HyperPull**.
2. Set its scope to **System UI** (`com.android.systemui`) only.
3. Reboot.

The module has no launcher icon and no settings screen; it is either enabled in
LSPosed or not.

## Test

1. Send yourself a Telegram message (or any app that normally cannot be pulled).
2. Open the notification shade.
3. Pull down the small bar at the bottom of the notification.

Expected: the app opens in a HyperOS floating window.

If it does not work, collect the log:

```bash
adb logcat -s HyperPull
```

On load the module confirms which methods it hooked:

```text
hooked=updateMiniWindowBar,updateFreeformTipView
```

The first time a row is processed, it logs:

```text
ExpandableNotificationRowInjector bar=true canSlide=true miniBarVisible=true
```

If no `HyperPull` line appears at all, the hook never installed or never fired:
check that the scope is System UI and that the device was rebooted.

- `canSlide=false` / `miniBarVisible=false` — the field was not found on this
  SystemUI version. The module failed open and changed nothing.
- `bar=false` — `getMiniBar()` was not found or returned null.
- All true but no window — SystemUI has another eligibility check after the
  gesture. Report the log; the next step is to locate and neutralise that check.

## Limitations

- No per-app blacklist. Apps that genuinely cannot run in freeform (for example
  Google Authenticator) may show the bar but fail to open a window.
- Hooks are tied to internal SystemUI class/field names. A HyperOS update can
  rename them; the module then logs `not found` and does nothing.
- The bar is forced visible on every row, so it can appear on rows where the
  stock UI would hide it.
- Built and unit-tested, but device behaviour is not verified in CI here.

## Uninstall

Disable the module in LSPosed and reboot, then uninstall the APK. No data or
system files are modified.

## How it works

- `HyperPullModule` is the libxposed entry point; the static scope comes from
  `META-INF/xposed/scope.list`.
- `NotificationMiniWindowHook` hooks `updateMiniWindowBar()` and
  `updateFreeformTipView()` on `ExpandableNotificationRowInjector`.
- After each native call, it sets `canSlide=true`, `miniBarVisible=true`, and
  makes the mini bar visible with full alpha.
- `Reflect` contains the fail-open reflection helpers, covered by JVM tests.

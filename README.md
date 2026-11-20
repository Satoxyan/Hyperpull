# HyperPull

Make every normal notification on HyperOS 4 pullable into HyperOS's floating
(small) window, not just the ones Xiaomi whitelists.

HyperOS already ships the gesture: a thin `mini_window_bar` at the bottom of a
heads-up notification. SystemUI only answers "can this package slide?" true for
apps in Xiaomi's whitelist, so apps like Telegram lose the gesture even though
they support freeform. HyperPull hooks `AppMiniWindowManagerImpl.canNotificationSlide`
and makes that package check answer true for every app.

It deliberately does **not** force the bar or `canSlide` on each row. SystemUI
keeps its own visibility rule (the bar only appears on pinned/heads-up rows), so
no bar is added to notifications sitting in the shade.

This is an Xposed module for LSPosed (libxposed API 102). It only touches normal
notifications — the Super Island / focus notification path is not modified.

It also unlocks HyperOS's simultaneous freeform window cap from 0.3.0. HyperOS
keeps the count in `MiuiFreeFormStackDisplayStrategy.getMaxMiuiFreeFormStackCount`
and exits the bottom window when a new one would exceed it, so opening a third
window replaces the first. The module returns a high count, disables the
per-package `shouldStopStartFreeform` gate, and removes the game-window
exclusivity rule. On Android 16 / HyperOS 4 that method is private; the scan
matches by name across `declaredMethods`.

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
2. Set its scope to **System UI** (`com.android.systemui`) and **System
   Framework** (`system`).
3. Reboot.

The module has no launcher icon and no settings screen; it is either enabled in
LSPosed or not.

## Test

1. Send yourself a Telegram message (or any app that normally cannot be pulled).
2. When the heads-up notification appears, pull down the small bar at its bottom.
3. Optionally open the shade: the bar must **not** appear there.

Expected: the app opens in a HyperOS floating window from heads-up only.

### Freeform window limit

Open three different apps in floating windows. None of the earlier ones should
close. The log only reports install status:

```text
loaded in system_server
freeform count unlocked (N method(s))
freeform start gate unlocked
game freeform exclusivity unlocked
```

If the third window still replaces one, the `loaded in system_server` line tells
you whether the hook loaded at all:

- No `loaded in system_server` line — System Framework scope is missing or the
  module did not load in `system_server`.
- `freeform count method not found` — HyperOS renamed the count method.
- Hooks installed but a window still closes — the cap now lives elsewhere;
  report the log and the caller stack.

If it does not work, collect the log:

```bash
adb logcat -s HyperPull
```

On load the module confirms its hook:

```text
hooked=AppMiniWindowManagerImpl.canNotificationSlide
whitelist unlocked (2 list(s))
```

If no `HyperPull` line appears at all, the hook never installed or never fired:
check that the scope is System UI and that the device was rebooted.

- `canNotificationSlide not found` — this SystemUI version renamed the class or
  method. The module failed open and changed nothing.
- `hooked=...` but no `whitelist unlocked` line — the settings object or its
  whitelist fields moved. Also fail-open, nothing changed.
- Bar shows in heads-up but the pull does not open a window — Telegram accepts
  the gesture but SystemUI's launch step rejected it. Report the log; the next
  step is to inspect that launch path.

## Limitations

- No per-app blacklist. Apps that genuinely cannot run in freeform (for example
  Google Authenticator) may show the bar in heads-up but fail to open a window.
- Hooks are tied to internal SystemUI class/field names. A HyperOS update can
  rename them; the module then logs `not found` and does nothing.
- The bar follows the stock heads-up rule; it is not shown on rows in the
  notification shade.
- Built and unit-tested, but device behaviour is not verified in CI here.

## Uninstall

Disable the module in LSPosed and reboot, then uninstall the APK. No data or
system files are modified.

## How it works

- `HyperPullModule` is the libxposed entry point; the static scope comes from
  `META-INF/xposed/scope.list`.
- `MiniWindowWhitelistHook` hooks
  `AppMiniWindowManagerImpl.canNotificationSlide(String, PendingIntent)`.
- Before the native call, it replaces `mAllowNotificationSlide` /
  `mAllowNotificationSlideLocal` with a read-only list view that reports every
  non-blank package as allowed. The other gates (activity PendingIntent, feature
  flag, keyguard/window state) stay native.
- SystemUI then computes `canSlide` and bar visibility itself, so the bar still
  only shows on pinned/heads-up rows.
- `Reflect` contains the fail-open reflection helpers, covered by JVM tests.
- `FreeformLimitHook` runs in `system_server` (scope `system`) and hooks
  `MiuiFreeFormStackDisplayStrategy.getMaxMiuiFreeFormStackCount` (by name, so
  the Android 16 private visibility is fine), `shouldStopStartFreeform`, and the
  game-only `MiuiFreeFormActivityStack.isGameApp` check.
- `isGameApp` is only overridden when the caller is one of the window-exiting
  paths; `needForegroundPin` keeps the original answer so game windows stay
  pinnable.

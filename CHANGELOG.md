# Changelog

Feature history for HyperPull. Versions before 0.3.0 are documented from the
existing release notes.

## 0.3.0

- Unlocked the simultaneous freeform window limit. Opening more floating windows
  no longer closes the oldest one.
- Removed the per-package freeform start gate (`shouldStopStartFreeform`).
- Removed the game-window freeform exclusivity rule, while keeping game windows
  pinnable.
- Added the `system` (System Framework) scope for the hooks in `system_server`.

## 0.2.0

- The notification mini-window bar now follows the stock heads-up rule: it
  appears only on heads-up notifications and never on rows inside the shade.
- Replaced the forced per-row visibility with a whitelist unlock at
  `AppMiniWindowManagerImpl.canNotificationSlide`, so SystemUI keeps deciding
  `canSlide` and every other gate.

## 0.1.0

- Initial release: every normal notification can be pulled into a HyperOS
  floating window, not just Xiaomi-whitelisted apps.

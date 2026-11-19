package io.github.satoxyan.hyperpull

import io.github.libxposed.api.XposedModule
import java.util.concurrent.ConcurrentHashMap

/**
 * Unlocks SystemUI's per-package mini-window whitelist.
 *
 * `ExpandableNotificationRowInjector.updateMiniWindowBar()` and
 * `updateFreeformTipView()` call `canNotificationSlide(pkg, pendingIntent)` and
 * only then decide `canSlide` and whether the bar is visible at all. By making
 * that gate answer true for every package we keep the native heads-up gate:
 * the bar still appears only on pinned/modal (heads-up) rows, but for every app.
 */
internal object MiniWindowWhitelistHook {

    private const val TAG = "HyperPull"

    private val MANAGER_CLASSES = listOf(
        "com.android.systemui.statusbar.notification.policy.AppMiniWindowManagerImpl",
        "com.android.systemui.statusbar.notification.policy.AppMiniWindowManager",
    )
    private const val SETTINGS_FIELD = "notificationSettingsManager"
    private val WHITELIST_FIELDS = listOf("mAllowNotificationSlide", "mAllowNotificationSlideLocal")

    private val installedLoaders = ConcurrentHashMap.newKeySet<ClassLoader>()
    private val loggedSettings = ConcurrentHashMap.newKeySet<Class<*>>()

    fun install(module: XposedModule, classLoader: ClassLoader) {
        if (!installedLoaders.add(classLoader)) return

        val gate = MANAGER_CLASSES.firstNotNullOfOrNull { className ->
            runCatching {
                Class.forName(className, false, classLoader).declaredMethods.firstOrNull {
                    it.name == "canNotificationSlide" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0] == String::class.java
                }
            }.getOrNull()
        }

        if (gate == null) {
            installedLoaders.remove(classLoader)
            module.log(android.util.Log.INFO, TAG, "canNotificationSlide not found")
            return
        }

        runCatching {
            module.hook(gate).intercept { chain ->
                runCatching { unlock(module, chain.thisObject) }
                chain.proceed()
            }
            module.log(
                android.util.Log.INFO,
                TAG,
                "hooked=${gate.declaringClass.simpleName}.canNotificationSlide",
            )
        }.onFailure {
            installedLoaders.remove(classLoader)
            module.log(android.util.Log.INFO, TAG, "hook canNotificationSlide failed — ${it.message}")
        }
    }

    private fun unlock(module: XposedModule, manager: Any?) {
        if (manager == null) return
        val settings = Reflect.get(manager, SETTINGS_FIELD) ?: return

        val wrapped = WHITELIST_FIELDS.count { name ->
            val whitelist = wrapWhitelist(Reflect.get(settings, name)) ?: return@count false
            Reflect.set(settings, name, whitelist)
        }

        if (wrapped > 0 && loggedSettings.add(settings.javaClass)) {
            module.log(android.util.Log.INFO, TAG, "whitelist unlocked ($wrapped list(s))")
        }
    }
}

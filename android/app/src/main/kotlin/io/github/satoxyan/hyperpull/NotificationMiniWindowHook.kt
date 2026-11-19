package io.github.satoxyan.hyperpull

import android.view.View
import io.github.libxposed.api.XposedModule
import java.util.concurrent.ConcurrentHashMap

internal fun hookSummary(names: List<String>): String =
    if (names.isEmpty()) "no injector methods found" else "hooked=" + names.joinToString(",")

internal object NotificationMiniWindowHook {

    private const val INJECTOR_CLASS =
        "com.android.systemui.statusbar.notification.row.ExpandableNotificationRowInjector"
    private const val METHODS = "updateMiniWindowBar,updateFreeformTipView"
    private const val TAG = "HyperPull"

    private val installedLoaders = ConcurrentHashMap.newKeySet<ClassLoader>()
    private val loggedClasses = ConcurrentHashMap.newKeySet<Class<*>>()

    fun install(module: XposedModule, classLoader: ClassLoader) {
        if (!installedLoaders.add(classLoader)) return

        val loaded = runCatching { Class.forName(INJECTOR_CLASS, false, classLoader) }
        val injector = loaded.getOrNull()
        if (injector == null) {
            installedLoaders.remove(classLoader)
            module.log(
                android.util.Log.INFO,
                TAG,
                "$INJECTOR_CLASS not found: ${loaded.exceptionOrNull()?.message}",
            )
            return
        }

        val hooked = mutableListOf<String>()
        for (name in METHODS.split(",")) {
            val method = runCatching {
                injector.declaredMethods.firstOrNull { it.name == name && it.parameterCount == 0 }
            }.getOrNull() ?: continue

            runCatching {
                module.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    runCatching { apply(module, chain.thisObject) }
                    result
                }
                hooked += name
            }.onFailure {
                module.log(android.util.Log.INFO, TAG, "hook $name failed — ${it.message}")
            }
        }

        if (hooked.isEmpty()) installedLoaders.remove(classLoader)
        module.log(android.util.Log.INFO, TAG, hookSummary(hooked))
    }

    private fun apply(module: XposedModule, injector: Any?) {
        if (injector == null) return

        val bar = Reflect.invokeNoArg(injector, "getMiniBar") as? View
        val canSlide = Reflect.setBoolean(injector, "canSlide", true)
        val miniBarVisible = Reflect.setBoolean(injector, "miniBarVisible", true)
        if (canSlide || miniBarVisible) {
            bar?.let {
                it.alpha = 1f
                it.visibility = View.VISIBLE
            }
        }

        if (loggedClasses.add(injector.javaClass)) {
            module.log(
                android.util.Log.INFO,
                TAG,
                "${injector.javaClass.simpleName} " +
                    "bar=${bar != null} canSlide=$canSlide miniBarVisible=$miniBarVisible",
            )
        }
    }
}

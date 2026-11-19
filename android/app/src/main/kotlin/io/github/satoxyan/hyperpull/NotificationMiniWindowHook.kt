package io.github.satoxyan.hyperpull

import android.view.View
import io.github.libxposed.api.XposedModule
import java.util.Collections
import java.util.WeakHashMap

internal object NotificationMiniWindowHook {

    private const val INJECTOR_CLASS =
        "com.android.systemui.statusbar.notification.row.ExpandableNotificationRowInjector"
    private const val METHODS = "updateMiniWindowBar,updateFreeformTipView"

    private val installedLoaders = Collections.newSetFromMap(WeakHashMap<ClassLoader, Boolean>())
    private val loggedClasses = Collections.newSetFromMap(WeakHashMap<Class<*>, Boolean>())

    fun install(module: XposedModule, classLoader: ClassLoader) {
        if (!installedLoaders.add(classLoader)) return

        val injector = runCatching { Class.forName(INJECTOR_CLASS, false, classLoader) }
            .getOrNull()
        if (injector == null) {
            installedLoaders.remove(classLoader)
            module.log(android.util.Log.INFO, "HyperPull", "$INJECTOR_CLASS not found")
            return
        }

        var installed = false
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
                installed = true
            }.onFailure {
                module.log(android.util.Log.INFO, "HyperPull", "hook $name failed — ${it.message}")
            }
        }

        if (!installed) {
            installedLoaders.remove(classLoader)
            module.log(android.util.Log.INFO, "HyperPull", "no injector methods found")
        }
    }

    private fun apply(module: XposedModule, injector: Any?) {
        if (injector == null) return

        val bar = Reflect.invokeNoArg(injector, "getMiniBar") as? View
        bar?.let {
            it.alpha = 1f
            it.visibility = View.VISIBLE
        }
        val canSlide = Reflect.setBoolean(injector, "canSlide", true)
        val miniBarVisible = Reflect.setBoolean(injector, "miniBarVisible", true)

        if (loggedClasses.add(injector.javaClass)) {
            module.log(
                android.util.Log.INFO,
                "HyperPull",
                "${injector.javaClass.simpleName} " +
                    "bar=${bar != null} canSlide=$canSlide miniBarVisible=$miniBarVisible",
            )
        }
    }
}

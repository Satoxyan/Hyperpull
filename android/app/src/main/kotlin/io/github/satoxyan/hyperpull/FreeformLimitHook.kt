package io.github.satoxyan.hyperpull

import io.github.libxposed.api.XposedModule

/**
 * Unlocks HyperOS's simultaneous freeform/floating window limit.
 *
 * system_server computes the cap in
 * `MiuiFreeFormStackDisplayStrategy.getMaxMiuiFreeFormStackCount(...)`.
 * `onMiuiFreeFormStasckAdded` exits the bottom stack when the new window pushes
 * the count past that cap, which is the "a new window replaces an old one"
 * behaviour. `shouldStopStartFreeform` blocks a third freeform of the same
 * package, and game windows have a separate rule that exits other game windows
 * regardless of the cap.
 *
 * On Android 16 / HyperOS 4 the count method became private, so the scan uses
 * `declaredMethods` and matches by name instead of visibility.
 */
internal object FreeformLimitHook {

    private const val TAG = "HyperPull"
    private const val UNLOCKED = 256

    private val COUNT_METHODS = setOf(
        "getMaxMiuiFreeFormStackCount",
        "getMaxMiuiFreeFormStackCountForFlashBack",
    )

    private val GAME_LIMIT_CALLERS = setOf(
        "com.android.server.wm.MiuiFreeFormStackDisplayStrategy#onMiuiFreeFormStasckAdded",
        "com.android.server.wm.MiuiFreeFormManagerService#getGameFreeFormCount",
    )

    // Lambdas compiled by the framework carry a numeric suffix, match the prefix.
    private val GAME_LIMIT_CALLER_PREFIXES = listOf(
        "com.android.server.wm.MiuiFreeFormManagerService#lambda\$getBottomGameFreeFormActivityStack",
    )

    fun install(module: XposedModule, classLoader: ClassLoader) {
        val strategy = runCatching {
            Class.forName(
                "com.android.server.wm.MiuiFreeFormStackDisplayStrategy",
                false,
                classLoader,
            )
        }.getOrNull()

        if (strategy == null) {
            module.log(android.util.Log.INFO, TAG, "MiuiFreeFormStackDisplayStrategy not found")
            return
        }

        val countMethods = strategy.declaredMethods.filter { it.name in COUNT_METHODS }
        countMethods.forEach { method ->
            runCatching {
                module.hook(method).intercept { chain ->
                    val pkg = runCatching { chain.getArg(0) as? String }.getOrNull()
                    module.log(
                        android.util.Log.INFO,
                        TAG,
                        "freeform max ${method.name}($pkg) -> $UNLOCKED",
                    )
                    UNLOCKED
                }
            }.onFailure {
                module.log(android.util.Log.INFO, TAG, "hook ${method.name} failed — ${it.message}")
            }
        }

        if (countMethods.isEmpty()) {
            module.log(android.util.Log.INFO, TAG, "freeform count method not found")
        } else {
            module.log(
                android.util.Log.INFO,
                TAG,
                "freeform count unlocked (${countMethods.size} method(s))",
            )
        }

        strategy.declaredMethods.firstOrNull { it.name == "shouldStopStartFreeform" }?.let { method ->
            runCatching {
                module.hook(method).intercept { false }
                module.log(android.util.Log.INFO, TAG, "freeform start gate unlocked")
            }.onFailure {
                module.log(
                    android.util.Log.INFO,
                    TAG,
                    "hook shouldStopStartFreeform failed — ${it.message}",
                )
            }
        }

        installGameRule(module, classLoader)
    }

    private fun installGameRule(module: XposedModule, classLoader: ClassLoader) {
        val stack = runCatching {
            Class.forName(
                "com.android.server.wm.MiuiFreeFormActivityStack",
                false,
                classLoader,
            )
        }.getOrNull() ?: return

        val isGameApp = stack.declaredMethods.firstOrNull { it.name == "isGameApp" } ?: return

        runCatching {
            module.hook(isGameApp).intercept { chain ->
                val result = chain.proceed()
                if (result == true && isGameLimitCaller(Throwable().stackTrace)) false else result
            }
            module.log(android.util.Log.INFO, TAG, "game freeform exclusivity unlocked")
        }.onFailure {
            module.log(android.util.Log.INFO, TAG, "hook isGameApp failed — ${it.message}")
        }
    }

    /**
     * Only the paths that exit other windows may treat a game as a normal app.
     * `needForegroundPin` also calls `isGameApp` and must keep the original
     * answer, otherwise game windows lose their foreground-pin eligibility.
     */
    internal fun isGameLimitCaller(frames: Array<StackTraceElement>): Boolean =
        frames.any { frame ->
            val caller = "${frame.className}#${frame.methodName}"
            caller in GAME_LIMIT_CALLERS ||
                GAME_LIMIT_CALLER_PREFIXES.any { caller.startsWith(it) }
        }
}

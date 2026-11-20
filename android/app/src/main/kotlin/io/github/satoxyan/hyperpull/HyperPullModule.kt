package io.github.satoxyan.hyperpull

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

class HyperPullModule : XposedModule() {

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.android.systemui") return
        log(android.util.Log.INFO, "HyperPull", "loaded in ${param.packageName}")
        MiniWindowWhitelistHook.install(this, param.defaultClassLoader)
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(android.util.Log.INFO, "HyperPull", "loaded in system_server")
        FreeformLimitHook.install(this, param.classLoader)
    }
}

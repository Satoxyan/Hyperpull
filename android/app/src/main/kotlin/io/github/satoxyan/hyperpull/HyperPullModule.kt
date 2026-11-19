package io.github.satoxyan.hyperpull

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class HyperPullModule : XposedModule() {

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.android.systemui") return
        log(android.util.Log.INFO, "HyperPull", "loaded in ${param.packageName}")
    }
}

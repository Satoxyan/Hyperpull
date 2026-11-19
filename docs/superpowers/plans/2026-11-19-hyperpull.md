# HyperPull Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modul Xposed kecil yang membuat tarik-turun notifikasi normal HyperOS 4 selalu memicu floating window.

**Architecture:** Hook `ExpandableNotificationRowInjector` milik SystemUI, lalu setelah method native selesai paksa `canSlide`/`miniBarVisible` true dan tampilkan `mini_window_bar`. Tidak ada UI, tidak ada peluncur freeform sendiri.

**Tech Stack:** Kotlin, Android Gradle Plugin 9.5.1, Kotlin 2.4.21, Gradle 9.9.0, libxposed API 102.0.0, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-19-hyperpull-design.md`

## Global Constraints

- `applicationId` dan namespace: `io.github.satoxyan.hyperpull`.
- `minSdk = 33`, `targetSdk = 37`, `compileSdk` 37, JVM target 21.
- Scope Xposed hanya `com.android.systemui`, `staticScope=true`.
- Tanpa Compose, tanpa activity, tanpa settings, tanpa filter per app.
- Semua reflection SystemUI wajib fail-open (`runCatching`), tidak boleh crash.
- Nama file hook: `NotificationMiniWindowHook.kt`; entry point: `HyperPullModule.kt`.
- Tag log: `HyperPull`.

## Review Focus

- Field `canSlide` tidak ada di versi SystemUI lain → `setBoolean` mengembalikan `false`, tidak crash.
- Field bertipe `java.lang.Boolean`, bukan primitif → tetap ter-set.
- Field berada di superclass → tetap ditemukan.
- `getMiniBar()` tidak ada atau mengembalikan null → hook tetap jalan tanpa menyentuh view.
- Class/method injector tidak ditemukan → modul tidak memasang hook dan hanya menulis log.

---

### Task 1: Scaffold proyek Android + metadata modul

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/resources/META-INF/xposed/module.prop`
- Create: `android/app/src/main/resources/META-INF/xposed/scope.list`
- Create: `android/app/src/main/resources/META-INF/xposed/java_init.list`
- Create: `android/app/src/main/kotlin/dev/syoxide/notifyfloat/HyperPullModule.kt`
- Create: `.gitignore`
- Reuse: `android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/*` (sudah disalin)

**Interfaces:**
- Consumes: tidak ada.
- Produces: APK debug yang bisa di-install; class `io.github.satoxyan.hyperpull.HyperPullModule : XposedModule()` dengan `onPackageLoaded(param)`.

- [ ] **Step 1: Tulis konfigurasi Gradle**

`android/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")
    }
}

rootProject.name = "HyperPull"
include(":app")
```

`android/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
}
```

`android/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 2: Tulis `android/app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.satoxyan.hyperpull"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "io.github.satoxyan.hyperpull"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 3: Tulis manifest dan metadata libxposed**

`android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="HyperPull" />
</manifest>
```

`android/app/src/main/resources/META-INF/xposed/module.prop`:

```properties
minApiVersion=101
targetApiVersion=102
staticScope=true
```

`android/app/src/main/resources/META-INF/xposed/scope.list`:

```text
com.android.systemui
```

`android/app/src/main/resources/META-INF/xposed/java_init.list`:

```text
io.github.satoxyan.hyperpull.HyperPullModule
```

- [ ] **Step 4: Tulis entry point minimal**

`android/app/src/main/kotlin/dev/syoxide/notifyfloat/HyperPullModule.kt`:

```kotlin
package io.github.satoxyan.hyperpull

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class HyperPullModule : XposedModule() {

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.android.systemui") return
        log(android.util.Log.INFO, "HyperPull", "loaded in ${param.packageName}")
    }
}
```

- [ ] **Step 5: Tulis `.gitignore`**

```gitignore
.gradle/
build/
android/.gradle/
android/build/
android/app/build/
local.properties
*.iml
.idea/
```

- [ ] **Step 6: Build APK**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, file `android/app/build/outputs/apk/debug/app-debug.apk` ada.

- [ ] **Step 7: Commit**

```bash
git add .gitignore android docs
git commit -m "chore: scaffold HyperPull Xposed module"
```

---

### Task 2: Helper reflection yang bisa diuji di JVM

**Files:**
- Create: `android/app/src/main/kotlin/dev/syoxide/notifyfloat/Reflect.kt`
- Test: `android/app/src/test/kotlin/dev/syoxide/notifyfloat/ReflectTest.kt`

**Interfaces:**
- Consumes: tidak ada.
- Produces:
  - `Reflect.findField(type: Class<*>, name: String): Field?`
  - `Reflect.setBoolean(target: Any, name: String, value: Boolean): Boolean`
  - `Reflect.invokeNoArg(target: Any, name: String): Any?`

- [ ] **Step 1: Tulis test yang gagal**

`android/app/src/test/kotlin/dev/syoxide/notifyfloat/ReflectTest.kt`:

```kotlin
package io.github.satoxyan.hyperpull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectTest {

    private open class Base {
        var miniBarVisible = false
    }

    private class Fake : Base() {
        var canSlide = false

        var boxed: Boolean? = false

        fun getMiniBar(): String = "bar"
    }

    @Test
    fun setsPrimitiveField() {
        val fake = Fake()
        assertTrue(Reflect.setBoolean(fake, "canSlide", true))
        assertTrue(fake.canSlide)
    }

    @Test
    fun setsInheritedField() {
        val fake = Fake()
        assertTrue(Reflect.setBoolean(fake, "miniBarVisible", true))
        assertTrue(fake.miniBarVisible)
    }

    @Test
    fun setsBoxedBooleanField() {
        val fake = Fake()
        assertTrue(Reflect.setBoolean(fake, "boxed", true))
        assertEquals(true, fake.boxed)
    }

    @Test
    fun missingFieldReturnsFalse() {
        assertFalse(Reflect.setBoolean(Fake(), "doesNotExist", true))
    }

    @Test
    fun invokeNoArgReturnsValue() {
        assertEquals("bar", Reflect.invokeNoArg(Fake(), "getMiniBar"))
    }

    @Test
    fun missingMethodReturnsNull() {
        assertNull(Reflect.invokeNoArg(Fake(), "nope"))
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'io.github.satoxyan.hyperpull.ReflectTest'`
Expected: FAIL, `unresolved reference: Reflect`.

- [ ] **Step 3: Implementasikan `Reflect.kt`**

```kotlin
package io.github.satoxyan.hyperpull

import java.lang.reflect.Field

internal object Reflect {

    fun findField(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            val field = runCatching { current.getDeclaredField(name) }.getOrNull()
            if (field != null) {
                field.isAccessible = true
                return field
            }
            current = current.superclass
        }
        return null
    }

    fun setBoolean(target: Any, name: String, value: Boolean): Boolean {
        val field = findField(target.javaClass, name) ?: return false
        return runCatching {
            if (field.type == java.lang.Boolean::class.java) {
                field.set(target, value)
            } else {
                field.setBoolean(target, value)
            }
            true
        }.getOrDefault(false)
    }

    fun invokeNoArg(target: Any, name: String): Any? = runCatching {
        val method = target.javaClass.getMethod(name)
        method.isAccessible = true
        method.invoke(target)
    }.getOrNull()
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'io.github.satoxyan.hyperpull.ReflectTest'`
Expected: PASS, 6 test.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/dev/syoxide/notifyfloat/Reflect.kt android/app/src/test
git commit -m "feat: add reflection helper with JVM tests"
```

---

### Task 3: Hook bar notifikasi normal

**Files:**
- Create: `android/app/src/main/kotlin/dev/syoxide/notifyfloat/NotificationMiniWindowHook.kt`
- Modify: `android/app/src/main/kotlin/dev/syoxide/notifyfloat/HyperPullModule.kt`

**Interfaces:**
- Consumes: `Reflect.setBoolean(target: Any, name: String, value: Boolean): Boolean`, `Reflect.invokeNoArg(target: Any, name: String): Any?`.
- Produces: `NotificationMiniWindowHook.install(module: XposedModule, classLoader: ClassLoader)`.

- [ ] **Step 1: Implementasikan hook**

`NotificationMiniWindowHook.kt`:

```kotlin
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
```

- [ ] **Step 2: Sambungkan dari entry point**

Ganti isi `onPackageLoaded` menjadi:

```kotlin
    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.android.systemui") return
        log(android.util.Log.INFO, "HyperPull", "loaded in ${param.packageName}")
        NotificationMiniWindowHook.install(this, param.defaultClassLoader)
    }
```

- [ ] **Step 3: Jalankan unit test**

Run: `cd android && ./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 4: Build APK**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/dev/syoxide/notifyfloat
git commit -m "feat: force notification mini-window handle for all rows"
```

---

### Task 4: README + protokol uji perangkat

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: APK dari Task 3.
- Produces: instruksi build/install/uji yang bisa dijalankan pengguna.

- [ ] **Step 1: Tulis README**

Isi wajib: tujuan satu paragraf, requirement (HyperOS 4, LSPosed, root), cara build
(`cd android && ./gradlew :app:assembleDebug`), cara install/aktifkan (scope hanya
System UI, reboot), protokol uji Telegram, perintah log
(`adb logcat -s HyperPull`), arti log `bar/canSlide/miniBarVisible`, batasan
(tidak menyentuh super island, app non-freeform bisa gagal, versi SystemUI bisa berubah),
dan cara uninstall.

- [ ] **Step 2: Verifikasi perintah di README cocok dengan struktur repo**

Run: `test -f android/gradlew && test -f android/app/src/main/resources/META-INF/xposed/module.prop && echo OK`
Expected: `OK`.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add build, install, and device test instructions"
```

---

### Task 5 (pengguna): verifikasi di perangkat

Tidak ada kode. Jalankan:

1. Install `app-debug.apk`.
2. Aktifkan **HyperPull** di LSPosed, scope hanya **System UI**, reboot.
3. Kirim notifikasi Telegram, buka notification shade, tarik bar kecil di bawah notifikasi.
4. Jika floating window tidak terbuka, ambil `adb logcat -s HyperPull` dan kirim hasilnya.

Expected sukses: Telegram terbuka dalam floating window. Jika gagal, log menunjukkan
apakah `canSlide`/`miniBarVisible` ditemukan; kegagalan di sini berarti perlu
pendekatan B (cari method kelayakan SystemUI).

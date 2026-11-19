package io.github.satoxyan.hyperpull

import java.lang.reflect.Field

internal object Reflect {

    fun findField(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            val field = runCatching {
                current.getDeclaredField(name).apply { isAccessible = true }
            }.getOrNull()
            if (field != null) return field
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

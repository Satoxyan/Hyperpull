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

    fun get(target: Any, name: String): Any? {
        val field = findField(target.javaClass, name) ?: return null
        return runCatching { field.get(target) }.getOrNull()
    }

    fun set(target: Any, name: String, value: Any?): Boolean {
        val field = findField(target.javaClass, name) ?: return false
        return runCatching { field.set(target, value) }.isSuccess
    }
}

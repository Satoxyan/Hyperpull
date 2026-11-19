package io.github.satoxyan.hyperpull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectTest {

    private open class Base {
        var inherited: Any? = "base"
    }

    private class Fake : Base() {
        var value: Any? = null
    }

    @Test
    fun getsField() {
        val fake = Fake().apply { value = "x" }
        assertEquals("x", Reflect.get(fake, "value"))
    }

    @Test
    fun setsField() {
        val fake = Fake()
        assertTrue(Reflect.set(fake, "value", "y"))
        assertEquals("y", fake.value)
    }

    @Test
    fun reachesInheritedField() {
        val fake = Fake()
        assertEquals("base", Reflect.get(fake, "inherited"))
        assertTrue(Reflect.set(fake, "inherited", "changed"))
        assertEquals("changed", fake.inherited)
    }

    @Test
    fun missingFieldReturnsNull() {
        assertNull(Reflect.get(Fake(), "doesNotExist"))
        assertFalse(Reflect.set(Fake(), "doesNotExist", "x"))
    }
}

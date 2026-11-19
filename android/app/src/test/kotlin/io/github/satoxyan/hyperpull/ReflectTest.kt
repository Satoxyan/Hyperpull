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

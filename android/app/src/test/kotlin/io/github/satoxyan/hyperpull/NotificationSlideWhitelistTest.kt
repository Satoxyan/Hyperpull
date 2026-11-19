package io.github.satoxyan.hyperpull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSlideWhitelistTest {

    @Test
    fun allowsAnyNonBlankPackage() {
        val whitelist = NotificationSlideWhitelist(listOf("com.whatsapp"))
        assertTrue(whitelist.contains("org.telegram.messenger"))
    }

    @Test
    fun rejectsBlankPackage() {
        assertFalse(NotificationSlideWhitelist(emptyList()).contains(""))
        assertFalse(NotificationSlideWhitelist(emptyList()).contains("   "))
    }

    @Test
    fun keepsOriginalEntriesAndOrder() {
        val whitelist = NotificationSlideWhitelist(listOf("com.whatsapp", "com.miui.home"))
        assertEquals(2, whitelist.size)
        assertEquals("com.whatsapp", whitelist[0])
        assertEquals("com.miui.home", whitelist[1])
    }

    @Test
    fun wrapsPlainListOnce() {
        val wrapped = wrapWhitelist(listOf("com.whatsapp"))
        assertTrue(wrapped is NotificationSlideWhitelist)
        assertNull(wrapWhitelist(wrapped))
    }

    @Test
    fun ignoresMissingOrWrongTypes() {
        assertNull(wrapWhitelist(null))
        assertNull(wrapWhitelist("com.whatsapp"))
    }
}

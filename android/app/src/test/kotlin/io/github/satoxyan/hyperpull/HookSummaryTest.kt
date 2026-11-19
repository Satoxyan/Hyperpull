package io.github.satoxyan.hyperpull

import org.junit.Assert.assertEquals
import org.junit.Test

class HookSummaryTest {

    @Test
    fun emptyMeansNotFound() {
        assertEquals("no injector methods found", hookSummary(emptyList()))
    }

    @Test
    fun listsHookedNames() {
        assertEquals(
            "hooked=updateMiniWindowBar,updateFreeformTipView",
            hookSummary(listOf("updateMiniWindowBar", "updateFreeformTipView")),
        )
    }
}

package io.github.satoxyan.hyperpull

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeformLimitHookTest {

    private fun frame(className: String, methodName: String) =
        StackTraceElement(className, methodName, null, 0)

    @Test
    fun matchesStackAddedPath() {
        val frames = arrayOf(
            frame("com.android.server.wm.MiuiFreeFormStackDisplayStrategy", "onMiuiFreeFormStasckAdded"),
        )
        assertTrue(FreeformLimitHook.isGameLimitCaller(frames))
    }

    @Test
    fun matchesGameLambdaPrefix() {
        val frames = arrayOf(
            frame(
                "com.android.server.wm.MiuiFreeFormManagerService",
                "lambda\$getBottomGameFreeFormActivityStack\$1",
            ),
        )
        assertTrue(FreeformLimitHook.isGameLimitCaller(frames))
    }

    @Test
    fun keepsForegroundPinAndUnrelatedCallers() {
        val frames = arrayOf(
            frame("com.android.server.wm.MiuiFreeFormStackDisplayStrategy", "needForegroundPin"),
            frame("com.android.server.wm.MiuiFreeFormManagerService", "getGameFreeFormCountExtra"),
        )
        assertFalse(FreeformLimitHook.isGameLimitCaller(frames))
    }
}

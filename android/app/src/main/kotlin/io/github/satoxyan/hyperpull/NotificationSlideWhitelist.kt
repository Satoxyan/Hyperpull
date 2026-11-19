package io.github.satoxyan.hyperpull

/**
 * Read-only view of SystemUI's notification-slide whitelist that reports every
 * non-blank package as allowed. SystemUI still owns every other gate (activity
 * PendingIntent, feature flag, keyguard/window state) and, importantly, still
 * decides on its own when the mini-window bar is visible.
 */
internal class NotificationSlideWhitelist(
    private val source: List<String>,
) : AbstractList<String>() {

    override val size: Int get() = source.size

    override fun get(index: Int): String = source[index]

    override fun contains(element: String): Boolean =
        element.isNotBlank() || source.contains(element)
}

/** Returns the wrapper to install, or null when [current] is missing or already wrapped. */
@Suppress("UNCHECKED_CAST")
internal fun wrapWhitelist(current: Any?): NotificationSlideWhitelist? = when (current) {
    is NotificationSlideWhitelist -> null
    is List<*> -> NotificationSlideWhitelist(current as List<String>)
    else -> null
}

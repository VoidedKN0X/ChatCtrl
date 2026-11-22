package com.vknxworks.chatctrl.matrix

/**
 * The rules for sending several files at once, kept apart from Android so they're easy to read and check.
 *
 * Matrix has one event per file, so a caption and a reply can only sit on one of them. Like Telegram's albums
 * (and Element X, which only offers a caption for a single file), the caption goes on the **first** file, and so
 * does the reply: the first message quotes the one you replied to, the rest follow it without repeating the quote.
 * In a thread every file goes into the thread, because they're all sent through the thread's own timeline.
 */
object AttachmentPlan {
    /** The caption for the file at [index]: only the first one gets it, and a blank caption is none at all. */
    fun captionFor(index: Int, caption: String?): String? =
        if (index == 0) caption?.trim()?.takeIf { it.isNotEmpty() } else null

    /** The event the file at [index] replies to: only the first one, so the quote isn't repeated N times. */
    fun replyFor(index: Int, replyTo: String?): String? = if (index == 0) replyTo else null

    /** Whether a file of [size] bytes is over the server's limit. Unknown sizes or limits never block a send. */
    fun tooBig(size: Long?, maxUploadSize: Long?): Boolean =
        size != null && maxUploadSize != null && maxUploadSize > 0 && size > maxUploadSize

    /**
     * Puts files picked or shared in one go into the list to send: in the order given, each only once, at most
     * [max]. Returns the list and how many were left out because there were more than [max].
     */
    fun <T> take(items: List<T>, max: Int): Pair<List<T>, Int> {
        val unique = items.distinct()
        val kept = unique.take(max.coerceAtLeast(0))
        return kept to (unique.size - kept.size)
    }

    /** Removes the item at [index] from the staged list, keeping the order of the rest. */
    fun <T> removeAt(items: List<T>, index: Int): List<T> =
        if (index !in items.indices) items else items.filterIndexed { i, _ -> i != index }

    /** "12.3 MB"-style size for the staging strip and messages (powers of 1000, like Android's file sizes). */
    fun formatSize(bytes: Long): String {
        if (bytes < 1000) return "$bytes B"
        val units = listOf("kB", "MB", "GB", "TB")
        var value = bytes / 1000.0
        var unit = 0
        while (value >= 1000 && unit < units.lastIndex) { value /= 1000; unit++ }
        val shown = if (value >= 100) "%.0f".format(java.util.Locale.ROOT, value) else "%.1f".format(java.util.Locale.ROOT, value)
        return "$shown ${units[unit]}"
    }
}

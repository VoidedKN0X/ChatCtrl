package com.vknxworks.chatctrl.ui.location

/** A point on the map, with how sure its sender was of it (metres), when their geo: URI says. */
data class GeoSpot(val lat: Double, val lon: Double, val accuracy: Float? = null)

/**
 * Reads a geo: URI (RFC 5870), such as `geo:50.85,4.35;u=20`. Only coordinates are taken from someone else's
 * event, never anything else in it.
 */
fun parseGeoUri(uri: String): GeoSpot? {
    val t = uri.trim()
    if (!t.startsWith("geo:", ignoreCase = true)) return null
    val main = t.substring(4).substringBefore(';').substringBefore('?')
    val parts = main.split(',')
    if (parts.size < 2) return null
    val lat = parts[0].trim().toDoubleOrNull() ?: return null
    val lon = parts[1].trim().toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    val u = Regex(";u=([0-9.]+)", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)?.toFloatOrNull()
    return GeoSpot(lat, lon, u)
}

/** A geo: URI for sending a picked spot: six decimals (about 10 cm). */
fun GeoSpot.toGeoUri(): String =
    "geo:" + "%.6f".format(java.util.Locale.ROOT, lat) + "," + "%.6f".format(java.util.Locale.ROOT, lon)

fun GeoSpot.display(): String =
    "%.5f".format(java.util.Locale.ROOT, lat) + ", " + "%.5f".format(java.util.Locale.ROOT, lon)

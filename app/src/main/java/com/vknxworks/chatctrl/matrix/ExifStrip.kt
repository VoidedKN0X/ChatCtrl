package com.vknxworks.chatctrl.matrix

import android.media.ExifInterface
import java.io.File

/**
 * Removes where and when a photo was taken (and the camera's serial number and owner name) from a copy of
 * a picture before it is sent. Android's photo picker already does this; pictures that come through
 * "File" or are shared to ChatCtrl from a camera, gallery or file manager kept them.
 *
 * Uses the platform's own [android.media.ExifInterface] (no extra library). Only JPEG, PNG and WebP can be
 * rewritten in place; HEIC/HEIF and videos are sent as they are (see the README). The picture itself, its
 * orientation and its other details are left alone.
 */
object ExifStrip {
    /** What [strip] did. */
    enum class Result { NOT_SUPPORTED, NOTHING_TO_STRIP, STRIPPED, FAILED }

    /** Mime types the platform ExifInterface can save in place. */
    private val WRITABLE = setOf("image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/webp")

    fun supports(mime: String): Boolean = mime.lowercase().substringBefore(';').trim() in WRITABLE

    /** Every GPS tag, the capture time, and the details that identify the camera or its owner. */
    val TAGS: List<String> = listOf(
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DIFFERENTIAL,
        ExifInterface.TAG_GPS_DOP,
        "GPSHPositioningError",
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
    )

    /**
     * Strips [TAGS] from [file] in place. The file is only rewritten when it has one of them. Never throws:
     * [Result.FAILED] means the file may not be usable any more and should be copied again from its source.
     */
    fun strip(file: File, mime: String): Result {
        if (!supports(mime)) return Result.NOT_SUPPORTED
        return try {
            val exif = ExifInterface(file.absolutePath)
            val present = TAGS.filter { exif.getAttribute(it) != null }
            if (present.isEmpty()) return Result.NOTHING_TO_STRIP
            present.forEach { exif.setAttribute(it, null) }
            exif.saveAttributes()
            // Check it really is gone (a format the platform reads but can't write would keep it).
            val after = ExifInterface(file.absolutePath)
            if (TAGS.any { after.getAttribute(it) != null }) Result.FAILED else Result.STRIPPED
        } catch (e: Exception) {
            Result.FAILED
        } catch (e: OutOfMemoryError) {
            Result.FAILED
        }
    }
}

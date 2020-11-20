@file:Suppress("UNCHECKED_CAST")
package com.example.week3.exifTool
//import android.media.ExifInterface
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap


//TODO: catch exceptions
private fun ExifInterface.mAttributes(): Any {
    val mAttributesField = this.javaClass.getDeclaredField("mAttributes")
    mAttributesField.isAccessible = true
    return mAttributesField.get(this)
}

/**
 * Created on 12/22/16.
 */
fun ExifInterface.getTags(): HashMap<String, String?> {
    val mAttributes = mAttributes()
    var map = HashMap<String, String?>()
    if (mAttributes is Array<*>) {
        val arrayOfMapAux = mAttributes as Array<HashMap<String, *>>
        arrayOfMapAux.indices
                .flatMap { mAttributes[it].entries }
                .forEach { map[it.key] = this.getAttribute(it.key) }
    } else if (mAttributes is HashMap<*, *>) {
        map = mAttributes as HashMap<String, String?>
    }

    val latLonArray = FloatArray(2)
    if (this.getLatLong(latLonArray)) {
        map["EXIF_LATITUDE"] = latLonArray[0].toString()
        map["EXIF_LONGITUDE"] = latLonArray[1].toString()
    }
    return map
}

fun ExifInterface.removeAllTags(onSuccess: () -> Unit,
                                onFailure: (Throwable) -> Unit) {
    try {
        val mAttributes = mAttributes()

        if (mAttributes is Array<*>) {
            val arrayOfMapAux = mAttributes as Array<HashMap<String, *>>
            arrayOfMapAux.forEach { map -> map.clear() }

        } else if (mAttributes is HashMap<*, *>) {
            val map = mAttributes as HashMap<String, String>
            map.clear()
        }
        this.saveAttributes()
        onSuccess()
    } catch (e: IOException) {
        onFailure(e)
    }
}

/**
 * Ok, this is very tricky
 */
fun ExifInterface.removeTags(tags: Set<String>,
                             onSuccess: () -> Unit,
                             onFailure: (Throwable) -> Unit) {
    try {
        val mAttributes = mAttributes()

        if (mAttributes is Array<*>) {
            val arrayOfMapAux = mAttributes as Array<HashMap<String, *>>
            arrayOfMapAux.forEach { map ->
                map.keys.filter { it in tags }
                        .forEach { key -> map.remove(key) }
            }

        } else if (mAttributes is HashMap<*, *>) {
            val map = mAttributes as HashMap<String, String>
            map.keys.filter { it in tags }
                    .forEach { map.remove(it) }
        }
        this.saveAttributes()
        onSuccess()
    } catch (e: IOException) {
        onFailure(e)
    }
}

fun ExifInterface.convertDecimalToDegrees(decimal: Double): String {
    var latitude = Math.abs(decimal)
    val degree = latitude.toInt()
    latitude *= 60
    latitude -= (degree * 60.0)
    val minute = latitude.toInt()
    latitude *= 60
    latitude -= (minute * 60.0)
    val second = (latitude * 1000.0).toInt()
    return "$degree/1,$minute/1,$second/1000"
}

fun ExifInterface.getLatitudeRef(latitude: Double): String =
        if (latitude < 0.0) "S" else "N"

fun ExifInterface.getLongitudeRef(longitude: Double): String =
        if (longitude < 0.0) "W" else "E"


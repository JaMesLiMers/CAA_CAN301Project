package com.example.week3.exifTool

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*


/**
 * Created on 12/22/16.
 */
data class ExifTagsContainer(val list: List<ExifField>, val type: Type) {
    fun getOnStringProperties(): String = when {
        list.isEmpty() -> "No data provided"
        else -> {
            val s = StringBuilder()
            list.forEach { s.append("${it.tag}: ${it.attribute}\n") }
            s.toString().substring(0, s.length - 1)
        }
    }
}

enum class Type { GPS, DATE, CAMERA_PROPERTIES, DIMENSION, OTHER }
data class ExifField(val tag: String, val attribute: String?)

data class Location(val latitude: Double, val longitude: Double)
object Constant{val EXIF_LATITUDE= "Latitude"
    val EXIF_LONGITUDE = "Longitude"}

class PhotoDetailPresenter(){

    lateinit var exifTagsList: List<ExifTagsContainer>
    lateinit var exifInterface: ExifInterface
    lateinit var filePath: String

    var latitude: Double? = null
    var longitude: Double? = null

    lateinit var imageUri: Uri
    lateinit var file:File
    fun initialize(intent: Intent) {
        this.filePath = intent.getStringExtra("path_file").toString()
        this.computeTags() // 得到tag

        this.setImageByGivenAPath() // 得到 file imageuri 还有 filepath等
        //populateExifProperties() //直接return
        //getAddressByTriggerRequest()
    }

    fun initialize(str: String) {
        this.filePath = str
        this.computeTags() // 得到tag

        this.setImageByGivenAPath() // 得到 file imageuri 还有 filepath等
        //populateExifProperties() //直接return
        //getAddressByTriggerRequest()
    }

    private fun computeTags() {
        exifInterface = ExifInterface(filePath)
        val map = exifInterface.getTags()
        exifTagsList = transformList(map) //拿到list
        latitude = map["EXIF_LATITUDE"]?.toDouble()
        longitude = map["EXIF_LONGITUDE"]?.toDouble()
    }

//    private fun populateExifProperties() {
//        view.setExifDataList(exifTagsList)
//    }

    private fun setImageByGivenAPath() {
        //Log.d(this.javaClass.simpleName, filePath)
        this.imageUri = Uri.fromFile(File(filePath))
        this.file = File(filePath)
        //view.setImage(file.name, file.getSize(), imageUri)
    }



    private fun transformList(map: HashMap<String, String?>): List<ExifTagsContainer> {
        val locationsList = arrayListOf<ExifField>()
        val gpsList = arrayListOf<ExifField>()
        val datesList = arrayListOf<ExifField>()
        val cameraPropertiesList = arrayListOf<ExifField>()
        val dimensionsList = arrayListOf<ExifField>()
        val othersList = arrayListOf<ExifField>()
        map.forEach {
            when (it.key) {
                Constant.EXIF_LATITUDE
                    , Constant.EXIF_LONGITUDE ->
                    locationsList.add(ExifField(it.key, it.value))
                ExifInterface.TAG_DATETIME
                    , ExifInterface.TAG_DATETIME_DIGITIZED ->
                    datesList.add(ExifField(it.key, it.value))
                ExifInterface.TAG_MAKE
                    , ExifInterface.TAG_MODEL ->
                    cameraPropertiesList.add(ExifField(it.key, it.value))
                ExifInterface.TAG_IMAGE_LENGTH
                    , ExifInterface.TAG_IMAGE_WIDTH ->
                    dimensionsList.add(ExifField(it.key, it.value))
                else -> {
                    if (it.key.contains("GPS")) gpsList.add(ExifField(it.key, it.value))
                    else othersList.add(ExifField(it.key, it.value))
                }
            }
        }
        locationsList.addAll(gpsList)
        return arrayListOf(ExifTagsContainer(locationsList, Type.GPS),
                ExifTagsContainer(datesList, Type.DATE),
                ExifTagsContainer(cameraPropertiesList, Type.CAMERA_PROPERTIES),
                ExifTagsContainer(dimensionsList, Type.DIMENSION),
                ExifTagsContainer(othersList, Type.OTHER))
    }

    private fun appendZeroIfNeeded(n: Int): String {
        val s = n.toString()
        if (s.length == 1)
            return "0$s"
        else
            return s
    }

//    //TODO: refactor and clean code.
//    fun changeExifDate(year: Int, month: Int, dayOfMonth: Int) {
//        val locationExifContainerList = exifTagsList.find { it.type == Type.DATE }?.list!!
//        val dateTimeShort: String
//        val dateTimeLong: String
//
//        if (locationExifContainerList.isEmpty()) {
//            val df = SimpleDateFormat("HH:mm:ss")
//            val calendar = Calendar.getInstance()
//            dateTimeLong = "$year:${appendZeroIfNeeded(month)}:${appendZeroIfNeeded(dayOfMonth)} ${df.format(calendar.time)}"
//            dateTimeShort = ""
//        } else {
//            val auxListLong = mutableListOf<ExifField>()
//            locationExifContainerList.forEach {
//                if (it.attribute.length > 10) auxListLong.add(it)
//            }
//            val actualDate = auxListLong.first().attribute.substring(11)
//            dateTimeLong = "$year:${appendZeroIfNeeded(month)}:${appendZeroIfNeeded(dayOfMonth)} $actualDate"
//            dateTimeShort = "$year:${appendZeroIfNeeded(month)}:${appendZeroIfNeeded(dayOfMonth)}"
//        }
//        try {
//            if (locationExifContainerList.isEmpty()) {
//                exifInterface.setAttribute(android.media.ExifInterface.TAG_DATETIME, dateTimeLong)
//            } else {
//                locationExifContainerList.forEach {
//                    if (it.attribute.length > 10)
//                        exifInterface.setAttribute(it.tag, dateTimeLong)
//                    else
//                        exifInterface.setAttribute(it.tag, dateTimeShort)
//                }
//            }
//            exifInterface.saveAttributes()
//
//            computeTags()
//            view.changeExifDataList(exifTagsList)
//
//            view.onCompleteDateChanged()
//            Log.d(this.javaClass.simpleName, "Date was changed: year: $year  month: $month day: $dayOfMonth")
//        } catch (e: IOException) {
//            Log.e(this.javaClass.simpleName, "${e.cause} - ${e.message}")
//            view.onError(view.getContext().resources.getString(R.string.date_changed_message_error))
//        }
//    }
    fun setExifDate(datetime: String){
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME, datetime)
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, datetime)
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, datetime)
        try {
            exifInterface.saveAttributes() //最后保存起来
        } catch (e: IOException) {
            Log.e("saveError", "Cannot save EXIF", e)
        }
        computeTags()
    }

    fun removeExifDate(){
        val set: MutableSet<String> = HashSet(listOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_DATETIME_ORIGINAL
                ))
        exifInterface.removeTags(set, {}, {})
    }

    fun setExifCamera(make: String, model: String){
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, make)
        exifInterface.setAttribute(ExifInterface.TAG_MODEL, model)
        try {
            exifInterface.saveAttributes() //最后保存起来
        } catch (e: IOException) {
            Log.e("saveError", "Cannot save EXIF", e)
        }
//        computeTags()
    }

    fun removeExifCamera(){
        val set: MutableSet<String> = HashSet(listOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL
        ))
        exifInterface.removeTags(set, {}, {})
    }


//    public fun setExifGPS(latitude: Double, longitude: Double){
//
//        // Todo：
//        // "确定" 时：干些什么
//        // 两样东西存在lat 和 lon里面
//        // 需要设置和更新
////        val strLongitude: String = convert(longitude, FORMAT_SECONDS)
////        val strLatitude: String = convert(latitude, FORMAT_SECONDS)
//        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitude.toString())
//        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitude.toString())
//        try {
//            exifInterface.saveAttributes()
//        }catch (e: IOException){
//            Log.e("pa", "pa", e)
//        }
//    }
    
    fun removeExifGPS(){
        val set: MutableSet<String> = HashSet(listOf(
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
                ExifInterface.TAG_GPS_VERSION_ID))
        exifInterface.removeTags(set, {}, {})
    }

    fun setExifGPS(latitude: Double, longitude: Double): Boolean {
        removeExifGPS()
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convert(latitude))
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitudeRef(latitude))
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convert(longitude))
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitudeRef(longitude))
        exifInterface.saveAttributes()
        return true
    }

    /**
     * convert latitude into DMS (degree minute second) format. For instance<br></br>
     * -79.948862 becomes<br></br>
     * 79/1,56/1,55903/1000<br></br>
     * It works for latitude and longitude<br></br>
     * @param latitude could be longitude.
     * @return
     */
    private fun convert(latitude: Double): String {
        var latitude = latitude
        latitude = Math.abs(latitude)
        val degree = latitude.toInt()
        latitude *= 60.0
        latitude -= degree * 60.0
        val minute = latitude.toInt()
        latitude *= 60.0
        latitude -= minute * 60.0
        val second = (latitude * 1000.0).toInt()
        val sb = java.lang.StringBuilder(20)
        sb.append(degree)
        sb.append("/1,")
        sb.append(minute)
        sb.append("/1,")
        sb.append(second)
        sb.append("/1000")
        return sb.toString()
    }

    private fun createDebugStringBuilder(filePath: File): java.lang.StringBuilder? {
        return java.lang.StringBuilder("Set Exif to file='").append(filePath.absolutePath).append("'\n\t")
    }

    private fun latitudeRef(latitude: Double): String? {
        return if (latitude < 0.0) "S" else "N"
    }

    private fun longitudeRef(longitude: Double): String? {
        return if (longitude < 0.0) "W" else "E"
    }


}
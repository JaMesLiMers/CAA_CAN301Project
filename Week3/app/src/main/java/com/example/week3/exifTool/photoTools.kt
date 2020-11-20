package com.example.week3.exifTool

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.example.week3.R


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

    private fun computeTags() {
        exifInterface = ExifInterface(filePath)
        val map = exifInterface.getTags()
        exifTagsList = transformList(map) //拿到list
        latitude = map[Constant.EXIF_LATITUDE]?.toDouble()
        longitude = map[Constant.EXIF_LONGITUDE]?.toDouble()
    }

//    private fun populateExifProperties() {
//        view.setExifDataList(exifTagsContainerList)
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


}
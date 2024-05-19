package com.example.melon.data.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import retrofit2.HttpException
import java.io.File

//class FileRepository {
//    suspend fun uploadImage(file: File, latitude: Double, longitude: Double): Boolean{
//        val latitudeRequestBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
//        val longitudeRequestBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
//
//        return try{
//            ApiService.instance.uploadImage(
//                file = MultipartBody.Part.createFormData(
//                        "image",
//                        file.name,
//                        file.asRequestBody("image/jpeg".toMediaTypeOrNull())),
//                latitude = latitudeRequestBody,
//                longitude = longitudeRequestBody
//            )
//            true
//        }
//        catch (e:IOException){
//            Log.e("FileRepository", "IOException: ${e.message}")
//            false
//        } catch (e: HttpException){
//            Log.e("FileRepository", "HttpException: ${e.code()}")
//            false
//        }
//    }
//}

class FileRepository {
    suspend fun uploadImage(file: File, latitude: Double, longitude: Double): Boolean {
        val latitudeRequestBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val longitudeRequestBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        return try {
            val response = ApiService.instance.uploadImage(
                file = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())),
                latitude = latitudeRequestBody,
                longitude = longitudeRequestBody
            )

            // Log the response details
            Log.d("FileRepository", "Upload Success: ${response}")

            true
        } catch (e: IOException) {
            Log.e("FileRepository", "IOException: ${e.message}")
            false
        } catch (e: HttpException) {
            Log.e("FileRepository", "HttpException: ${e.code()} - ${e.message()}")
            false
        }
    }
}

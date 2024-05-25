package com.example.melon.data.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import retrofit2.HttpException
import java.io.File

class FileRepository {
    private val apiService = ApiConfig.getApiService()
    suspend fun uploadImage(file: File, latitude: Double, longitude: Double): Boolean{
        val latitudeRequestBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val longitudeRequestBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        return try{
            val response = apiService.uploadImage(
                file = MultipartBody.Part.createFormData(
                        "file",
                        file.name,
                        file.asRequestBody("image/jpg".toMediaTypeOrNull())),
                latitude = latitudeRequestBody,
                longitude = longitudeRequestBody
            )
            Log.d("FileRepository", "Upload success: ${response}")
            true
        }
        catch (e:IOException){
            Log.e("FileRepository", "IOException: ${e.message}")
            false
        } catch (e: HttpException){
            Log.e("FileRepository", "HttpException: ${e.code()}")
            false
        }
    }
}
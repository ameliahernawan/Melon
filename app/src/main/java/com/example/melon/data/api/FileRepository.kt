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
    suspend fun uploadImage(file: File, latitude: Double, longitude: Double): Boolean{
        return try{
            val latitudeRequestBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudeRequestBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val response = ApiService.instance.uploadImage (
                file = MultipartBody.Part.createFormData(
                        "image",
                        file.name,
                        file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                ),
                latitude = latitudeRequestBody,
                longitude = longitudeRequestBody
            )
            Log.d("FileRepository", "Response: $response")
            // Check if response is successful
            if (response.success) {
                // Do something with successful response, if needed
                true
            } else {
                // Log error if response is not successful
                Log.e("FileRepository", "Failed to upload image: $response")
                false
            }
        } catch (e:IOException){
            Log.e("FileRepository", "IOException: ${e.message}")
            false
        } catch (e: HttpException){
            Log.e("FileRepository", "HttpException: ${e.code()}")
            false
        }
    }
}
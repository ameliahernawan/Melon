package com.example.melon.data.api

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class FileViewModel(application: Application): AndroidViewModel(application) {
    private val repository = FileRepository()

    private val _uploadResult = MutableLiveData<Boolean>()
    val uploadResult: LiveData<Boolean> get() = _uploadResult
    fun uploadImage(file: File, latitude: Double, longitude: Double){
        viewModelScope.launch{
            val result = repository.uploadImage(file, latitude, longitude)
            _uploadResult.postValue(result)
        }
    }
}
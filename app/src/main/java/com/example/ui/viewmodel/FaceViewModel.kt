package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.FriendWithPhotos
import com.example.data.api.FaceMatchResult
import com.example.data.repository.FriendRepository
import com.example.data.service.FaceRecognitionService
import com.example.utils.SampleFaceGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FaceViewModel(
    application: Application,
    private val repository: FriendRepository,
    private val faceRecognitionService: FaceRecognitionService
) : AndroidViewModel(application) {

    val allFriends: StateFlow<List<FriendWithPhotos>> = repository.allFriends
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _queryPhoto = MutableStateFlow<ByteArray?>(null)
    val queryPhoto: StateFlow<ByteArray?> = _queryPhoto.asStateFlow()

    private val _scanResult = MutableStateFlow<FaceMatchResult?>(null)
    val scanResult: StateFlow<FaceMatchResult?> = _scanResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setQueryPhoto(bytes: ByteArray?) {
        _queryPhoto.value = bytes
        _scanResult.value = null
        _errorMessage.value = null
    }

    fun addFriend(name: String, socialMedia: String, photoBytes: List<ByteArray>) {
        viewModelScope.launch {
            try {
                repository.insertFriend(name, socialMedia, photoBytes)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Arkadaş eklenirken hata: ${e.message}"
            }
        }
    }

    fun addPhotoToFriend(friendId: Long, photoBytes: ByteArray) {
        viewModelScope.launch {
            try {
                repository.addPhotoToFriend(friendId, photoBytes)
            } catch (e: Exception) {
                _errorMessage.value = "Fotoğraf eklenirken hata: ${e.message}"
            }
        }
    }

    fun deleteFriend(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteFriend(id)
            } catch (e: Exception) {
                _errorMessage.value = "Silinirken hata: ${e.message}"
            }
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            try {
                repository.deletePhoto(photoId)
            } catch (e: Exception) {
                _errorMessage.value = "Fotoğraf silinirken hata: ${e.message}"
            }
        }
    }

    fun runFaceRecognition() {
        val query = _queryPhoto.value
        if (query == null) {
            _errorMessage.value = "Lütfen önce taranacak bir fotoğraf seçin veya çekin."
            return
        }

        val friends = allFriends.value
        if (friends.isEmpty()) {
            _errorMessage.value = "Kayıtlı arkadaş bulunamadı. Lütfen önce arkadaş ekleyin."
            return
        }

        _isScanning.value = true
        _scanResult.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = faceRecognitionService.recognizeFace(query, friends)
                _scanResult.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Tanıma işlemi başarısız: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun loadSampleFriends() {
        viewModelScope.launch {
            try {
                // Generate 3 sample friends
                val ahmetPhoto = SampleFaceGenerator.generateFace("Ahmet", Color.parseColor("#1976D2"), hasGlasses = true, hasHat = false, hasMustache = false)
                val aysePhoto = SampleFaceGenerator.generateFace("Ayşe", Color.parseColor("#C2185B"), hasGlasses = false, hasHat = true, hasMustache = false)
                val canPhoto = SampleFaceGenerator.generateFace("Can", Color.parseColor("#FF8F00"), hasGlasses = false, hasHat = false, hasMustache = true)

                repository.insertFriend("Ahmet Yılmaz", "@ahmet_ylmz", listOf(ahmetPhoto))
                repository.insertFriend("Ayşe Demir", "@ayse_dmr", listOf(aysePhoto))
                repository.insertFriend("Can Kaya", "@can_kaya", listOf(canPhoto))
                
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Örnekler yüklenirken hata: ${e.message}"
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FaceViewModel::class.java)) {
                val database = AppDatabase.getDatabase(application)
                val repository = FriendRepository(database.friendDao())
                val service = FaceRecognitionService()
                @Suppress("UNCHECKED_CAST")
                return FaceViewModel(application, repository, service) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

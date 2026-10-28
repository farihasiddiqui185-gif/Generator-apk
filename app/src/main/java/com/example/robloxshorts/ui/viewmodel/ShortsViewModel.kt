package com.example.robloxshorts.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.robloxshorts.data.local.AppDatabase
import com.example.robloxshorts.data.local.entity.VideoEntity
import com.example.robloxshorts.data.model.BulkGenerationParams
import com.example.robloxshorts.data.model.ShortsCategory
import com.example.robloxshorts.data.model.VideoDuration
import com.example.robloxshorts.data.model.VideoLanguage
import com.example.robloxshorts.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiNotification(
    val message: String,
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

class ShortsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = VideoRepository(application, db.videoDao())

    val allVideos: StateFlow<List<VideoEntity>> = repository.getAllVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGeneratingCount: StateFlow<Int> = allVideos.map { list ->
        list.count { it.status == "QUEUED" || it.status == "SCRIPTING" || it.status == "RENDERING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedVideos: StateFlow<List<VideoEntity>> = allVideos.map { list ->
        list.filter { it.status == "COMPLETED" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(0) // 0: Home, 1: Create, 2: Gallery, 3: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _generationParams = MutableStateFlow(BulkGenerationParams())
    val generationParams: StateFlow<BulkGenerationParams> = _generationParams.asStateFlow()

    private val _selectedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedVideoIds: StateFlow<Set<String>> = _selectedVideoIds.asStateFlow()

    private val _activePlayingVideo = MutableStateFlow<VideoEntity?>(null)
    val activePlayingVideo: StateFlow<VideoEntity?> = _activePlayingVideo.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    // API Key settings stored in SharedPreferences
    private val prefs = application.getSharedPreferences("roblox_shorts_prefs", Context.MODE_PRIVATE)
    private val _apiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun updateParams(update: (BulkGenerationParams) -> BulkGenerationParams) {
        _generationParams.value = update(_generationParams.value)
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
        prefs.edit().putString("gemini_api_key", key).apply()
        showNotification("API Key updated successfully!", isSuccess = true)
    }

    fun startBulkGeneration() {
        val params = _generationParams.value
        viewModelScope.launch {
            val key = _apiKey.value.takeIf { it.isNotBlank() }
            repository.enqueueBulkGeneration(params, key)
            showNotification("Enqueued ${params.quantity} video(s) for rendering!", isSuccess = true)
            _currentTab.value = 2 // Switch to Gallery to watch live progress
        }
    }

    fun quickGenerate(quantity: Int) {
        _generationParams.value = _generationParams.value.copy(quantity = quantity)
        startBulkGeneration()
    }

    fun retryVideo(id: String) {
        viewModelScope.launch {
            val key = _apiKey.value.takeIf { it.isNotBlank() }
            repository.retryVideo(id, key)
            showNotification("Retrying video generation...", isSuccess = true)
        }
    }

    fun deleteVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
            showNotification("Video deleted", isSuccess = true)
        }
    }

    fun toggleSelectVideo(id: String) {
        val current = _selectedVideoIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedVideoIds.value = current
    }

    fun selectAllCompleted() {
        _selectedVideoIds.value = completedVideos.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedVideoIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selectedVideoIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteVideosByIds(ids)
            _selectedVideoIds.value = emptySet()
            showNotification("Deleted ${ids.size} videos", isSuccess = true)
        }
    }

    fun saveToPhoneGallery(video: VideoEntity) {
        viewModelScope.launch {
            showNotification("Saving to Phone Gallery...", isSuccess = true)
            val result = repository.saveToGallery(video)
            result.onSuccess {
                showNotification("SAVED TO GALLERY! Check your Movies/Gallery app.", isSuccess = true)
            }.onFailure { err ->
                showNotification("Failed to save: ${err.message}", isSuccess = false)
            }
        }
    }

    fun downloadMp4(video: VideoEntity) {
        viewModelScope.launch {
            showNotification("Exporting MP4 to Downloads folder...", isSuccess = true)
            val result = repository.saveToDownloads(video)
            result.onSuccess { file ->
                showNotification("DOWNLOAD SUCCESS: Saved to ${file.name}", isSuccess = true)
            }.onFailure { err ->
                showNotification("Download failed: ${err.message}", isSuccess = false)
            }
        }
    }

    fun shareVideo(video: VideoEntity) {
        repository.gallerySaver.shareVideo(video)
    }

    fun exportSelectedAsZip() {
        val selected = completedVideos.value.filter { _selectedVideoIds.value.contains(it.id) }
        if (selected.isEmpty()) {
            showNotification("Please select completed videos first", isSuccess = false)
            return
        }
        viewModelScope.launch {
            showNotification("Creating ZIP archive with ${selected.size} videos...", isSuccess = true)
            val result = repository.exportBatchAsZip(selected)
            result.onSuccess { zipFile ->
                showNotification("ZIP Created! Opening share sheet...", isSuccess = true)
                repository.gallerySaver.shareZip(zipFile)
            }.onFailure { err ->
                showNotification("ZIP Export failed: ${err.message}", isSuccess = false)
            }
        }
    }

    fun exportAllAsZip() {
        val completed = completedVideos.value
        if (completed.isEmpty()) {
            showNotification("No completed videos available yet to export", isSuccess = false)
            return
        }
        viewModelScope.launch {
            showNotification("Packaging all ${completed.size} videos into ZIP...", isSuccess = true)
            val result = repository.exportBatchAsZip(completed)
            result.onSuccess { zipFile ->
                showNotification("ZIP Ready (${(zipFile.length() / 1024 / 1024)}MB)! Opening share sheet...", isSuccess = true)
                repository.gallerySaver.shareZip(zipFile)
            }.onFailure { err ->
                showNotification("ZIP Export failed: ${err.message}", isSuccess = false)
            }
        }
    }

    fun playVideo(video: VideoEntity) {
        _activePlayingVideo.value = video
    }

    fun closePlayer() {
        _activePlayingVideo.value = null
    }

    fun clearNotification() {
        _notification.value = null
    }

    private fun showNotification(message: String, isSuccess: Boolean) {
        _notification.value = UiNotification(message, isSuccess)
    }
}

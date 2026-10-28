package com.example.robloxshorts.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.robloxshorts.data.local.dao.VideoDao
import com.example.robloxshorts.data.local.entity.VideoEntity
import com.example.robloxshorts.data.model.BulkGenerationParams
import com.example.robloxshorts.data.model.ShortsCategory
import com.example.robloxshorts.data.model.VideoDuration
import com.example.robloxshorts.data.model.VideoLanguage
import com.example.robloxshorts.engine.GallerySaver
import com.example.robloxshorts.engine.RobloxAudioSynthesizer
import com.example.robloxshorts.engine.RobloxStoryGenerator
import com.example.robloxshorts.engine.RobloxVideoRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

class VideoRepository(
    private val context: Context,
    private val videoDao: VideoDao
) {
    private val storyGenerator = RobloxStoryGenerator(context)
    private val audioSynthesizer = RobloxAudioSynthesizer(context)
    private val videoRenderer = RobloxVideoRenderer(context)
    val gallerySaver = GallerySaver(context)

    private val queueMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isQueueWorkerRunning = false

    fun getAllVideos(): Flow<List<VideoEntity>> = videoDao.getAllVideos()

    suspend fun getVideoById(id: String): VideoEntity? = videoDao.getVideoById(id)

    suspend fun enqueueBulkGeneration(
        params: BulkGenerationParams,
        apiKey: String?
    ): List<String> {
        val createdIds = mutableListOf<String>()
        val entities = mutableListOf<VideoEntity>()

        for (i in 0 until params.quantity) {
            val id = UUID.randomUUID().toString()
            createdIds.add(id)

            val titleSuffix = if (params.quantity > 1) " #${i + 1}" else ""
            val entity = VideoEntity(
                id = id,
                title = "${params.category.title}$titleSuffix",
                category = params.category.title,
                durationSeconds = params.duration.seconds,
                language = params.language.code,
                status = "QUEUED",
                progress = 0.0f,
                progressMessage = "Waiting in queue...",
                videoPath = null,
                thumbnailPath = null,
                fileSizeBytes = 0L,
                createdAt = System.currentTimeMillis() + i * 10L,
                errorMessage = null,
                isSavedToGallery = false,
                scriptJson = ""
            )
            entities.add(entity)
        }

        videoDao.insertVideos(entities)
        triggerQueueProcessing(apiKey)
        return createdIds
    }

    suspend fun retryVideo(id: String, apiKey: String?) {
        val video = videoDao.getVideoById(id) ?: return
        videoDao.updateProgress(id, "QUEUED", 0f, "Retrying generation...")
        triggerQueueProcessing(apiKey)
    }

    private fun triggerQueueProcessing(apiKey: String?) {
        scope.launch {
            queueMutex.withLock {
                if (isQueueWorkerRunning) return@withLock
                isQueueWorkerRunning = true
            }

            try {
                processQueue(apiKey)
            } finally {
                queueMutex.withLock {
                    isQueueWorkerRunning = false
                }
            }
        }
    }

    private suspend fun processQueue(apiKey: String?) {
        while (true) {
            val pendingVideos = videoDao.getQueuedVideos()
            if (pendingVideos.isEmpty()) break

            val current = pendingVideos.first()
            val category = ShortsCategory.values().firstOrNull { it.title == current.category }
                ?: ShortsCategory.HORROR
            val duration = VideoDuration.values().firstOrNull { it.seconds == current.durationSeconds }
                ?: VideoDuration.SEC_30
            val language = VideoLanguage.values().firstOrNull { it.code == current.language }
                ?: VideoLanguage.EN

            try {
                // 1. Scripting phase
                videoDao.updateProgress(current.id, "SCRIPTING", 0.05f, "Writing viral Roblox script with Gemini AI...")
                val script = storyGenerator.generateScript(
                    category = category,
                    duration = duration,
                    language = language,
                    customPrompt = "",
                    apiKey = apiKey
                )

                // 2. Synthesizing Audio
                videoDao.updateProgress(current.id, "GENERATING_SCENES", 0.20f, "Synthesizing dynamic gaming music & sound effects...")
                val tempAudioFile = File(context.cacheDir, "audio_${current.id}.wav")
                audioSynthesizer.synthesizeAudio(script, tempAudioFile)

                // 3. Rendering video frames & subtitles
                videoDao.updateProgress(current.id, "RENDERING", 0.25f, "Rendering 3D Roblox scenes & subtitles...")
                val (videoFile, thumbFile) = videoRenderer.renderVideo(
                    script = script,
                    audioWavFile = tempAudioFile,
                    onProgress = { progressFraction, message ->
                        scope.launch {
                            videoDao.updateProgress(current.id, "RENDERING", progressFraction, message)
                        }
                    }
                )

                tempAudioFile.delete()

                // 4. Mark Completed
                val completedEntity = current.copy(
                    title = script.title,
                    status = "COMPLETED",
                    progress = 1.0f,
                    progressMessage = "Render Complete!",
                    videoPath = videoFile.absolutePath,
                    thumbnailPath = thumbFile.absolutePath,
                    fileSizeBytes = videoFile.length(),
                    scriptJson = script.toJson(),
                    errorMessage = null
                )
                videoDao.updateVideo(completedEntity)

            } catch (e: Exception) {
                Log.e("VideoRepo", "Failed to render video ${current.id}", e)
                val failedEntity = current.copy(
                    status = "FAILED",
                    progress = 0f,
                    progressMessage = "Generation Failed",
                    errorMessage = e.localizedMessage ?: "Unknown rendering failure"
                )
                videoDao.updateVideo(failedEntity)
            }
        }
    }

    suspend fun deleteVideo(video: VideoEntity) {
        video.videoPath?.let { File(it).delete() }
        video.thumbnailPath?.let { File(it).delete() }
        videoDao.deleteVideo(video)
    }

    suspend fun deleteVideosByIds(ids: List<String>) {
        ids.forEach { id ->
            val v = videoDao.getVideoById(id)
            v?.videoPath?.let { File(it).delete() }
            v?.thumbnailPath?.let { File(it).delete() }
        }
        videoDao.deleteVideosByIds(ids)
    }

    suspend fun saveToGallery(video: VideoEntity): Result<Uri> {
        val result = gallerySaver.saveVideoToGallery(video)
        if (result.isSuccess) {
            videoDao.markSavedToGallery(video.id)
        }
        return result
    }

    suspend fun saveToDownloads(video: VideoEntity): Result<File> {
        return gallerySaver.saveToDownloads(video)
    }

    suspend fun exportBatchAsZip(videos: List<VideoEntity>): Result<File> {
        return gallerySaver.createZipArchive(videos)
    }
}

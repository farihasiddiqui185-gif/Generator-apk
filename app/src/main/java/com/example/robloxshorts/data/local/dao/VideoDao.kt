package com.example.robloxshorts.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.robloxshorts.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE status = 'QUEUED' ORDER BY createdAt ASC")
    suspend fun getQueuedVideos(): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteVideosByIds(ids: List<String>)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Query("UPDATE videos SET status = :status, progress = :progress, progressMessage = :message WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Float, message: String)

    @Query("UPDATE videos SET isSavedToGallery = 1 WHERE id = :id")
    suspend fun markSavedToGallery(id: String)
}

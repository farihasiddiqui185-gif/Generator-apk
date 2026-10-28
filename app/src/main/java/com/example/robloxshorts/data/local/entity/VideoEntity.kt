package com.example.robloxshorts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val durationSeconds: Int,
    val language: String,
    val status: String, // "QUEUED", "SCRIPTING", "RENDERING", "COMPLETED", "FAILED"
    val progress: Float, // 0.0 to 1.0
    val progressMessage: String,
    val videoPath: String?,
    val thumbnailPath: String?,
    val fileSizeBytes: Long,
    val createdAt: Long,
    val errorMessage: String?,
    val isSavedToGallery: Boolean,
    val scriptJson: String
)

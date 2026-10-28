package com.example.robloxshorts.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.robloxshorts.data.local.entity.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GallerySaver(private val context: Context) {

    suspend fun saveVideoToGallery(video: VideoEntity): Result<Uri> = withContext(Dispatchers.IO) {
        val srcPath = video.videoPath ?: return@withContext Result.failure(
            IllegalStateException("Video file path is null")
        )
        val srcFile = File(srcPath)
        if (!srcFile.exists() || srcFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalStateException("Source MP4 video file does not exist on disk")
            )
        }

        try {
            val fileName = "RobloxShort_${video.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(25)}_${System.currentTimeMillis()}.mp4"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/RobloxShorts")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry"))

                resolver.openOutputStream(itemUri).use { outStream ->
                    if (outStream == null) {
                        return@withContext Result.failure(IllegalStateException("Could not open MediaStore output stream"))
                    }
                    FileInputStream(srcFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)

                Result.success(itemUri)
            } else {
                // Android 9 and below
                val moviesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "RobloxShorts"
                )
                if (!moviesDir.exists()) moviesDir.mkdirs()

                val destFile = File(moviesDir, fileName)
                FileInputStream(srcFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                // Trigger media scan so it shows up in gallery
                var scannedUri: Uri? = null
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf("video/mp4")
                ) { _, uri ->
                    scannedUri = uri
                }

                val finalUri = scannedUri ?: Uri.fromFile(destFile)
                Result.success(finalUri)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveToDownloads(video: VideoEntity): Result<File> = withContext(Dispatchers.IO) {
        val srcPath = video.videoPath ?: return@withContext Result.failure(
            IllegalStateException("Video file path is null")
        )
        val srcFile = File(srcPath)
        if (!srcFile.exists()) return@withContext Result.failure(IllegalStateException("Source file missing"))

        try {
            val fileName = "RobloxShort_${System.currentTimeMillis()}.mp4"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fileName)

            FileInputStream(srcFile).use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )

            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createZipArchive(videos: List<VideoEntity>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "RobloxShorts_Batch_${System.currentTimeMillis()}.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                var count = 0
                for (v in videos) {
                    val path = v.videoPath ?: continue
                    val file = File(path)
                    if (!file.exists()) continue

                    val cleanTitle = v.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(20)
                    val entryName = "${count + 1}_${cleanTitle}.mp4"
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                    count++
                }
                if (count == 0) {
                    return@withContext Result.failure(IllegalStateException("No completed videos found to package"))
                }
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareVideo(video: VideoEntity) {
        val path = video.videoPath ?: return
        val file = File(path)
        if (!file.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            putExtra(Intent.EXTRA_TEXT, "Watch my new Roblox Short: ${video.title} #Roblox #RobloxShorts")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Roblox Short via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareZip(zipFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Roblox AI Shorts Batch Archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share ZIP Archive via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

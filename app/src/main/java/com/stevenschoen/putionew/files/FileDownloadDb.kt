package com.stevenschoen.putionew.files

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Maybe

@Entity(tableName = "fileDownloads")
data class FileDownload(
        @PrimaryKey
        val fileId: Long,
        var downloadId: Long?,
        var status: Status,
        var uri: String?,
        var downloadedMp4: Boolean?
) {
    enum class Status {
        Downloaded, InProgress, NotDownloaded;

        class RoomConverters {
            @TypeConverter
            fun fromStatus(status: Status) = status.ordinal
            @TypeConverter
            fun toStatus(status: Int) = values()[status]
        }
    }
}

@Dao
interface FileDownloadDao {
    companion object {
        const val getByFileIdQuery = "SELECT * FROM fileDownloads WHERE fileId = :fileId"
        const val getByDownloadIdQuery = "SELECT * FROM fileDownloads WHERE downloadId = :downloadId"
    }

    @Query("SELECT * FROM fileDownloads")
    fun getAll(): List<FileDownload>

    @Query("Select * FROM fileDownloads WHERE status = :status")
    fun getAllByStatus(status: FileDownload.Status): List<FileDownload>

    @Query(getByFileIdQuery)
    fun getByFileIdOnce(fileId: Long): Maybe<FileDownload>
    @Query(getByFileIdQuery)
    fun getByFileIdUpdating(fileId: Long): Flowable<FileDownload>
    @Query(getByFileIdQuery)
    fun getByFileIdSynchronous(fileId: Long): FileDownload?

    @Query(getByDownloadIdQuery)
    fun getByDownloadIdOnce(downloadId: Long): Maybe<FileDownload>
    @Query(getByDownloadIdQuery)
    fun getByDownloadIdUpdating(downloadId: Long): Flowable<FileDownload>
    @Query(getByDownloadIdQuery)
    fun getByDownloadIdSynchronous(downloadId: Long): FileDownload?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg download: FileDownload)

    @Update
    fun update(vararg download: FileDownload)

    @Delete
    fun delete(download: FileDownload)
}

@Database(entities = arrayOf(FileDownload::class), version = 5)
@TypeConverters(FileDownload.Status.RoomConverters::class)
abstract class FileDownloadDatabase: RoomDatabase() {
    abstract fun fileDownloadsDao(): FileDownloadDao
}
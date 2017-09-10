package com.stevenschoen.putionew.files

import android.arch.persistence.room.*
import io.reactivex.Flowable

@Entity(tableName = "fileDownloads")
@TypeConverters(FileDownload.Status.RoomConverters::class)
data class FileDownload(
        @PrimaryKey
        val fileId: Long,
        var downloadId: Long?,
        var status: Status,
        var uri: String?
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

    @Query(getByFileIdQuery)
    fun getByFileId(fileId: Long): Flowable<FileDownload>
    @Query(getByFileIdQuery)
    fun getByFileIdSynchronous(fileId: Long): FileDownload

    @Query(getByDownloadIdQuery)
    fun getByDownloadId(downloadId: Long): Flowable<FileDownload>
    @Query(getByDownloadIdQuery)
    fun getByDownloadIdSynchronous(downloadId: Long): FileDownload

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg download: FileDownload)

    @Update
    fun update(vararg download: FileDownload)

    @Delete
    fun delete(download: FileDownload)
}

@Database(entities = arrayOf(FileDownload::class), version = 4)
abstract class FileDownloadDatabase: RoomDatabase() {
    abstract fun fileDownloadsDao(): FileDownloadDao
}
package com.stevenschoen.putionew.files

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.support.v4.content.ContextCompat
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.putioApp
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.util.concurrent.TimeUnit

class FileDownloadHelper(context: Context) {

  companion object {
    val slash = File.separator
    fun LongArray.asCommas() = joinToString(separator = ",")
  }

  val appContext = context.applicationContext!!
  val fileDownloads = putioApp(appContext).fileDownloadDatabase.fileDownloadsDao()
  val utils = putioApp(appContext).putioUtils!!

  fun hasPermission() = ContextCompat.checkSelfPermission(appContext,
      Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

  fun downloadFile(file: PutioFile): Single<Long> {
    return Single.fromCallable {
      download(file.getDownloadUrl(utils),
          file.name + if (file.isFolder) ".zip" else "")
    }
        .subscribeOn(Schedulers.io())
        .doOnSuccess { downloadId ->
          fileDownloads.insert(FileDownload(file.id, downloadId,
              FileDownload.Status.InProgress, null, false))
        }
        .observeOn(AndroidSchedulers.mainThread())
  }

  fun downloadVideo(video: PutioFile, mp4: Boolean): Single<Long> {
    if (mp4 && video.isMp4) throw IllegalArgumentException("${video.name} is already an MP4")
    return Single.fromCallable {
      val filename = if (mp4) FilenameUtils.removeExtension(video.name) + ".mp4" else video.name
      download(video.getDownloadUrl(utils, mp4), filename)
    }
        .subscribeOn(Schedulers.io())
        .doOnSuccess { downloadId ->
          fileDownloads.insert(FileDownload(video.id, downloadId,
              FileDownload.Status.InProgress, null, mp4))
        }
        .observeOn(AndroidSchedulers.mainThread())
  }

  fun download(url: String, filename: String): Long {
    if (!hasPermission()) {
      throw PermissionNotGrantedException()
    }

    val subPath = "put.io$slash$filename"
    val file = File("${Environment
        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}$slash$subPath")
    file.parentFile.mkdirs()

    val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(Uri.parse(url)).apply {
      setDescription("put.io")
      allowScanningByMediaScanner()
      setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, subPath)
    }

    return manager.enqueue(request)
  }

  fun copyZipLink(vararg fileIds: Long): Completable {
    return getZipUrl(*fileIds)
        .doOnSuccess { PutioUtils.copy(appContext, "Download link", it) }
        .ignoreElement()
  }

  fun getZipUrl(vararg fileIds: Long): Single<String> {
    return utils.restInterface.createZip(fileIds.asCommas())
        .map { it.zipId }
        .flatMapPublisher {
          Single.defer { utils.restInterface.getZip(it) }
              .repeatWhen { it.delay(1, TimeUnit.SECONDS) }
        }
        .filter { it.url != null }
        .map { it.url }
        .firstOrError()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }

  class PermissionNotGrantedException : SecurityException(
      "External storage write permission not granted")
}

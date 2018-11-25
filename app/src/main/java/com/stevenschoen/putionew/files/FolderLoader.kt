package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.getUniqueLoaderId
import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.model.responses.FilesListResponse
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.Charset

class FolderLoader(context: Context, private val folder: PutioFile) : PutioBaseLoader(context) {

  private val diskCache by lazy { DiskCache(context) }

  private val folderSubject = BehaviorSubject.create<ResponseOrError>()
  fun folder() = folderSubject.observeOn(AndroidSchedulers.mainThread())!!

  fun publishCachedFileIfNeeded() {
    fun isNeeded() = !hasFresh()
    if (isNeeded()) {
      Completable.fromCallable {
        if (diskCache.isCached(folder.id)) {
          diskCache.getCached(folder.id)?.let {
            if (isNeeded()) {
              folderSubject.onNext(FolderResponse(false).apply {
                parent = it.parent
                files = it.files
              })
            }
          }
        }
      }
          .subscribeOn(Schedulers.io())
          .subscribe({ },
              { error ->
                PutioUtils.getRxJavaThrowable(error).printStackTrace()
                diskCache.deleteCached(folder.id)
              })
    }
  }

  private var refreshSubscription: Disposable? = null
  fun refreshFolder(onlyIfStaleOrEmpty: Boolean = false, cache: Boolean = true) {
    if (onlyIfStaleOrEmpty && (hasFresh() || isRefreshing())) return
    refreshSubscription?.dispose()
    refreshSubscription = api.files(folder.id).subscribe({ response ->
      refreshSubscription = null
      if (cache) diskCache.cache(response)
      folderSubject.onNext(FolderResponse(true).apply {
        parent = response.parent
        files = response.files
      })
    }, { error ->
      refreshSubscription = null
      folderSubject.onNext(ResponseOrError.NetworkError(error))
    })
  }

  fun isRefreshing() = refreshSubscription != null && !refreshSubscription!!.isDisposed

  fun hasFresh() = folderSubject.value.let { (it is FolderResponse && it.fresh) }

  class FolderResponse(val fresh: Boolean) : FilesListResponse()

  class DiskCache(val context: Context) {
    private val filesCacheDir = File("${context.cacheDir}${File.separator}filesCache")

    private var gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    init {
      filesCacheDir.mkdirs()
    }

    fun cache(response: FilesListResponse) {
      val file = getFile(response.parent.id)
      try {
        FileUtils.writeStringToFile(file, gson.toJson(response), Charset.defaultCharset())
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }

    fun isCached(parentId: Long): Boolean = getFile(parentId).exists()

    fun getCached(parentId: Long): FilesListResponse? {
      val file = getFile(parentId)
      try {
        return gson.fromJson(FileUtils.readFileToString(file, Charset.defaultCharset()), FilesListResponse::class.java)
      } catch (e: FileNotFoundException) {
        // Not cached yet, no problem
      } catch (e: IOException) {
        e.printStackTrace()
      }

      return null
    }

    fun deleteCached(parentId: Long) = getFile(parentId).delete()

    private fun getFile(parentId: Long) = File("$filesCacheDir${File.separator}$parentId.json")

    fun deleteCache() = File("$filesCacheDir${File.pathSeparator}").deleteRecursively()
  }

  companion object {
    fun get(loaderManager: LoaderManager, context: Context, folder: PutioFile): FolderLoader {
      return loaderManager.initLoader(
          getUniqueLoaderId(FolderLoader::class.java), null, object : Callbacks(context) {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
          return FolderLoader(context, folder)
        }
      }) as FolderLoader
    }
  }
}

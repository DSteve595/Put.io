package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.squareup.moshi.Moshi
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
import okio.Okio
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

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
              folderSubject.onNext(
                  FolderResponse(
                      fresh = false,
                      parent = it.parent,
                      files = it.files
                  )
              )
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
      folderSubject.onNext(
          FolderResponse(
              fresh = true,
              parent = response.parent,
              files = response.files
          )
      )
    }, { error ->
      refreshSubscription = null
      folderSubject.onNext(ResponseOrError.NetworkError(error))
    })
  }

  fun isRefreshing() = refreshSubscription != null && !refreshSubscription!!.isDisposed

  fun hasFresh() = folderSubject.value.let { (it is FolderResponse && it.fresh) }

  class FolderResponse(
      val fresh: Boolean,
      files: List<PutioFile>,
      parent: PutioFile
  ) : FilesListResponse(files = files, parent = parent)

  // TODO make a db
  class DiskCache(val context: Context) {
    private val filesCacheDir = File("${context.cacheDir}${File.separator}filesCache")

    private val moshi = Moshi.Builder().build()

    init {
      filesCacheDir.mkdirs()
    }

    fun cache(response: FilesListResponse) {
      val file = getFile(response.parent.id)
      try {
        moshi.adapter(FilesListResponse::class.java).toJson(Okio.buffer(Okio.sink(file)), response)
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }

    fun isCached(parentId: Long): Boolean = getFile(parentId).exists()

    fun getCached(parentId: Long): FilesListResponse? {
      val file = getFile(parentId)
      try {
        return moshi.adapter(FilesListResponse::class.java).fromJson(Okio.buffer(Okio.source(file)))
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

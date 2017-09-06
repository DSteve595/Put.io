package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.getUniqueLoaderId
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

class FolderLoader(context: Context, private val folder: PutioFile) : PutioBaseLoader(context) {

    val diskCache by lazy { DiskCache() }

    private val folderSubject = BehaviorSubject.create<FolderResponse>()
    fun folder() = folderSubject.observeOn(AndroidSchedulers.mainThread())

    fun publishCachedFileIfNeeded() {
        fun isNeeded() = (!folderSubject.hasValue() || !folderSubject.value.fresh)
        if (isNeeded()) {
            Completable.fromCallable {
                if (diskCache.isCached(folder.id)) {
                    diskCache.getCached(folder.id)?.let {
                        if (isNeeded()) {
                            folderSubject.onNext(FolderResponse(false, it.parent, it.files))
                        }
                    }
                }
            }
                    .subscribeOn(Schedulers.io())
                    .subscribe({ },
                            { error ->
                                error.printStackTrace()
                                diskCache.deleteCached(folder.id)
                            })
        }
    }

    var refreshSubscription: Disposable? = null
    fun refreshFolder(onlyIfStaleOrEmpty: Boolean = false, cache: Boolean = true) {
        if (onlyIfStaleOrEmpty && (hasFresh() || isRefreshing())) return
        refreshSubscription?.dispose()
        refreshSubscription = api.files(folder.id).subscribe({ response ->
            refreshSubscription = null
            if (cache) diskCache.cache(response)
            folderSubject.onNext(FolderResponse(true, response.parent, response.files))
        }, { error ->
            refreshSubscription = null
            folderSubject?.onError(error)
            error.printStackTrace()
        })
    }

    fun isRefreshing() = refreshSubscription != null && !refreshSubscription!!.isDisposed

    fun hasFresh(): Boolean = (folderSubject.hasValue() && folderSubject.value.fresh)

    data class FolderResponse(val fresh: Boolean, val parent: PutioFile, val files: List<PutioFile>)

    inner class DiskCache {
        private val filesCacheDir = File("${context.cacheDir}${File.separator}filesCache")

        internal var gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

        init {
            filesCacheDir.mkdirs()
        }

        fun cache(response: FilesListResponse) {
            val file = getFile(response.parent.id)
            try {
                FileUtils.writeStringToFile(file, gson.toJson(response))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun isCached(parentId: Long): Boolean = getFile(parentId).exists()

        fun getCached(parentId: Long): FilesListResponse? {
            val file = getFile(parentId)
            try {
                return gson.fromJson(FileUtils.readFileToString(file), FilesListResponse::class.java)
            } catch (e: FileNotFoundException) {
                // Not cached yet, no problem
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }

        fun deleteCached(parentId: Long) {
            val file = getFile(parentId)
            file.delete()
        }

        private fun getFile(parentId: Long): File {
            return File("$filesCacheDir${File.separator}$parentId.json")
        }
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
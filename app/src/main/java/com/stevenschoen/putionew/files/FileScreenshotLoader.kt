package com.stevenschoen.putionew.files

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.squareup.picasso.Picasso
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.getUniqueLoaderId
import com.stevenschoen.putionew.model.files.PutioFile
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class FileScreenshotLoader(context: Context, val file: PutioFile) : PutioBaseLoader(context) {

  private val screenshotSubject = BehaviorSubject.create<Bitmap>()
  fun screenshot() = screenshotSubject.observeOn(AndroidSchedulers.mainThread())!!

  var disposable: Disposable? = null
  fun load(onlyIfEmpty: Boolean = false) {
    if (onlyIfEmpty && (screenshotSubject.hasValue() || isLoading())) return
    disposable?.dispose()
    val single = Single.fromCallable {
      Picasso.get()
          .load(file.screenshot)
          .get()
    }.subscribeOn(Schedulers.io())

    disposable = single.subscribe({ response ->
      disposable = null
      screenshotSubject.onNext(response)
    }, { error ->
      disposable = null
      screenshotSubject.onError(error)
      PutioUtils.getRxJavaThrowable(error).printStackTrace()
    })
  }

  fun isLoading(): Boolean {
    return disposable != null && !disposable!!.isDisposed
  }

  companion object {
    fun get(loaderManager: LoaderManager, context: Context, file: PutioFile): FileScreenshotLoader {
      return loaderManager.initLoader(
          getUniqueLoaderId(FileScreenshotLoader::class.java), null, object : Callbacks(context) {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
          return FileScreenshotLoader(context, file)
        }
      }) as FileScreenshotLoader
    }
  }
}

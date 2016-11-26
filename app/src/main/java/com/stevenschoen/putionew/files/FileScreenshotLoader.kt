package com.stevenschoen.putionew.files

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import com.squareup.picasso.Picasso
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.getUniqueLoaderId
import com.stevenschoen.putionew.model.files.PutioFile
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject

class FileScreenshotLoader(context: Context, val file: PutioFile) : PutioBaseLoader(context) {

    private val screenshotSubject = BehaviorSubject.create<Bitmap>()
    fun screenshot() = screenshotSubject.observeOn(AndroidSchedulers.mainThread())

    var subscription: Subscription? = null
    fun load(onlyIfEmpty: Boolean = false) {
        if (onlyIfEmpty && (screenshotSubject.hasValue() || isLoading())) return
        subscription?.unsubscribe()
        val observable = Observable.fromCallable {
            Picasso.with(context)
                    .load(file.screenshot)
                    .get()
        }.subscribeOn(Schedulers.io())

        subscription = observable.subscribe({ response ->
            subscription = null
            screenshotSubject.onNext(response)
        }, { error ->
            subscription = null
            error.printStackTrace()
        })
    }

    fun isLoading(): Boolean {
        return subscription != null && !subscription!!.isUnsubscribed
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
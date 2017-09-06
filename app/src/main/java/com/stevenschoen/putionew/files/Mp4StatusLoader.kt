package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.getUniqueLoaderId
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.model.files.PutioMp4Status
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class Mp4StatusLoader(context: Context, val file: PutioFile) : PutioBaseLoader(context) {

    var refreshing = false

    private val mp4StatusSubject = BehaviorSubject.create<PutioMp4Status>()
    fun mp4Status() = mp4StatusSubject.observeOn(AndroidSchedulers.mainThread())
    fun lastMp4Status() = mp4StatusSubject.value

    private val observable = Single.defer { api.mp4Status(file.id) }
    private var disposable: Disposable? = null
    private fun makeSubscription() = observable.repeatWhen {
        it.delay(4, TimeUnit.SECONDS)
    }
            .takeUntil { !refreshing && isStarted }
            .subscribe({ response ->
                mp4StatusSubject.onNext(response.mp4Status)

                when (response.mp4Status.status!!) {
                    PutioMp4Status.Status.Completed,
                    PutioMp4Status.Status.AlreadyMp4,
                    PutioMp4Status.Status.NotAvailable -> {
                        stopRefreshing()
                    }
                    PutioMp4Status.Status.InQueue,
                    PutioMp4Status.Status.Preparing,
                    PutioMp4Status.Status.Converting,
                    PutioMp4Status.Status.Finishing -> {
                        startRefreshing()
                    }
                    PutioMp4Status.Status.Error -> stopRefreshing()
                }
            }, { error ->
                mp4StatusSubject.onError(error)
            })

    fun refreshOnce() {
        if (file.isMp4) {
            mp4StatusSubject.onNext(PutioMp4Status().apply {
                status = PutioMp4Status.Status.AlreadyMp4
            })
        } else {
            disposable?.dispose()
            disposable = makeSubscription()
        }
    }

    fun startRefreshing() {
        if (!refreshing) {
            refreshing = true
            refreshOnce()
        }
    }

    fun stopRefreshing() {
        disposable?.dispose()
        disposable = null
        refreshing = false
    }

    companion object {
        fun get(loaderManager: LoaderManager, context: Context, file: PutioFile): Mp4StatusLoader {
            return loaderManager.initLoader(
                    getUniqueLoaderId(Mp4StatusLoader::class.java), null, object : Callbacks(context) {
                override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
                    return Mp4StatusLoader(context, file)
                }
            }) as Mp4StatusLoader
        }
    }
}
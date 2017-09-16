package com.stevenschoen.putionew

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.stevenschoen.putionew.model.responses.TransfersListResponse
import com.stevenschoen.putionew.model.transfers.PutioTransfer
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import java.util.concurrent.TimeUnit

class PutioTransfersService : Service() {

    private val binder = TransfersServiceBinder()
    private var stopTask: TimerTask? = null

    inner class TransfersServiceBinder : Binder() {
        val service: PutioTransfersService
            get() = this@PutioTransfersService
    }

    private val utils by lazy {
        (application as PutioApplication).putioUtils
    }

    private val transfersSubject = BehaviorSubject.create<List<PutioTransfer>>()
    val transfers: Observable<List<PutioTransfer>>
        get() = transfersSubject

    private lateinit var transfersRefreshObservable: Observable<TransfersListResponse>
    private var transfersRefreshSubscription: Disposable? = null

    override fun onCreate() {
        super.onCreate()

        val transfersFetchObservable = Single.defer {
            utils!!.restInterface.transfers()
        }

        transfersRefreshObservable = transfersFetchObservable
                .retryWhen { it.delay(5, TimeUnit.SECONDS) }
                .repeatWhen { it.delay(8, TimeUnit.SECONDS) }
                .toObservable()

        startRefreshing()
    }

    private fun startRefreshing() {
        stopRefreshing()
        transfersRefreshSubscription = transfersRefreshObservable
                .subscribe { response ->
                    val responseTransfers = response.transfers.reversed()
                    val oldTransfers = transfersSubject.value
                    if (oldTransfers == null || oldTransfers != responseTransfers) {
                        transfersSubject.onNext(responseTransfers)
                    }
                }
    }

    private fun stopRefreshing() {
        transfersRefreshSubscription?.let {
            it.dispose()
            transfersRefreshSubscription = null
        }
    }

    fun refreshNow() = startRefreshing()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) = Service.START_NOT_STICKY

    override fun onBind(intent: Intent): IBinder? {
        if (stopTask != null) stopTask!!.cancel()
        return binder
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
        if (stopTask != null) stopTask!!.cancel()
    }

    override fun onUnbind(intent: Intent): Boolean {
        stopTask = object : TimerTask() {
            override fun run() {
                stopSelf()
            }
        }
        Timer().schedule(stopTask, 5000)

        return true
    }

    override fun onDestroy() {
        stopRefreshing()
        super.onDestroy()
    }
}
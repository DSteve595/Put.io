package com.stevenschoen.putionew;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.stevenschoen.putionew.activities.Putio;
import com.stevenschoen.putionew.model.responses.TransfersListResponse;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func0;
import rx.subjects.BehaviorSubject;

public class PutioTransfersService extends Service {
	
	private final IBinder binder = new TransfersServiceBinder();
	private TimerTask stopTask;

	public class TransfersServiceBinder extends Binder {
		public PutioTransfersService getService() {
			return PutioTransfersService.this;
		}
	}

	private PutioUtils utils;

	private BehaviorSubject<List<PutioTransfer>> transfers;

	private Observable<TransfersListResponse> transfersFetchObservable;
	private Subscription fetchSubscription;
	private Subscriber<TransfersListResponse> subscriber;
	
	@Override
	public void onCreate() {
		super.onCreate();

		utils = ((PutioApplication) getApplication()).getPutioUtils();

		transfers = BehaviorSubject.create();

		transfersFetchObservable = Observable.defer(new Func0<Observable<TransfersListResponse>>() {
			@Override
			public Observable<TransfersListResponse> call() {
				return utils.getRestInterface().transfers();
			}
		});
		subscriber = makeSubscriber();
		fetchSubscription = transfersFetchObservable.subscribe(subscriber);
	}

	public BehaviorSubject<List<PutioTransfer>> getTransfersObservable() {
		return transfers;
	}

	public void updateNow() {
		subscriber.unsubscribe();
		subscriber = makeSubscriber();
		transfersFetchObservable.subscribe(subscriber);
	}

	private Subscriber<TransfersListResponse> makeSubscriber() {
		return new Subscriber<TransfersListResponse>() {
			@Override
			public void onCompleted() { }

			@Override
			public void onError(Throwable e) {
				transfers.onError(e);
				if (!utils.isConnected(PutioTransfersService.this)) {
					Intent noNetworkIntent = new Intent(Putio.noNetworkIntent);
					noNetworkIntent.putExtra("from", "transfers");
					sendBroadcast(noNetworkIntent);
				}
				transfersFetchObservable.delaySubscription(8, TimeUnit.SECONDS).subscribe(this);
			}

			@Override
			public void onNext(TransfersListResponse response) {
				List<PutioTransfer> responseTransfers = response.getTransfers();
				Collections.reverse(responseTransfers);
				List<PutioTransfer> oldTransfers = transfers.getValue();
				if (oldTransfers == null || !oldTransfers.equals(responseTransfers)) {
					transfers.onNext(responseTransfers);
				}
				transfersFetchObservable.delaySubscription(8, TimeUnit.SECONDS).subscribe(this);
			}
		};
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (stopTask != null) stopTask.cancel();
		return binder;
	}
	
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		if (stopTask != null) stopTask.cancel();
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		stopTask = new TimerTask() {
			@Override
			public void run() {
				stopSelf();
			}
		};
		new Timer().schedule(stopTask, 5000);
		
		return true;
	}

	@Override
	public void onDestroy() {
		if (fetchSubscription != null) fetchSubscription.unsubscribe();
		super.onDestroy();
	}
}
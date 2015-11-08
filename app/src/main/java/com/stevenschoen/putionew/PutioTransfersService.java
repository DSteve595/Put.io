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
import rx.functions.Action1;
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

	private Subscription fetchSubscription;
	
	@Override
	public void onCreate() {
		super.onCreate();

		utils = ((PutioApplication) getApplication()).getPutioUtils();

		transfers = BehaviorSubject.create();

		final Observable<List<PutioTransfer>> transfersFetchObservable = Observable.create(new Observable.OnSubscribe<List<PutioTransfer>>() {
			@Override
			public void call(final Subscriber<? super List<PutioTransfer>> subscriber) {
				utils.getRestInterface().transfers()
						.subscribe(new Action1<TransfersListResponse>() {
							@Override
							public void call(TransfersListResponse transfersListResponse) {
								subscriber.onNext(transfersListResponse.getTransfers());
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								subscriber.onError(throwable);
							}
						});
			}
		});
		fetchSubscription = transfersFetchObservable.subscribe(new Subscriber<List<PutioTransfer>>() {
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
			public void onNext(List<PutioTransfer> transfersList) {
				Collections.reverse(transfersList);
				List<PutioTransfer> oldTransfers = transfers.getValue();
				if (oldTransfers == null || !oldTransfers.equals(transfersList)) {
					transfers.onNext(transfersList);
				}
				transfersFetchObservable.delaySubscription(8, TimeUnit.SECONDS).subscribe(this);
			}
		});
	}

	public BehaviorSubject<List<PutioTransfer>> getTransfersObservable() {
		return transfers;
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
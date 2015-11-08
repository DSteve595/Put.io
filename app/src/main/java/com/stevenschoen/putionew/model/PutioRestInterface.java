package com.stevenschoen.putionew.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.squareup.okhttp.Response;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.activities.Putio;
import com.stevenschoen.putionew.activities.TransfersActivity;
import com.stevenschoen.putionew.model.responses.AccountInfoResponse;
import com.stevenschoen.putionew.model.responses.BasePutioResponse;
import com.stevenschoen.putionew.model.responses.FileResponse;
import com.stevenschoen.putionew.model.responses.FilesListResponse;
import com.stevenschoen.putionew.model.responses.FilesSearchResponse;
import com.stevenschoen.putionew.model.responses.Mp4StatusResponse;
import com.stevenschoen.putionew.model.responses.TransfersListResponse;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface PutioRestInterface {
	@GET("/files/list")
	Observable<FilesListResponse> files(@Query("parent_id") long parentId);

	@GET("/files/search/{query}/page/-1")
	Observable<FilesSearchResponse> searchFiles(@Path("query") String query);

	@GET("/files/{id}")
	Observable<FileResponse> file(@Path("id") long id);

	@FormUrlEncoded
	@GET("/files/zip")
	BasePutioResponse zip(@Field("file_ids") String ids);

	@GET("/files/{id}/mp4")
	Mp4StatusResponse mp4Status(@Path("id") long id);

	@POST("/files/{id}/mp4")
	BasePutioResponse convertToMp4(@Path("id") long id);

    @FormUrlEncoded
    @POST("/files/create-folder")
    Observable<BasePutioResponse.FileChangingResponse> createFolder(@Field("name") String name, @Field("parent_id") long parentId);

    @FormUrlEncoded
	@POST("/files/rename")
	Observable<BasePutioResponse.FileChangingResponse> renameFile(@Field("file_id") long id, @Field("name") String name);

	@FormUrlEncoded
	@POST("/files/delete")
	Observable<BasePutioResponse.FileChangingResponse> deleteFile(@Field("file_ids") String ids);

    @FormUrlEncoded
    @POST("/files/move")
    Observable<BasePutioResponse.FileChangingResponse> moveFile(@Field("file_ids") String ids, @Field("parent_id") long newParentId);

	@GET("/transfers/list")
	Observable<TransfersListResponse> transfers();

	@FormUrlEncoded
	@POST("/transfers/add")
	void addTransferUrl(@Field("url") String url, @Field("extract") boolean extract, @Field("save_parent_id") long saveParentId, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/transfers/retry")
    BasePutioResponse retryTransfer(@Field("id") long id);

	@FormUrlEncoded
	@POST("/transfers/cancel")
	BasePutioResponse cancelTransfer(@Field("transfer_ids") String ids);

	@POST("/transfers/clean")
	BasePutioResponse cleanTransfers();

	@GET("/account/info")
	AccountInfoResponse account();

	public static abstract class PutioJob extends Job {
		private PutioUtils utils;
        
        protected PutioJob(PutioUtils utils) {
            this(new Params(0).requireNetwork(), utils);
        }

		protected PutioJob(Params params, PutioUtils utils) {
			super(params);
			this.utils = utils;
		}

		@Override
		public abstract void onRun() throws Throwable;

		protected PutioUtils getUtils() {
			return utils;
		}

		@Override
		public void onAdded() { }

		@Override
		protected void onCancel() { }

		@Override
		protected boolean shouldReRunOnThrowable(Throwable throwable) {
			return false;
		}
	}

	public static abstract class PutioUploadJob extends PutioJob implements Callback<Response> {
		protected Context context;
		private NotificationManager notifManager;
		private Intent retryIntent;

		protected PutioUploadJob(PutioUtils utils, Context context, Intent retryIntent) {
			super(utils);

			this.context = context;
			this.retryIntent = retryIntent;
			this.notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		}

		@Override
		public void onRun() throws Throwable {
			notifStart();
		}

		@Override
		public void success(Response response, retrofit.client.Response response2) {
			notifSucceeded();
		}

		@Override
		public void failure(RetrofitError error) {
			notifFailed();
		}

		protected void notifStart() {
			NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context);
			notifBuilder.setOngoing(true);
            notifBuilder.setCategory(NotificationCompat.CATEGORY_PROGRESS);
            notifBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
            notifBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
			notifBuilder.setContentTitle(context.getString(R.string.notification_title_uploading_torrent));
			notifBuilder.setSmallIcon(R.drawable.ic_notificon_transfer);
			notifBuilder.setTicker(context.getString(R.string.notification_ticker_uploading_torrent));
			notifBuilder.setProgress(1, 0, true);
			Notification notif = notifBuilder.build();
			notif.ledARGB = Color.parseColor("#FFFFFF00");
			try {
				notifManager.notify(1, notif);
			} catch (IllegalArgumentException e) {
				notifBuilder.setContentIntent(PendingIntent.getActivity(
						context, 0, new Intent(context, Putio.class), 0));
				notif = notifBuilder.build();
				notif.ledARGB = Color.parseColor("#FFFFFF00");
				notifManager.notify(1, notif);
			}
		}

		protected void notifSucceeded() {
			NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context);
			notifBuilder.setOngoing(false);
			notifBuilder.setAutoCancel(true);
            notifBuilder.setCategory(NotificationCompat.CATEGORY_PROGRESS);
            notifBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
            notifBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
			notifBuilder.setContentTitle(context.getString(R.string.notification_title_uploaded_torrent));
			notifBuilder.setContentText(context.getString(R.string.notification_body_uploaded_torrent));
			notifBuilder.setSmallIcon(R.drawable.ic_notificon_transfer);
			notifBuilder.setContentIntent(PendingIntent.getActivity(
					context, 0, new Intent(context, TransfersActivity.class),
					PendingIntent.FLAG_ONE_SHOT));
//				notifBuilder.addAction(R.drawable.ic_notif_watch, "Watch", null); TODO
			notifBuilder.setTicker(context.getString(R.string.notification_ticker_uploaded_torrent));
			notifBuilder.setProgress(0, 0, false);
			Notification notif = notifBuilder.build();
			notif.ledARGB = Color.parseColor("#FFFFFF00");
			notifManager.notify(1, notif);
		}

		protected void notifFailed() {
			NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context);
			notifBuilder.setOngoing(false);
			notifBuilder.setAutoCancel(true);
            notifBuilder.setCategory(NotificationCompat.CATEGORY_ERROR);
            notifBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notifBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
			notifBuilder.setContentTitle(context.getString(R.string.notification_title_error));
			notifBuilder.setContentText(context.getString(R.string.notification_body_error));
			notifBuilder.setSmallIcon(R.drawable.ic_notificon_transfer);
			PendingIntent retryNotifIntent = PendingIntent.getActivity(
					context, 0, retryIntent, PendingIntent.FLAG_ONE_SHOT);
			notifBuilder.addAction(R.drawable.ic_notif_retry, context.getString(R.string.notification_button_retry), retryNotifIntent);
			notifBuilder.setContentIntent(retryNotifIntent);
			notifBuilder.setTicker(context.getString(R.string.notification_ticker_error));
			Notification notif = notifBuilder.build();
			notif.ledARGB = Color.parseColor("#FFFFFF00");
			notifManager.notify(1, notif);
		}
	}

	public static class GetMp4StatusJob extends PutioJob {
		private long id;

		public GetMp4StatusJob(PutioUtils utils, long id) {
			super(utils);
			this.id = id;
		}

		@Override
		public void onRun() throws Throwable {
			Mp4StatusResponse networkResponse = getUtils().getRestInterface().mp4Status(id);
			getUtils().getEventBus().post(networkResponse);
		}
	}

	public static class PostConvertToMp4Job extends PutioJob {
		private long id;

		public PostConvertToMp4Job(PutioUtils utils, long id) {
			super(utils);
			this.id = id;
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getRestInterface().convertToMp4(id);
		}
	}

	public static class PostAddTransferJob extends PutioUploadJob {
		private String url;
        private boolean extract;
        private long saveParentId;

		public PostAddTransferJob(PutioUtils utils, String url, boolean extract, long saveParentId, Context context, Intent retryIntent) {
			super(utils, context, retryIntent);
			this.url = url;
            this.extract = extract;
            this.saveParentId = saveParentId;
		}

		@Override
		public void onRun() throws Throwable {
			super.onRun();
			getUtils().getRestInterface().addTransferUrl(url, extract, saveParentId, this);
		}
	}

    public static class PostRetryTransferJob extends PutioJob {
        private long id;

        public PostRetryTransferJob(PutioUtils utils, long id) {
            super(utils);
            this.id = id;
        }

        @Override
        public void onRun() throws Throwable {
            getUtils().getRestInterface().retryTransfer(id);
        }
    }

	public static class PostCancelTransferJob extends PutioJob {
		private long[] ids;

		public PostCancelTransferJob(PutioUtils utils, long... ids) {
			super(utils);
			this.ids = ids;
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getRestInterface().cancelTransfer(PutioUtils.longsToString(ids));
		}
	}

	public static class PostCleanTransfersJob extends PutioJob {
		public PostCleanTransfersJob(PutioUtils utils) {
			super(utils);
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getRestInterface().cleanTransfers();
		}
	}

	public static class GetAccountInfoJob extends PutioJob {
		public GetAccountInfoJob(PutioUtils utils) {
			super(utils);
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getEventBus().post(getUtils().getRestInterface().account());
		}

	}
}
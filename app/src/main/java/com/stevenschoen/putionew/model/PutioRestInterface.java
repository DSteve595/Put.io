package com.stevenschoen.putionew.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
import com.stevenschoen.putionew.model.responses.CachedFilesListResponse;
import com.stevenschoen.putionew.model.responses.FileResponse;
import com.stevenschoen.putionew.model.responses.FilesListResponse;
import com.stevenschoen.putionew.model.responses.FilesSearchResponse;
import com.stevenschoen.putionew.model.responses.Mp4StatusResponse;
import com.stevenschoen.putionew.model.responses.TransfersListResponse;

import org.apache.commons.io.FileUtils;

import java.io.File;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

public interface PutioRestInterface {
	@GET("/files/list")
	FilesListResponse files(@Query("parent_id") int parentId);

	@GET("/files/search/{query}/page/-1")
	FilesSearchResponse searchFiles(@Path("query") String query);

	@GET("/files/{id}")
	FileResponse file(@Path("id") int id);

	@FormUrlEncoded
	@GET("/files/zip")
	BasePutioResponse zip(@Field("file_ids") String ids);

	@GET("/files/{id}/mp4")
	Mp4StatusResponse mp4Status(@Path("id") int id);

	@POST("/files/{id}/mp4")
	BasePutioResponse convertToMp4(@Path("id") int id);

	@Multipart
	@POST("/files/upload")
	void uploadFile(@Part("file") TypedFile file, @Part("parent_id") int parentId, Callback<Response> callback);

    @FormUrlEncoded
	@POST("/files/rename")
	BasePutioResponse.FileChangingResponse renameFile(@Field("file_id") int id, @Field("name") String name);

	@FormUrlEncoded
	@POST("/files/delete")
	BasePutioResponse.FileChangingResponse deleteFile(@Field("file_ids") String ids);

    @FormUrlEncoded
    @POST("/files/move")
    BasePutioResponse.FileChangingResponse moveFile(@Field("file_ids") String ids, @Field("parent_id") String newParentId);

	@GET("/transfers/list")
	TransfersListResponse transfers();

	@FormUrlEncoded
	@POST("/transfers/add")
	void addTransferUrl(@Field("url") String url, @Field("extract") boolean extract, @Field("save_parent_id") int saveParentId, Callback<Response> callback);

    @FormUrlEncoded
    @POST("/transfers/retry")
    BasePutioResponse retryTransfer(@Field("id") int id);

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

	public static class GetFilesListJob extends PutioJob {
		private int parentId;

		private boolean alsoUseCache;

		public GetFilesListJob(PutioUtils utils, int parentId, boolean alsoUseCache) {
			super(utils);
			this.parentId = parentId;
			this.alsoUseCache = alsoUseCache;
		}

		@Override
		public void onRun() throws Throwable {
			if (alsoUseCache) {
				CachedFilesListResponse cachedResponse = getUtils().getFilesCache().getCached(parentId);
				if (cachedResponse != null) {
					getUtils().getEventBus().post(cachedResponse);
				}
			}

			FilesListResponse networkResponse = getUtils().getRestInterface().files(parentId);
			if (alsoUseCache) getUtils().getFilesCache().cache(networkResponse, parentId);
			getUtils().getEventBus().post(networkResponse);
		}
	}

	public static class GetFilesSearchJob extends PutioJob {
		private String query;

		public GetFilesSearchJob(PutioUtils utils, String query) {
			super(utils);
			this.query = query;
		}

		@Override
		public void onRun() throws Throwable {
			FilesSearchResponse networkResponse = getUtils().getRestInterface().searchFiles(query);
            networkResponse.setQuery(query);
			getUtils().getEventBus().post(networkResponse);
		}
	}

	public static class GetFileJob extends PutioJob {
		private int id;

		public GetFileJob(PutioUtils utils, int id) {
			super(utils);
			this.id = id;
		}

		@Override
		public void onRun() throws Throwable {
			FileResponse networkResponse = getUtils().getRestInterface().file(id);
			getUtils().getEventBus().post(networkResponse);
		}
	}

	public static class GetZipAndDownloadJob extends PutioJob {
		private int[] ids;

		public GetZipAndDownloadJob(PutioUtils utils, Context context, int... ids) {
			super(utils);
			this.ids = ids;
		}

		@Override
		public void onRun() throws Throwable {
//			getUtils().getRestInterface().zip(PutioUtils.intsToString(ids)); TODO
		}
	}

	public static class GetMp4StatusJob extends PutioJob {
		private int id;

		public GetMp4StatusJob(PutioUtils utils, int id) {
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
		private int id;

		public PostConvertToMp4Job(PutioUtils utils, int id) {
			super(utils);
			this.id = id;
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getRestInterface().convertToMp4(id);
		}
	}

	public static class PostUploadFileJob extends PutioUploadJob {
		private Uri fileUri;
        private int parentId;

		public PostUploadFileJob(PutioUtils utils, Context context, Intent retryIntent, Uri fileUri, int parentId) {
			super(utils, context, retryIntent);
			this.fileUri = fileUri;
            this.parentId = parentId;
		}

		@Override
		public void onRun() throws Throwable {
			super.onRun();
			File file;
			if (fileUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
				file = new File(context.getCacheDir(), "upload.torrent");
				ContentResolver cr = context.getContentResolver();
				FileUtils.copyInputStreamToFile(cr.openInputStream(fileUri), file);
			} else {
				file = new File(fileUri.getPath());
			}

			try {
				getUtils().getRestInterface().uploadFile(
						new TypedFile("application/x-bittorrent", file), parentId, this);
			} catch (Exception e) {
				file.delete();
				throw e;
			}
		}
	}

	public static class PostRenameFileJob extends PutioJob {
		private int id;
		private String name;

		public PostRenameFileJob(PutioUtils utils, int id, String name) {
			super(utils);
			this.id = id;
			this.name = name;
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getEventBus().post(getUtils().getRestInterface().renameFile(id, name));
		}
	}

	public static class PostDeleteFilesJob extends PutioJob {
		private int[] ids;

		public PostDeleteFilesJob(PutioUtils utils, int... ids) {
			super(utils);
			this.ids = ids;
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getEventBus().post(getUtils().getRestInterface().deleteFile(PutioUtils.intsToString(ids)));
		}
	}

    public static class PostMoveFilesJob extends PutioJob {
        private int newParentId;
        private int[] ids;

        public PostMoveFilesJob(PutioUtils utils, int newParentId, int... ids) {
            super(utils);
            this.newParentId = newParentId;
            this.ids = ids;
        }

        @Override
        public void onRun() throws Throwable {
            getUtils().getEventBus().post(getUtils().getRestInterface().moveFile(PutioUtils.intsToString(ids), Integer.toString(newParentId)));
        }
    }

	public static class GetTransfersJob extends PutioJob {
		public GetTransfersJob(PutioUtils utils) {
			super(new Params(0).requireNetwork().groupBy("gettransfers"), utils);
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getEventBus().post(getUtils().getRestInterface().transfers());
		}
	}

	public static class PostAddTransferJob extends PutioUploadJob {
		private String url;
        private boolean extract;
        private int saveParentId;

		public PostAddTransferJob(PutioUtils utils, String url, boolean extract, int saveParentId, Context context, Intent retryIntent) {
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
        private int id;

        public PostRetryTransferJob(PutioUtils utils, int id) {
            super(utils);
            this.id = id;
        }

        @Override
        public void onRun() throws Throwable {
            getUtils().getRestInterface().retryTransfer(id);
        }
    }

	public static class PostCancelTransferJob extends PutioJob {
		private int[] ids;

		public PostCancelTransferJob(PutioUtils utils, int... ids) {
			super(utils);
			this.ids = ids;
		}

		@Override
		public void onRun() throws Throwable {
			getUtils().getRestInterface().cancelTransfer(PutioUtils.intsToString(ids));
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
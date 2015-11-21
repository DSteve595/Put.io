package com.stevenschoen.putionew.model;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.model.responses.AccountInfoResponse;
import com.stevenschoen.putionew.model.responses.BasePutioResponse;
import com.stevenschoen.putionew.model.responses.FileResponse;
import com.stevenschoen.putionew.model.responses.FilesListResponse;
import com.stevenschoen.putionew.model.responses.FilesSearchResponse;
import com.stevenschoen.putionew.model.responses.Mp4StatusResponse;
import com.stevenschoen.putionew.model.responses.TransfersListResponse;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import retrofit.http.Body;
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
	Observable<PutioTransfer> addTransferUrl(@Field("url") String url, @Field("extract") boolean extract, @Field("save_parent_id") long saveParentId);

    @FormUrlEncoded
    @POST("/transfers/retry")
    Observable<BasePutioResponse> retryTransfer(@Field("id") long id);

	@FormUrlEncoded
	@POST("/transfers/cancel")
	Observable<BasePutioResponse> cancelTransfer(@Field("transfer_ids") String ids);

	@POST("/transfers/clean")
	Observable<BasePutioResponse> cleanTransfers(@Body String nothing);

	@GET("/account/info")
	Observable<AccountInfoResponse> account();

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
}
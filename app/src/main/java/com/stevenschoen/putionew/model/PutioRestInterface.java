package com.stevenschoen.putionew.model;

import com.stevenschoen.putionew.model.responses.AccountInfoResponse;
import com.stevenschoen.putionew.model.responses.BasePutioResponse;
import com.stevenschoen.putionew.model.responses.FileResponse;
import com.stevenschoen.putionew.model.responses.FilesListResponse;
import com.stevenschoen.putionew.model.responses.FilesSearchResponse;
import com.stevenschoen.putionew.model.responses.Mp4StatusResponse;
import com.stevenschoen.putionew.model.responses.SubtitlesListResponse;
import com.stevenschoen.putionew.model.responses.TransfersListResponse;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PutioRestInterface {
	@GET("files/list")
	Single<FilesListResponse> files(@Query("parent_id") long parentId);

	@GET("files/search/{query}/page/-1")
	Single<FilesSearchResponse> searchFiles(@Path("query") String query);

	@GET("files/{id}")
	Single<FileResponse> file(@Path("id") long id);

	@GET("files/{id}/subtitles")
	Single<SubtitlesListResponse> subtitles(@Path("id") long id);

	@FormUrlEncoded
	@GET("files/zip")
	BasePutioResponse zip(@Field("file_ids") String ids);

	@GET("files/{id}/mp4")
	Single<Mp4StatusResponse> mp4Status(@Path("id") long id);

	@POST("files/{id}/mp4")
	Single<BasePutioResponse> convertToMp4(@Path("id") long id);

    @FormUrlEncoded
    @POST("files/create-folder")
    Single<BasePutioResponse.FileChangingResponse> createFolder(@Field("name") String name, @Field("parent_id") long parentId);

    @FormUrlEncoded
	@POST("files/rename")
	Single<BasePutioResponse.FileChangingResponse> renameFile(@Field("file_id") long id, @Field("name") String name);

	@FormUrlEncoded
	@POST("files/delete")
	Single<BasePutioResponse.FileChangingResponse> deleteFile(@Field("file_ids") String ids);

    @FormUrlEncoded
    @POST("files/move")
    Single<BasePutioResponse.FileChangingResponse> moveFile(@Field("file_ids") String ids, @Field("parent_id") long newParentId);

	@GET("transfers/list")
	Single<TransfersListResponse> transfers();

	@FormUrlEncoded
	@POST("transfers/add")
	Single<PutioTransfer> addTransferUrl(@Field("url") String url, @Field("extract") boolean extract, @Field("save_parent_id") long saveParentId);

    @FormUrlEncoded
    @POST("transfers/retry")
    Single<BasePutioResponse> retryTransfer(@Field("id") long id);

	@FormUrlEncoded
	@POST("transfers/cancel")
	Single<BasePutioResponse> cancelTransfer(@Field("transfer_ids") String ids);

	@POST("transfers/clean")
	Single<BasePutioResponse> cleanTransfers(@Body String nothing);

	@GET("account/info")
	Single<AccountInfoResponse> account();
}
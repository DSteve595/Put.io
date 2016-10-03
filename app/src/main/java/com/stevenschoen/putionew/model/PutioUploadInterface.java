package com.stevenschoen.putionew.model;

import com.stevenschoen.putionew.model.responses.PutioTransferFileUploadResponse;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import rx.Observable;

public interface PutioUploadInterface {
	@Multipart
	@POST("files/upload")
	Observable<PutioTransferFileUploadResponse> uploadFile(@Part MultipartBody.Part file, @Part("parent_id") long parentId);
}
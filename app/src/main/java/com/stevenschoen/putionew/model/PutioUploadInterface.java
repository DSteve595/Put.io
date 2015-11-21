package com.stevenschoen.putionew.model;

import com.stevenschoen.putionew.model.responses.PutioTransferFileUploadResponse;

import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedFile;
import rx.Observable;

public interface PutioUploadInterface {
	@Multipart
	@POST("/files/upload")
	Observable<PutioTransferFileUploadResponse> uploadFile(@Part("file") TypedFile file, @Part("parent_id") long parentId);
}
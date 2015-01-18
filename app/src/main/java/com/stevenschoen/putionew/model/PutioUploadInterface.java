package com.stevenschoen.putionew.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.squareup.okhttp.Response;
import com.stevenschoen.putionew.PutioUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;

import retrofit.Callback;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedFile;

public interface PutioUploadInterface {
	@Multipart
	@POST("/files/upload")
	void uploadFile(@Part("file") TypedFile file, @Part("parent_id") long parentId, Callback<Response> callback);

    public static class PostUploadFileJob extends PutioRestInterface.PutioUploadJob {
        private PutioUploadInterface uploadInterface;

		private Uri fileUri;
        private long parentId;

		public PostUploadFileJob(PutioUtils utils, PutioUploadInterface uploadInterface, Context context, Intent retryIntent, Uri fileUri, long parentId) {
			super(utils, context, retryIntent);
            this.uploadInterface = uploadInterface;

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
				uploadInterface.uploadFile(
						new TypedFile("application/x-bittorrent", file), parentId, this);
			} catch (Exception e) {
				file.delete();
				throw e;
			}
		}
	}
}
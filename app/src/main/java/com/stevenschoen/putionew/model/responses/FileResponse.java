package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFileData;

public class FileResponse {
	private String status;
	private PutioFileData file;

	public String getStatus() {
		return status;
	}

	public PutioFileData getFile() {
		return file;
	}
}

package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFileData;

public class FileResponse extends BasePutioResponse {
	private PutioFileData file;

	public PutioFileData getFile() {
		return file;
	}
}

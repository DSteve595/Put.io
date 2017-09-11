package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.ResponseOrError;
import com.stevenschoen.putionew.model.files.PutioFile;

public class FileResponse extends ResponseOrError.BasePutioResponse {
	private PutioFile file;

	public PutioFile getFile() {
		return file;
	}
}

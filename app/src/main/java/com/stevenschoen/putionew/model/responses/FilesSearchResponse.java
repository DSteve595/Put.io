package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFileData;

import java.util.List;

public class FilesSearchResponse extends BasePutioResponse {
	private List<PutioFileData> files;

	public List<PutioFileData> getFiles() {
		return files;
	}
}

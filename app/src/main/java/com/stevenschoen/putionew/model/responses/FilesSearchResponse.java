package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFileData;

import java.util.List;

public class FilesSearchResponse {
	private String status;
	private List<PutioFileData> files;

	public String getStatus() {
		return status;
	}

	public List<PutioFileData> getFiles() {
		return files;
	}
}

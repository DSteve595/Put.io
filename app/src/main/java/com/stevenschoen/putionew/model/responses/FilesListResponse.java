package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFileData;

import java.util.List;

public class FilesListResponse {
	private String status;
	private List<PutioFileData> files;
	private PutioFileData parent;

	public String getStatus() {
		return status;
	}

	public List<PutioFileData> getFiles() {
		return files;
	}

	public PutioFileData getParent() { return parent; }
}

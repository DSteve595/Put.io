package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFileData;

import java.util.List;

public class FilesListResponse extends BasePutioResponse {
	private List<PutioFileData> files;
	private PutioFileData parent;

	public List<PutioFileData> getFiles() {
		return files;
	}

	public PutioFileData getParent() { return parent; }
}

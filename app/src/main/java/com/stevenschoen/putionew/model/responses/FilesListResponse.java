package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFile;

import java.util.List;

public class FilesListResponse extends BasePutioResponse {
	private List<PutioFile> files;
	private PutioFile parent;

	public List<PutioFile> getFiles() {
		return files;
	}

	public PutioFile getParent() { return parent; }
}

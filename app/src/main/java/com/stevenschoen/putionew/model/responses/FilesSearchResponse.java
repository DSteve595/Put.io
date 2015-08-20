package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFile;

import java.util.List;

public class FilesSearchResponse extends BasePutioResponse {
	private List<PutioFile> files;

	public List<PutioFile> getFiles() {
		return files;
	}
}

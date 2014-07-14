package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFileData;

import java.util.List;

public class FilesSearchResponse extends BasePutioResponse {
	private List<PutioFileData> files;

    private String query;

	public List<PutioFileData> getFiles() {
		return files;
	}

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}

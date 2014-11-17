package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.files.PutioFile;

import java.util.List;

public class FilesSearchResponse extends BasePutioResponse {
	private List<PutioFile> files;

    private String query;

	public List<PutioFile> getFiles() {
		return files;
	}

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}

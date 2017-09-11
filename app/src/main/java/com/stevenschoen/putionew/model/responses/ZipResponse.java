package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.ResponseOrError;

public class ZipResponse extends ResponseOrError.BasePutioResponse {
	private String url;
	private long size;

	public String getUrl() {
		return url;
	}

	public long getSize() {
		return size;
	}
}

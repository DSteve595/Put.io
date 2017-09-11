package com.stevenschoen.putionew.model.responses;

import com.google.gson.annotations.SerializedName;
import com.stevenschoen.putionew.model.ResponseOrError;
import com.stevenschoen.putionew.model.files.PutioSubtitle;

import java.util.List;

public class SubtitlesListResponse extends ResponseOrError.BasePutioResponse {
	private List<PutioSubtitle> subtitles;
    @SerializedName("default")
    private String defaultSubtitleKey;

	public List<PutioSubtitle> getSubtitles() {
		return subtitles;
	}

    public String getDefaultSubtitleKey() {
        return defaultSubtitleKey;
    }
}
package com.stevenschoen.putionew.model.responses

import com.google.gson.annotations.SerializedName
import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioSubtitle

class SubtitlesListResponse(
    val subtitles: List<PutioSubtitle>,
    @SerializedName("default")
    val defaultSubtitleKey: String
) : ResponseOrError.BasePutioResponse()

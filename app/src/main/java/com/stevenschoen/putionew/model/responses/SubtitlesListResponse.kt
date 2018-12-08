package com.stevenschoen.putionew.model.responses

import com.squareup.moshi.Json
import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioSubtitle

class SubtitlesListResponse(
    val subtitles: List<PutioSubtitle>,
    @Json(name = "default")
    val defaultSubtitleKey: String
) : ResponseOrError.BasePutioResponse()

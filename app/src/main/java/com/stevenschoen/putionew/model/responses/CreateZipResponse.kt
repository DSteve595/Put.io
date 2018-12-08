package com.stevenschoen.putionew.model.responses

import com.squareup.moshi.Json
import com.stevenschoen.putionew.model.ResponseOrError

class CreateZipResponse(
    @Json(name = "zip_id")
    val zipId: Long
) : ResponseOrError.BasePutioResponse()

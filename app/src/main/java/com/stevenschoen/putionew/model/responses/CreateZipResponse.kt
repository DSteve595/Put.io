package com.stevenschoen.putionew.model.responses

import com.google.gson.annotations.SerializedName
import com.stevenschoen.putionew.model.ResponseOrError

class CreateZipResponse(
    @SerializedName("zip_id")
    val zipId: Long
) : ResponseOrError.BasePutioResponse()

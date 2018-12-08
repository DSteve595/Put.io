package com.stevenschoen.putionew.model.responses

import com.stevenschoen.putionew.model.ResponseOrError

class ZipResponse(
    val url: String,
    val size: Long
) : ResponseOrError.BasePutioResponse()

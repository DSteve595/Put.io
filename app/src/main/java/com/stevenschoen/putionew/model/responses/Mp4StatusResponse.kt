package com.stevenschoen.putionew.model.responses

import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioMp4Status

class Mp4StatusResponse(
    val mp4Status: PutioMp4Status
) : ResponseOrError.BasePutioResponse()

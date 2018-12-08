package com.stevenschoen.putionew.model.responses

import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioFile

class FileResponse(
    val file: PutioFile
) : ResponseOrError.BasePutioResponse()

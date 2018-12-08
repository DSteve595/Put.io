package com.stevenschoen.putionew.model.responses

import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioFile

class FilesSearchResponse(
    val files: List<PutioFile>
) : ResponseOrError.BasePutioResponse()

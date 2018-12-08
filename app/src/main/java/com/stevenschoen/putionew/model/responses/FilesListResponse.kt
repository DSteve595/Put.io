package com.stevenschoen.putionew.model.responses

import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioFile

open class FilesListResponse(
    val files: List<PutioFile>,
    val parent: PutioFile
) : ResponseOrError.BasePutioResponse()

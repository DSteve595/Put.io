package com.stevenschoen.putionew.model

sealed class ResponseOrError {
    open class BasePutioResponse : ResponseOrError() {
        val status: String? = null

        class FileChangingResponse : BasePutioResponse()
        class PutioTransferFileUploadResponse : BasePutioResponse()
    }

    class NetworkError(val error: Throwable) : ResponseOrError()
}
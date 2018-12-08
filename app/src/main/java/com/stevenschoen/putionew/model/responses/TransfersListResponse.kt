package com.stevenschoen.putionew.model.responses

import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.transfers.PutioTransfer

class TransfersListResponse(
    val transfers: List<PutioTransfer>
) : ResponseOrError.BasePutioResponse()

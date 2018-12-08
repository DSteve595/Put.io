package com.stevenschoen.putionew.model.responses

import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.account.PutioAccountInfo

class AccountInfoResponse(
    val info: PutioAccountInfo
) : ResponseOrError.BasePutioResponse()

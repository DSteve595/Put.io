package com.stevenschoen.putionew.model.transfers

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PutioTransfer(
    val id: Long,
    val fileId: Long,
    val size: Long,
    val name: String?,
    val estimatedTime: Long?,
    val createdTime: String?,
    val extract: Boolean,
    val currentRatio: Float?,
    val downSpeed: Long?,
    val upSpeed: Long?,
    val percentDone: Int?,
    val status: String?,
    val statusMessage: String?,
    val saveParentId: Long
) : Parcelable

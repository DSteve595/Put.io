package com.stevenschoen.putionew.model.transfers

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PutioTransfer(
    val id: Long,
    @Json(name = "file_id")
    val fileId: Long,
    val size: Long,
    val name: String?,
    @Json(name = "estimated_time")
    val estimatedTime: Long?,
    @Json(name = "created_time")
    val createdTime: String?,
    val extract: Boolean,
    @Json(name = "current_ratio")
    val currentRatio: Float?,
    @Json(name = "down_speed")
    val downSpeed: Long?,
    @Json(name = "up_speed")
    val upSpeed: Long?,
    @Json(name = "percent_done")
    val percentDone: Int?,
    val status: String?,
    @Json(name = "status_message")
    val statusMessage: String?,
    @Json(name = "save_parent_id")
    val saveParentId: Long
) : Parcelable

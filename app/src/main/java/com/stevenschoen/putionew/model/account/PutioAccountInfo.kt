package com.stevenschoen.putionew.model.account

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PutioAccountInfo(
    val username: String,
    val mail: String,
    val disk: DiskInfo
) : Parcelable {

  @Parcelize
  data class DiskInfo(
      val avail: Long,
      val size: Long,
      val used: Long
  ) : Parcelable

}

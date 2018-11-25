package com.stevenschoen.putionew.model.files

import android.content.res.Resources
import android.os.Parcelable
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PutioFile(
    val name: String,
    val id: Long,
    val isShared: Boolean? = false,
    val icon: String? = null,
    val screenshot: String? = null,
    val createdAt: String? = null,
    val firstAccessedAt: String? = null,
    val parentId: Long? = null,
    val isMp4Available: Boolean? = null,
    val contentType: String,
    val size: Long? = null,
    val crc32: String? = null
) : Parcelable {

  val isFolder: Boolean get() = (contentType == CONTENT_TYPE_FOLDER)

  val isMedia: Boolean
    get() {
      return PutioUtils.streamingMediaTypes.any {
        contentType.startsWith(it)
      }
    }

  val isVideo: Boolean get() = (contentType.startsWith("video"))

  val isMp4: Boolean get() = (contentType == "video/mp4")

  val isAccessed: Boolean get() = (!firstAccessedAt.isNullOrEmpty())

  fun getStreamUrl(utils: PutioUtils, mp4: Boolean): String {
    val base = PutioUtils.baseUrl + "files/" + id
    val streamOrStreamMp4 = if (mp4 && !isMp4) {
      "/mp4/stream"
    } else {
      "/stream"
    }

    return base + streamOrStreamMp4 + utils.tokenWithStuff
  }

  fun getDownloadUrl(utils: PutioUtils, mp4: Boolean = false): String {
    val base = PutioUtils.baseUrl + "files/" + id
    val downloadOrDownloadMp4 = if (mp4 && !isMp4) {
      "/mp4/download"
    } else {
      "/download"
    }

    return base + downloadOrDownloadMp4 + utils.tokenWithStuff
  }

  companion object {
    const val CONTENT_TYPE_FOLDER = "application/x-directory"

    fun makeRootFolder(resources: Resources): PutioFile {
      return PutioFile(
          id = 0,
          name = resources.getString(R.string.files),
          contentType = CONTENT_TYPE_FOLDER
      )
    }

  }
}

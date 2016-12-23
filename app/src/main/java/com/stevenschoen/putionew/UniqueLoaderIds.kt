package com.stevenschoen.putionew

import com.stevenschoen.putionew.files.FileScreenshotLoader
import com.stevenschoen.putionew.files.FolderLoader
import com.stevenschoen.putionew.files.Mp4StatusLoader
import com.stevenschoen.putionew.files.SearchLoader

fun getUniqueLoaderId(loaderClass: Class<out PutioBaseLoader>): Int {
    return when (loaderClass) {
        FolderLoader::class.java -> 1
        SearchLoader::class.java -> 2
        Mp4StatusLoader::class.java -> 3
        FileScreenshotLoader::class.java -> 4
        else -> throw RuntimeException("Couldn't find loader ID for $loaderClass")
    }
}
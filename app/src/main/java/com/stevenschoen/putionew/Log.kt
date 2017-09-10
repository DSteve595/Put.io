package com.stevenschoen.putionew

import android.util.Log

fun Any.log(message: String) {
    Log.d(this::class.java.simpleName.take(23), message)
}
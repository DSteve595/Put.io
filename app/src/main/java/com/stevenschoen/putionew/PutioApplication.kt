package com.stevenschoen.putionew

import android.arch.persistence.room.Room
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.support.multidex.MultiDexApplication
import android.support.v4.app.Fragment
import com.akaita.java.rxjava2debug.RxJava2Debug
import com.crashlytics.android.Crashlytics
import com.stevenschoen.putionew.files.FileDownloadDatabase
import com.stevenschoen.putionew.model.files.PutioFile
import io.fabric.sdk.android.Fabric
import net.danlew.android.joda.JodaTimeAndroid

class PutioApplication : MultiDexApplication() {

    var putioUtils: PutioUtils? = null
        get() {
            if (field == null) {
                try {
                    field = PutioUtils(this)
                } catch (e: PutioUtils.NoTokenException) {
                    return null
                }
            }
            return field
        }
    val fileDownloadDatabase by lazy {
        Room.databaseBuilder(this, FileDownloadDatabase::class.java, "fileDownloads").fallbackToDestructiveMigration().build()
    }

    override fun onCreate() {
        super.onCreate()

        Fabric.with(this, Crashlytics())

        JodaTimeAndroid.init(this)

        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannels(this)
        }

        if (BuildConfig.DEBUG) {
            RxJava2Debug.enableRxJava2AssemblyTracking()
        }
    }

    val isLoggedIn: Boolean
        get() {
            if (putioUtils == null) return false

            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            val token: String? = sharedPrefs.getString("token", null)
            return !token.isNullOrEmpty()
        }

    interface CastCallbacks {
        fun load(file: PutioFile, url: String, utils: PutioUtils)
    }
}

fun putioApp(context: Context) = context.applicationContext as PutioApplication
val Context.putioApp
    get() = putioApp(this)
val Fragment.putioApp
    get() = putioApp(context)
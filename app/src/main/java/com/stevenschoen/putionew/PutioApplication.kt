package com.stevenschoen.putionew

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import com.stevenschoen.putionew.files.FileDownloadDatabase
import com.stevenschoen.putionew.model.files.PutioFile
import net.danlew.android.joda.JodaTimeAndroid
import timber.log.Timber

class PutioApplication : Application() {

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
        Room.databaseBuilder(this, FileDownloadDatabase::class.java, "fileDownloads")
                .fallbackToDestructiveMigration()
                .build()
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }

        JodaTimeAndroid.init(this)

        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannels(this)
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
        fun isCasting(): Boolean
    }
}

fun putioApp(context: Context) = context.applicationContext as PutioApplication
val Context.putioApp
    get() = putioApp(this)
val Fragment.putioApp
    get() = putioApp(context!!)

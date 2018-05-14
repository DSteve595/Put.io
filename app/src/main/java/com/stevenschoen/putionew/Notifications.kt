package com.stevenschoen.putionew

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.support.annotation.RequiresApi

const val NOTIFICATION_CHANNEL_ID_TRANSFERS = "transfers"

@RequiresApi(26)
fun createNotificationChannels(context: Context) {
  val notificationManager = context.applicationContext.getSystemService(NotificationManager::class.java)

  val transfersChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID_TRANSFERS,
      context.getString(R.string.transfers),
      NotificationManager.IMPORTANCE_LOW)
  transfersChannel.enableVibration(false)
  notificationManager.createNotificationChannel(transfersChannel)
}

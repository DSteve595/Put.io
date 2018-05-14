package com.stevenschoen.putionew;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OpenDownloadReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent dm = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
    dm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(dm);
  }
}

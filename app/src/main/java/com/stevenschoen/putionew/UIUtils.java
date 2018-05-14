package com.stevenschoen.putionew;

import android.content.Context;
import android.os.Build;

public class UIUtils {

  private UIUtils() {
  }

  public static boolean isTV(Context context) {
    return context.getPackageManager().hasSystemFeature(
        "android.hardware.type.television");
  }

  public static boolean hasLollipop() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }

  public static boolean isTablet(Context context) {
    return (context.getResources().getBoolean(R.bool.split_layout));
  }
}

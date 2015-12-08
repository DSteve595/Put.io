package com.stevenschoen.putionew;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

public class UIUtils {

	public static boolean isTV(Context context) {
		return context.getPackageManager().hasSystemFeature(
				"android.hardware.type.television");
	}

	public static boolean hasJellyBean() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}

    public static boolean hasJellyBeanMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean hasJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

	public static boolean isTablet(Context context) {
		return (context.getResources().getBoolean(R.bool.split_layout));
	}
}
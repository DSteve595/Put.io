package com.stevenschoen.putionew.cast;

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.stevenschoen.putionew.ExpandedControlsActivity;

import java.util.List;

public class CastOptionsProvider implements OptionsProvider {

  public static final String CAST_APPLICATION_ID = "E5977464"; // Styled media receiver
//	public static final String CAST_APPLICATION_ID = "C18ACC9E";
//	public static final String CAST_APPLICATION_ID = "79E32AF2"; // Put.io's

  public static boolean isCastSdkAvailable(Context context) {
    return (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS);
  }

  @Override
  public CastOptions getCastOptions(Context context) {
    return new CastOptions.Builder()
        .setReceiverApplicationId(CAST_APPLICATION_ID)
        .setCastMediaOptions(new CastMediaOptions.Builder()
            .setNotificationOptions(new NotificationOptions.Builder()
                .setTargetActivityClassName(ExpandedControlsActivity.class.getName())
                .build())
            .setExpandedControllerActivityClassName(ExpandedControlsActivity.class.getName())
            .build())
        .build();
  }

  @Override
  public List<SessionProvider> getAdditionalSessionProviders(Context context) {
    return null;
  }
}

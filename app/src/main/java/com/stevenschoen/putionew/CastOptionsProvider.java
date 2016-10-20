package com.stevenschoen.putionew;

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;

import java.util.List;

public class CastOptionsProvider implements OptionsProvider {

	@Override
	public CastOptions getCastOptions(Context context) {
		return new CastOptions.Builder()
				.setReceiverApplicationId(PutioUtils.CAST_APPLICATION_ID)
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
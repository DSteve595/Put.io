package com.stevenschoen.putionew.cast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

import com.google.cast.ApplicationMetadata;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.ContentMetadata;
import com.google.cast.MediaProtocolMessageStream;
import com.google.cast.MediaProtocolMessageStream.PlayerState;
import com.google.cast.MediaRouteAdapter;
import com.google.cast.MediaRouteHelper;
import com.google.cast.MediaRouteStateChangeListener;
import com.google.cast.SessionError;
import com.stevenschoen.putionew.PutioFileData;
import com.stevenschoen.putionew.cast.PutioMessageStream.OnStatusUpdateListener;

public class CastService extends Service implements MediaRouteAdapter {
	
//    protected static final double MAX_VOLUME_LEVEL = 20;
    private static final double VOLUME_INCREMENT = 0.05;
	
	private final IBinder binder = new CastServiceBinder();
	private TimerTask stopTask;
	
	private CastContext castContext;
	private MediaRouter mediaRouter;
	private MediaRouteSelector mediaRouteSelector;
	private CastDevice selectedDevice;
    private MediaRouteStateChangeListener routeStateListener;
    private PutioMessageStream messageStream;
    private ApplicationSession session;
    
    private ArrayList<CastUpdateListener> castUpdateListeners = new ArrayList<CastUpdateListener>();

	@Override
	public IBinder onBind(Intent intent) {
		if (stopTask != null) stopTask.cancel();
		return binder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		castContext = new CastContext(getApplicationContext());
		mediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector(MediaRouteHelper.CATEGORY_CAST);
		mediaRouter = MediaRouter.getInstance(getApplicationContext());
		mediaRouter.addCallback(mediaRouteSelector, MediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_NOT_STICKY;
	}
	
	private void openChannel() {
		messageStream = new PutioMessageStream();
		messageStream.setOnStatusUpdateListener(new OnStatusUpdateListener() {
			@Override
			public void onStatusUpdated() {
				for (CastUpdateListener listener : castUpdateListeners) {
					listener.onInvalidate();
				}
			}
		});
		session.getChannel().attachMessageStream(messageStream);
	}
	
	public void loadAndPlayMedia(String title, String url) {
		if (messageStream != null) {
			ContentMetadata data = new ContentMetadata();
			data.setTitle(title);
			try {
				messageStream.loadMedia(url, data, true);
			} catch (IllegalStateException e) {
				e.printStackTrace();
				loadAndPlayMedia(title, url);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (CastUpdateListener listener : castUpdateListeners) {
			listener.onMediaPlay();
		}
	}
	
	public void mediaPlay() {
		if (messageStream != null) {
			try {
				messageStream.resume();
				for (CastUpdateListener listener : castUpdateListeners) {
					listener.onMediaPlay();
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void mediaPause() {
		if (messageStream != null) {
			try {
				messageStream.stop();
				for (CastUpdateListener listener : castUpdateListeners) {
					listener.onMediaPause();
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private MediaRouter.Callback MediaRouterCallback = new MediaRouter.Callback() {
		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo route) {
			MediaRouteHelper.requestCastDeviceForRoute(route);
		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo route) {
			if (session != null) {
				try {
					if (messageStream != null && session.hasChannel()) {
						messageStream.stop();
					}
					session.endSession();
					messageStream = null;
				} catch (IOException e) { }
			}
			selectedDevice = null;
			routeStateListener = null;
			for (CastUpdateListener listener : castUpdateListeners) {
				listener.onInvalidate();
			}
		}
	};
	
	public boolean isCasting() {
		return (selectedDevice != null);
	}

	@Override
	public void onDeviceAvailable(CastDevice device, String routeId, MediaRouteStateChangeListener listener) {
		selectedDevice = device;
        routeStateListener = listener;
        
    	try {
			session.resumeSession();
		} catch (RuntimeException e) {
			session = new ApplicationSession(castContext, selectedDevice);
			int flags = 0;
	        // Comment out the below line if you are not writing your own Notification Screen.
	        // flags |= ApplicationSession.FLAG_DISABLE_NOTIFICATION;
	        // Comment out the below line if you are not writing your own Lock Screen.
	        // flags |= ApplicationSession.FLAG_DISABLE_LOCK_SCREEN_REMOTE_CONTROL;
			session.setApplicationOptions(flags);
			session.setStopApplicationWhenEnding(true);
			ApplicationSession.Listener sessionListener = new ApplicationSession.Listener() {
			    @Override
				public void onSessionStarted(ApplicationMetadata appMetadata) {
					openChannel();
			    }

			    @Override
				public void onSessionStartFailed(SessionError error) {
			        Log.d("asdf", "onSessionStartFailed, category: " + error.getCategory() + "code: " + error.getCode());
			    }

			    @Override
				public void onSessionEnded(SessionError error) {
			        if (error != null) {
			            // The session ended due to an error.
			        } else {
			            // The session ended normally.
			        }
			    }
			};
			
			session.setListener(sessionListener);
			
			try {
//				session.startSession("PutioStevenSchoen"); // for leapcast
				session.startSession("4a67e184-0d21-4ecc-b87d-bff1f04b6abd_1"); // for google
//                session.startSession("4a67e184-0d21-4ecc-b87d-bff1f04b6abd"); // for eureka
			} catch (IllegalStateException ee) {
				ee.printStackTrace();
			} catch (IOException ee) {
				ee.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onSetVolume(double volume) {
		try {
			messageStream.setVolume(volume);
			routeStateListener.onVolumeChanged(volume);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void volumeUp() {
        if (messageStream != null) {
        	double currentVolume = messageStream.getVolume();
            if (currentVolume < 1.0) {
                onSetVolume(currentVolume + VOLUME_INCREMENT);
            }
        }
	}
	
	public void volumeDown() {
        if (messageStream != null) {
        	double currentVolume = messageStream.getVolume();
            if (currentVolume > 0.0) {
                onSetVolume(currentVolume - VOLUME_INCREMENT);
            }
        }
	}

	@Override
	public void onUpdateVolume(double delta) {
        if (messageStream != null) {
        	double currentVolume = messageStream.getVolume();
            onSetVolume(currentVolume + delta);
        }
	}
	
	public CastContext getCastContext() {
		return castContext;
	}
	
	public MediaRouteSelector getMediaRouteSelector() {
		return mediaRouteSelector;
	}
	
	public CastDevice getSelectedDevice() {
		return selectedDevice;
	}
	
	public MediaProtocolMessageStream getMessageStream() {
		return messageStream;
	}
	
	public boolean hasMediaLoaded() {
		return (messageStream != null && messageStream.getContentId() != null);
	}
	
	public boolean isPlaying() {
		if (messageStream != null) {
			return (messageStream.getPlayerState() == PlayerState.PLAYING);
		}
		return false;
	}
	
    public void addListener(CastUpdateListener listener) {
    	listener.onInvalidate();
    	castUpdateListeners.add(listener);
    }
    
    public void removeListener(CastUpdateListener listener) {
    	castUpdateListeners.remove(listener);
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	if (!isCasting()) {
    		stopTask = new TimerTask() {
    			@Override
    			public void run() {
    				if (!isCasting()) {
    					stopSelf();
    				}
    			}
    		};
    		new Timer().schedule(stopTask, 5000);
    	}
		return true;
    }
    
    @Override
    public void onRebind(Intent intent) {
    	if (stopTask != null) stopTask.cancel();
    	super.onRebind(intent);
    }
    
    @Override
    public void onDestroy() {
    	Log.d("asdf", "onDestroy");
    	if (session != null) {
        	try {
    			session.endSession();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    	castContext.dispose();
    	
    	super.onDestroy();
    }
	
	public class CastServiceBinder extends Binder {
        public CastService getService() {
            return CastService.this;
        }
    }
	
    public interface CastCallbacks {
        public void load(PutioFileData file, String url);
    }
    
    public interface CastUpdateListener {
    	public void onInvalidate();
    	public void onMediaPlay();
    	public void onMediaPause();
    }
}
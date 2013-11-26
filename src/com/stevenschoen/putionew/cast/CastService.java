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
		Log.d("asdf", "onCreate");
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
				openChannel();
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
					session.endSession();
				} catch (IOException e) { }
			}
			messageStream = null;
			selectedDevice = null;
			routeStateListener = null;
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
			ApplicationSession.Listener sessionListener = new ApplicationSession.Listener() {
			    @Override
				public void onSessionStarted(ApplicationMetadata appMetadata) {
					openChannel();
			    }

			    @Override
				public void onSessionStartFailed(SessionError error) {
			        // The session could not be started.
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
		} catch (IOException e) {
			e.printStackTrace();
		}
		
    	if (session.hasStopped()) {
			try {
	    		session.startSession("PutioStevenSchoen");
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
	}

	@Override
	public void onSetVolume(double volume) {
		
	}

	@Override
	public void onUpdateVolume(double delta) {
		
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
package com.stevenschoen.putionew.activities;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.view.ViewHelper;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;

public class Setup extends SherlockActivity {
	public SharedPreferences sharedPrefs;
	private WebView loginWebView;
	
	private int viewMode = 0;
	public static final int VIEWMODE_LOGIN = 1;
	public static final int VIEWMODE_LOADING = 2;
	public static final int VIEWMODE_NONETWORK = 3;
	
	private View viewLoading;
	
	private View viewNoNetwork;
	private TextView textNoConnection;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		final String loginUrl = "https://api.put.io/v2/oauth2/authenticate?client_id=83&response_type=token&redirect_uri=http://stevenschoen.com/callback.php";

		StrictMode.setThreadPolicy(new ThreadPolicy.Builder().permitNetwork().build());
		
		loginWebView = (WebView) findViewById(R.id.webview_setup);
		loginWebView.setVisibility(View.INVISIBLE);
		loginWebView.getSettings().setJavaScriptEnabled(true);
		loginWebView.setWebViewClient(new LoginWebViewClient());
		
		loginWebView.loadUrl(loginUrl);
		
		viewLoading = findViewById(R.id.loading_setup);
		viewLoading.setVisibility(View.INVISIBLE);
		
		viewNoNetwork = findViewById(R.id.view_setup_noconnection);
		viewNoNetwork.setVisibility(View.INVISIBLE);
		ViewHelper.setAlpha(viewNoNetwork, 0);
		
		Typeface robotoLight = Typeface.createFromAsset(this.getAssets(), "Roboto-Light.ttf");
		textNoConnection = (TextView) findViewById(R.id.text_setup_noconnection);
		textNoConnection.setTypeface(robotoLight);
		
		setViewMode(VIEWMODE_LOADING);
	}
	
	private class LoginWebViewClient extends WebViewClient {
		boolean error = false;
		
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			if (!error) {
				setViewMode(VIEWMODE_LOGIN);
			}
			loginWebView.postDelayed(new Runnable() {
				@Override
				public void run() {
					error = false;
				}
			}, 500);
		}
		 
		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			setViewMode(VIEWMODE_NONETWORK);
			error = true;
		}
		
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
    		view.loadUrl(url);
    		return true;
	    }
	    
	    @Override
		public void onLoadResource(WebView view, String url) {
	    	if (url.contains("code=")) {
				String[] strings = url.split("code=");
				String code = strings[1];

				final String finalUrl = new String(
						"https://api.put.io/v2/oauth2/access_token?client_id=83&client_secret=6xf3yaxu62uj1cjbzfvz&grant_type=authorization_code&redirect_uri=http://stevenschoen.com/callback.php&code="
								+ code);
				saveTokenFromWeb(finalUrl);
			} else if (url.contains("token=")) {
				saveToken(url.substring(url.indexOf("token=") + 6));
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem buttonTokenLogin = menu.add("Alternate login");
		buttonTokenLogin.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		buttonTokenLogin.setIcon(android.R.drawable.ic_menu_preferences);
		buttonTokenLogin.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				Dialog tokenLoginDialog = new Dialog(Setup.this, R.style.Putio_Dialog);
				tokenLoginDialog.setContentView(R.layout.dialog_tokenlogin);
				return false;
			}
		});
		
		return true;
	}
	
	private void setViewMode(int mode) {
		if (mode != viewMode) {
			if (viewMode != 0) {
				View viewToRemove = null;
				switch (this.viewMode) {
				case VIEWMODE_LOGIN:
					viewToRemove = loginWebView;
					break;
				case VIEWMODE_LOADING:
					viewToRemove = viewLoading;
					break;
				case VIEWMODE_NONETWORK:
					viewToRemove = viewNoNetwork;
					break;
				}
				final View viewToRemove2 = viewToRemove;
				animate(viewToRemove).setDuration(500).alpha(0).setListener(new AnimatorListener() {
					@Override
					public void onAnimationStart(Animator animation) {}
					@Override
					public void onAnimationRepeat(Animator animation) {}
					@Override
					public void onAnimationEnd(Animator animation) {
						viewToRemove2.setVisibility(View.INVISIBLE);
					}
					@Override
					public void onAnimationCancel(Animator animation) {}
				});
			}
			
			View viewToAdd = null;
			switch (mode) {
			case VIEWMODE_LOGIN:
				viewToAdd = loginWebView;
				break;
			case VIEWMODE_LOADING:
				viewToAdd = viewLoading;
				break;
			case VIEWMODE_NONETWORK:
				viewToAdd = viewNoNetwork;
				break;
			}
			ViewHelper.setAlpha(viewToAdd, 0);
			viewToAdd.setVisibility(View.VISIBLE);
			animate(viewToAdd).setDuration(500).alpha(1);
			
			viewMode = mode;
		}
	}
	
	public InputStream getJsonData(String url) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		URI uri;
		InputStream data = null;
		try {
			uri = new URI(url);
			HttpGet method = new HttpGet(uri);
			HttpResponse response = httpClient.execute(method);
			data = response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	void saveTokenFromWeb(final String url) {
		String token = null;
		
		JSONObject json;
		try {
			json = new JSONObject(PutioUtils.convertStreamToString(getJsonData(url)));
			token = json.getString("access_token");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		saveToken(token);
	}
	
	private void saveToken(String token) {
		sharedPrefs.edit().putString("token", token).commit();
		sharedPrefs.edit().putBoolean("loggedIn", true).commit();
		Toast.makeText(this, R.string.loginsuccess, Toast.LENGTH_SHORT).show();
		setResult(RESULT_OK);
		
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		
		finish();
	}
}
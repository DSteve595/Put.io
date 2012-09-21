package com.stevenschoen.putio.activities.setup;

import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.stevenschoen.putio.R;

public class Setup extends SherlockActivity {
	public SharedPreferences sharedPrefs;
	private Dialog webDialog;
	private WebView loginWebView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		final String loginUrl = "https://api.put.io/v2/oauth2/authenticate?client_id=83&response_type=token&redirect_uri=http://stevenschoen.com/callback.php";

		StrictMode.setThreadPolicy(new ThreadPolicy.Builder().permitNetwork().build());
		
		Typeface robotoThin = Typeface.createFromAsset(this.getAssets(), "Roboto-Thin.ttf");
		Typeface robotoLight = Typeface.createFromAsset(this.getAssets(), "Roboto-Light.ttf");
		Typeface robotoBold = Typeface.createFromAsset(this.getAssets(), "Roboto-Bold.ttf");
		
		TextView introText1 = (TextView) findViewById(R.id.text_setup_intro1);
		introText1.setTypeface(robotoLight);
		
		webDialog = new Dialog(this, R.style.Putio_Dialog);
		webDialog.setCanceledOnTouchOutside(false);
		webDialog.setContentView(R.layout.dialog_login);
		webDialog.setTitle("Log in");
		
		Button loginButton = (Button) findViewById(R.id.button_setup_login);
		loginButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				webDialog.show();
			}
		});
		
		Button goToSiteButton = (Button) findViewById(R.id.button_setup_gotosite);
		goToSiteButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent goToSiteIntent = new Intent(Intent.ACTION_VIEW);
				goToSiteIntent.setData(Uri.parse("http://put.io/"));
				startActivity(goToSiteIntent);
			}
		});
		
		Button cancelLogin = (Button) webDialog.findViewById(R.id.button_login_cancel);
		cancelLogin.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				loginWebView.loadUrl(loginUrl);
				webDialog.cancel();
			}
		});
		
		loginWebView = (WebView) webDialog.findViewById(R.id.webview_login);
		loginWebView.getSettings().setJavaScriptEnabled(true);
		loginWebView.setVisibility(View.VISIBLE);
		loginWebView.setWebViewClient(new LoginWebViewClient());
		
		loginWebView.loadUrl(loginUrl);
	}
	
	private class LoginWebViewClient extends WebViewClient {
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
	
	public String convertStreamToString(InputStream is) {
	    try {
	        return new java.util.Scanner(is).useDelimiter("\\A").next();
	    } catch (java.util.NoSuchElementException e) {
	        return "";
	    }
	}
	
	void saveTokenFromWeb(final String url) {
		String token = null;
		
		JSONObject json;
		try {
			json = new JSONObject(convertStreamToString(getJsonData(url)));
			token = json.getString("access_token");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		webDialog.dismiss();
		saveToken(token);
	}
	
	private void saveToken(String token) {
		sharedPrefs.edit().putString("token", token).commit();
		sharedPrefs.edit().putBoolean("loggedIn", true).commit();
		Toast.makeText(this, R.string.loginsuccess, Toast.LENGTH_SHORT).show();
		setResult(RESULT_OK);
		finish();
	}
}
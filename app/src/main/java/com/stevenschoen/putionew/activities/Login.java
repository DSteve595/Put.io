package com.stevenschoen.putionew.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.ApiKey;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Login extends AppCompatActivity {
    public SharedPreferences sharedPrefs;
    private WebView loginWebView;

    private int viewMode = 0;
    public static final int VIEWMODE_LOGIN = 1;
    public static final int VIEWMODE_LOADING = 2;
    public static final int VIEWMODE_NONETWORK = 3;

    private View viewLoading;

    private View viewNoNetwork;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String loginUrl = "https://api.put.io/v2/oauth2/authenticate?client_id=" + ApiKey.getClientId() + "&response_type=token&redirect_uri=http://stevenschoen.com/callback.php";

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
        viewNoNetwork.setAlpha(0);

        setViewMode(VIEWMODE_LOADING);
    }

    private class LoginWebViewClient extends WebViewClient {
        boolean error = false;

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (url.contains("token=")) {
                saveToken(url.substring(url.indexOf("token=") + 6));
            }

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

                final String finalUrl = "https://api.put.io/v2/oauth2/access_token?client_id=" +
                        ApiKey.getClientId() + "&client_secret=" +
                        ApiKey.getApiKey() + "&grant_type=authorization_code&redirect_uri=http://stevenschoen.com/callback.php&code="
						+ code;
                saveTokenFromWeb(finalUrl);
            } else if (url.contains("token=")) {
                saveToken(url.substring(url.indexOf("token=") + 6));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//		MenuItem buttonTokenLogin = menu.add("Alternate login");
//		buttonTokenLogin.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//		buttonTokenLogin.setIcon(android.R.drawable.ic_menu_preferences);
//		buttonTokenLogin.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//			
//			public boolean onMenuItemClick(MenuItem item) {
//				Dialog tokenLoginDialog = new Dialog(Setup.this, R.style.Putio_Dialog);
//				tokenLoginDialog.setContentView(R.layout.dialog_tokenlogin);
//				return false;
//			}
//		});

        return super.onCreateOptionsMenu(menu);
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
                viewToRemove.animate().setDuration(500).alpha(0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewToRemove2.setVisibility(View.INVISIBLE);
                    }
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
            viewToAdd.setAlpha(0);
            viewToAdd.setVisibility(View.VISIBLE);
            viewToAdd.animate().setDuration(500).alpha(1);

            viewMode = mode;
        }
    }

    public String getJsonData(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(new Request.Builder()
                    .url(url).build()).execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    void saveTokenFromWeb(final String url) {
        String token = null;

        JSONObject json;
        try {
            json = new JSONObject(getJsonData(url));
            token = json.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        saveToken(token);
    }

    private void saveToken(String token) {
        sharedPrefs.edit().putString("token", token).commit();
        Toast.makeText(this, R.string.loginsuccess, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        finish();
    }
}
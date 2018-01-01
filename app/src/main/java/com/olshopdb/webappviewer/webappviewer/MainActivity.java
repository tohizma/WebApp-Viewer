package com.olshopdb.webappviewer.webappviewer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private WebView webAppViewer;
    private ProgressBar progressBar;
    public String appURL = "";
    private RequestPermissionHandler mRequestPermissionHandler;
    private String[] appPermissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webAppViewer = findViewById(R.id.webViewer);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.GONE);

        String configuredUrl = readAppSetting("URLSetting");
        Toast.makeText(getApplicationContext(), "Saved URL : " + configuredUrl, Toast.LENGTH_LONG).show();
        if (configuredUrl.equalsIgnoreCase("")) {
            appURL = "https://www.google.com";
        }
        else {
            appURL = configuredUrl;
        }
        //Toast.makeText(getApplicationContext(),"URL to Open : " + appURL, Toast.LENGTH_LONG).show();
        displayWebApp(appURL);
    }

    private void askPermissions() {
        mRequestPermissionHandler = new RequestPermissionHandler();
        mRequestPermissionHandler.requestPermission(this, appPermissions, 123, new RequestPermissionHandler.RequestPermissionListener() {
            @Override
            public void onSuccess() {
                //Toast.makeText(getApplicationContext(), "request permission success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed() {
                //Toast.makeText(getApplicationContext(), "request permission failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mRequestPermissionHandler.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
    }

    public void displayWebApp(String url) {

        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ Permission APIs
            askPermissions();
        }

        webAppViewer.setWebViewClient(new GeoWebViewClient());
        webAppViewer.setWebChromeClient(new GeoWebChromeClient());

        WebSettings myBrowserSettings = webAppViewer.getSettings();
        myBrowserSettings.setJavaScriptEnabled(true);
        myBrowserSettings.setGeolocationEnabled(true);
        myBrowserSettings.setGeolocationDatabasePath(getFilesDir().getPath());
        myBrowserSettings.setAppCacheEnabled(true);
        myBrowserSettings.setDatabaseEnabled(true);
        myBrowserSettings.setDomStorageEnabled(true);

        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        myBrowserSettings.setAppCachePath(cacheDir.getPath());
        myBrowserSettings.setAppCacheMaxSize(1024*1024*8);
        myBrowserSettings.setAllowFileAccess(true);
        myBrowserSettings.setAppCacheEnabled(true);
        myBrowserSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // ===== Cookies ======
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT > 20){
            CookieManager.getInstance().setAcceptThirdPartyCookies(webAppViewer, true);
        }
        // ===== End Cookies ===

        registerForContextMenu(webAppViewer);
        webAppViewer.loadUrl(url);
    }

    public class GeoWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // When user clicks a hyperlink, load in the existing WebView
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //Log.i("OBDS-WebView", "onPageStarted... ");
            progressBar.setVisibility(View.VISIBLE);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //Log.i("OBDS-WebView", "onPageFinished... ");
            progressBar.setVisibility(View.GONE);
            super.onPageFinished(view, url);
        }
    }

    public class GeoWebChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin,
                                                       final GeolocationPermissions.Callback callback) {
            // Always grant permission since the app itself requires location
            // permission and the user has therefore already granted it
            callback.invoke(origin, true, false);

            //            final boolean remember = false;
            //            AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
            //            builder.setTitle("Locations");
            //            builder.setMessage("Would like to use your Current Location ")
            //                    .setCancelable(true).setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            //                public void onClick(DialogInterface dialog, int id) {
            //                    // origin, allow, remember
            //                    callback.invoke(origin, true, remember);
            //                }
            //            }).setNegativeButton("Don't Allow", new DialogInterface.OnClickListener() {
            //                public void onClick(DialogInterface dialog, int id) {
            //                    // origin, allow, remember
            //                    callback.invoke(origin, false, remember);
            //                }
            //            });
            //            AlertDialog alert = builder.create();
            //            alert.show();
        }
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null != cm) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return (info != null && info.isConnected());
        }
        return false;
    }

    public String readAppSetting(String settingName) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("WebAppViewer", getApplicationContext().MODE_PRIVATE); // 0 - for private mode

        return pref.getString(settingName, "");
    }

    public String saveAppSetting(String settingName, String settingValue) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("WebAppViewer", getApplicationContext().MODE_PRIVATE); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();

        editor.putString(settingName, settingValue);
        editor.commit();
        /*editor.remove("key"); // will delete key email
        editor.commit(); // commit changes
        editor.clear();
        editor.commit(); // commit changes*/
        return pref.getString(settingName, "");
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.about:
                Toast.makeText(getApplicationContext(),"WebApp Viewer (c) tohizma 2017. All Right Reserved.",Toast.LENGTH_LONG).show();
                return true;
            case R.id.seturl:
                final EditText txtUrl = new EditText(this);

                // Set the default text to a link of the Queen
                txtUrl.setHint("https://www.google.com");

                new AlertDialog.Builder(this)
                        .setTitle("URL SETTING")
                        //.setMessage("URL of Content")
                        .setView(txtUrl)
                        .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String url = txtUrl.getText().toString();
                                Toast.makeText(getApplicationContext(), "URL Saved. " + url, Toast.LENGTH_LONG).show();
                                saveAppSetting("URLSetting", url);
                                displayWebApp(url);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();

                return true;
            default:
                return super.onContextItemSelected(item);
        }

    }

    @Override
    public void onBackPressed() {
        // Pop the browser back stack or exit the activity
        if (webAppViewer.canGoBack()) {
            webAppViewer.goBack();
        }
        else {
            super.onBackPressed();
        }
    }
}

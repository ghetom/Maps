package com.ghetom.maps;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private WebView mapsWebView;
    private Button navTypeButton;
    private String currentUrl;
    private ArrayList<NavType> navTypes;
    private NavType currentNavType;
    private NavigationManager navManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize
        String baseUrl = "https://www.google.com/maps";
        currentUrl = baseUrl;
        navTypes = new ArrayList<NavType>(Arrays.asList(NavType.values()));
        currentNavType = NavType.MAGIC_EARTH;
        navManager = new NavigationManager(currentNavType);

        //Set application theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        //Create WebView with matching theme
        mapsWebView = (WebView) findViewById(R.id.mapsWebView);
        setWebViewTheme();

        //Create Button to choose navigation handling
        initializeNavTypeButton();

        //Set other settings
        WebSettings mapsWebSettings = mapsWebView.getSettings();
        mapsWebSettings.setJavaScriptEnabled(true);
        mapsWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        mapsWebSettings.setGeolocationEnabled(true);

        //Set location
        setLocation();

        //Load request
        Uri requestData = getIntent().getData();
        if(requestData != null) {
            String requestUrl = requestData.toString();
            UrlType requestUrlType = getUrlType(requestUrl);
            switch (requestUrlType) {
                case WEB:
                    currentUrl = requestUrl;
                    break;
                case LOCATION:
                    currentUrl = baseUrl + "/place/" + requestUrl.substring(4);
                    break;
                default:
                    break;
            }
        }

        //Handle request
        mapsWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String requestUrl = request.getUrl().toString();
                UrlType requestUrlType = getUrlType(requestUrl);
                String action = "";
                Uri requestUri = Uri.EMPTY;

                switch (requestUrlType) {
                    case NAVIGATION:
                        String redirectUrl = navManager.redirect(requestUrl);
                        action = Intent.ACTION_VIEW;
                        requestUri = navManager.isRedirected() ? Uri.parse(redirectUrl) : Uri.parse(requestUrl);
                        startActivity(action,requestUri);
                        return true;
                    case WEB:
                        requestUri = Uri.parse(requestUrl);
                        action = Intent.ACTION_VIEW;
                        startActivity(action,requestUri);
                        return true;
                    case PHONE:
                        requestUri = Uri.parse(requestUrl);
                        action = Intent.ACTION_DIAL;
                        startActivity(action,requestUri);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error){
                Toast toast = Toast.makeText(getBaseContext(), error.getDescription(),
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
                toast.show();
            }
        });

        //Load map
        mapsWebView.loadUrl(currentUrl);
    }

    private void setLocation() {
        //Set webView location access
        mapsWebView.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (origin.contains("google.com")) {
                    callback.invoke(origin, true, false);
                }
            }
        });

        //Ask for location permission
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
        } else{
            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void initializeNavTypeButton(){
        navTypeButton = (Button) findViewById(R.id.navTypeButton);
        navTypeButton.setText(currentNavType.toString());
        navTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchNavType();
            }
        });
    }

    private void switchNavType(){
        int index = navTypes.indexOf(currentNavType);
        NavType newNavType = index < navTypes.size()-1 ? navTypes.get(index+1) : navTypes.get(0);
        currentNavType = newNavType;
        navManager.setNavType(currentNavType);
        navTypeButton.setText(currentNavType.toString());
    }

    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        // Show message when permission is denied.
        if (!isGranted) {
            Toast toast = Toast.makeText(getBaseContext(), "This app needs location permission to navigate", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
            toast.show();
        }
    });

    private void startActivity(String action, Uri data){
        Intent request = new Intent();
        request.setAction(action);
        request.setData(data);
        startActivity(request);
    }

    private void setWebViewTheme() {
        // Set WebView theme matching the system theme.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    WebSettingsCompat.setForceDark(mapsWebView.getSettings(),WebSettingsCompat.FORCE_DARK_ON);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    WebSettingsCompat.setForceDark(mapsWebView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
                    break;
            }
        }
    }

    private UrlType getUrlType(String url){
        if (url.startsWith("https://maps.app.goo.gl")){
            return UrlType.NAVIGATION;
        }
        else if(url.startsWith("https://") || url.startsWith("http://")) {
            return UrlType.WEB;
        }
        else if (url.startsWith("tel:")){
            return UrlType.PHONE;
        } else if (url.startsWith("geo:")){
            return UrlType.LOCATION;
        }
        return UrlType.NOT_VALID;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mapsWebView.canGoBack() && !mapsWebView.getUrl().equals("about:blank")) {
                        mapsWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
package com.goatappshop.builder.app551fea2e6652a;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.goatappshop.builder.app551fea2e6652a.util.CommonUtilities;

import android.webkit.WebResourceResponse;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.GeolocationPermissions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class Main extends Activity {

   /**
    * Main Webview
    */
    public static WebView webView;

    public static Boolean webviewIsLoaded = false;
    public static Boolean webviewHasFailed = false;

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public String admobId = "", admobType = "";
    public boolean wasOnAd = false;

    public static String baseUrl = "";

    // Camera
    private Uri mCapturedImageURI = null;
    private final static int FILECHOOSER_RESULTCODE = 1;

    // GCM
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;
    String regid;

    //AdMob
    private AdView adView;
    private InterstitialAd interstitial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        baseUrl = this.getApplicationContext().getString(R.string.url);

        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setUserAgentString(webView.getSettings().getUserAgentString() + " type/siberian.application");
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        final ProgressDialog pd = ProgressDialog.show(Main.this, "",
                this.getApplicationContext().getString(R.string.load_message), true);
        webView.setInitialScale(1);
        settings.setGeolocationEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(false);
        settings.setSupportMultipleWindows(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setLoadsImagesAutomatically(true);

        settings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        settings.setGeolocationDatabasePath(getFilesDir().getPath());
        settings.setAllowFileAccess(true);
        settings.setAppCacheEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        checkConnection();

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        webView.addJavascriptInterface(new JsInterface(), "android");

        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {

                Main.webviewIsLoaded = !Main.webviewHasFailed;
                if(Main.webviewIsLoaded) {
                    view.loadUrl("javascript:if(window.Application) { Application.setDeviceUid(\""+regid+"\"); }");
                    view.loadUrl("javascript:if(window.Application) { Application.addHandler('geo_protocol') }");
                    view.loadUrl("javascript:if(window.Application) { Application.addHandler('media_player'); }");
                    view.loadUrl("javascript:if(window.Application) { Application.addHandler('address_book'); }");
                    view.loadUrl("javascript:if(window.Application) { Application.addHandler('camera_picture'); }");
                }
                pd.dismiss();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView Webview, String url) {
                Log.i("url", url);

                WebView.HitTestResult hr = Webview.getHitTestResult();

                try {

                    if (hr == null) {
                        return false;
                    }

                    if(hr.getType() == WebView.HitTestResult.PHONE_TYPE) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    } else if (url.startsWith("tel:")) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    } else if (hr.getType() == WebView.HitTestResult.GEO_TYPE) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    } else if (url.startsWith("geo:")) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    } else if (hr.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {

                        if (url.contains("www.youtube.com")) {
                            Intent myWebLink = new Intent(Intent.ACTION_VIEW);
                            Uri uri = Uri.parse(url);
                            myWebLink.setDataAndType(uri, "text/html");
                            myWebLink.addCategory(Intent.CATEGORY_BROWSABLE);
                            startActivity(myWebLink);
                        } else {
                            Intent intent = new Intent(Main.this, Browser.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("url", url);
                            startActivity(intent);
                        }

                        return true;

                    } else {
                        Log.e("Webview", "Not from a click");
                    }

                } catch(Exception e) {

                }

                return false;

            }

            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {

                try {

                    final Uri uri = Uri.parse(url);

                    if (uri.getPath().startsWith("/app:")) {

                        Main.this.runOnUiThread(new Runnable() {
                            public void run() {

                                Log.e("Intercepting path succeeded:", uri.getPath());
//                            Map<String, String> params = this._parseParams(uri.getPath());
                                ArrayList paramsTmp = new ArrayList(Arrays.asList(uri.getPath().split(":")));
                                paramsTmp.remove(0);
                                Map<String, String> params = new HashMap<String, String>();

                                for(int i = 0; i < paramsTmp.size(); i++) {
                                    String value = null;
                                    try {
                                        value = paramsTmp.get(i + 1).toString();
                                    } catch(Exception e) {
                                        value = null;
                                    }
                                    params.put(paramsTmp.get(i).toString(), value);
                                    // params.put(paramsTmp.get(i).toString(), paramsTmp.get(i+1).toString());
                                    i++;
                                }

                                Log.e("Method", "Done parsing");
                                for (String methodName : params.keySet()) {
                                    Log.e("Method", methodName);
                                    Method methodToFind = null;

                                    try {
                                        Class[] cArg = new Class[1];
                                        cArg[0] = String.class;
                                        methodToFind = Main.class.getMethod(methodName, cArg);
                                        if (methodToFind != null) {
                                            Log.e("Method", "Found");

                                            methodToFind.invoke(Main.this, params.get(methodName));

                                        } else {
                                            Log.e("Method", "Not Found");
                                        }
                                    } catch (Exception e) {
                                        Log.e("Method", e.toString());
                                    }

                                }
                            }
                        });
//                        Log.e("List parameters", Map);

                    }

                } catch(Exception e) {
                    Log.e("Intercepting path Error:", "Message: " + e.getMessage());
                    e.printStackTrace();
                }

                return super.shouldInterceptRequest(view, url);
//                if (url.contains(".css")) {
//                    return getCssWebResourceResponseFromAsset();
//                } else {
//                    return super.shouldInterceptRequest(view, url);
//                }
            }

            private Map _parseParams(String path) {

                ArrayList paramsTmp = new ArrayList(Arrays.asList(path.split(":")));
                paramsTmp.remove(0);
                Map<String, String> params = new HashMap<String, String>();

                for(int i = 0; i < paramsTmp.size(); i++) {
                    params.put(paramsTmp.get(i).toString(), paramsTmp.get(i+1).toString());
                    i++;
                }

                return params;
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//                view.setVisibility(WebView.GONE);
                Toast.makeText(getApplicationContext(), R.string.no_internet_connection, Toast.LENGTH_LONG).show();
                Main.webviewIsLoaded = false;
                Main.webviewHasFailed = true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        Log.i("Loading URL", baseUrl);
        webView.loadUrl(baseUrl);

        // Handling the GCM Registration
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(CommonUtilities.TAG, "No valid Google Play Services APK found.");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();

        if(!admobId.equals("") && admobType.equals("interstitial") && !wasOnAd) {
            loadInterstitialAd();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        wasOnAd = !wasOnAd;
    }

    public void setIsOnline(String isOnline) {
        if(isOnline == "0") {
            Log.e("Method", "is now offline");
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        } else {
            Log.e("Method", "is now online");
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        }
    }

    public void openYoutubePlayer(String videoId) {

        Log.i("HTML5MediaPlayer - Video ID", videoId);
        Log.i("HTML5MediaPlayer - Video Type", "Youtube");
        Intent intent = new Intent(Main.this, HTML5MediaPlayer.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("videoId", videoId);
        intent.putExtra("videoType", "youtube");
        startActivity(intent);

    }

    public void openVimeoPlayer(String videoId) {
        Intent myWebLink = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("http://player.vimeo.com/video/"+videoId);
        myWebLink.setDataAndType(uri, "text/html");
        myWebLink.addCategory(Intent.CATEGORY_BROWSABLE);
        startActivity(myWebLink);
    }

    public void createAdMobView(String p_admobId, String p_admobType) {

        admobId = p_admobId;
        admobType = p_admobType;

        Log.i("createAdMobView - AD TYPE", admobType);
        if(admobType.equals("banner")) {
            loadBannerAd();
        } else {
            loadInterstitialAd();
        }

    }

    public void loadBannerAd() {
        adView = new AdView(this);
        adView.setAdUnitId(admobId);
        adView.setAdSize(AdSize.SMART_BANNER);

        final LinearLayout adViewLayout = (LinearLayout) findViewById(R.id.adView_layout);
        adViewLayout.setVisibility(View.VISIBLE);

        adView.setAdListener(new AdListener() {

            @Override
            public void onAdFailedToLoad(int errorCode) {
                adViewLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAdLoaded() {
                webView.loadUrl("javascript:angular.element(document.body).addClass('has-banner-" + adView.getHeight() + "')");
            }
        });

        adViewLayout.addView(adView);

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("D23570B0FE04E1EB577C28898E882A56")
                .addTestDevice("C900D6882871F1114E53F118F73D99BB")
                .build();
        adView.loadAd(adRequest);
    }

    public void loadInterstitialAd() {
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(admobId);

        // Set the AdListener.
        interstitial.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                interstitial.show();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.i("AD ERR", "Couldn't load interstitial ad");
            }

            public void onAdClosed() {
                wasOnAd = true;
            }
        });

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("D23570B0FE04E1EB577C28898E882A56")
                .addTestDevice("C900D6882871F1114E53F118F73D99BB")
                .build();
        interstitial.loadAd(adRequest);
    }

    public void appIsLoaded() {

    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) {
            webView.goBack();
        } else {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which == DialogInterface.BUTTON_POSITIVE) {
                        finish();
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(webView.getContext());
            builder.setMessage(R.string.quit_message).setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.no, dialogClickListener).show();
            return;

        }
    }

    private void checkConnection() {

        boolean isConnected = false;
        ConnectivityManager check = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (check != null) {

            NetworkInfo[] info = check.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        isConnected = true;
                    }
                }

                if(!isConnected) {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                }
            }

        }

    }

    /** Push Notification **/

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(CommonUtilities.TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        Log.i("codeVersion",this.getApplicationContext().getPackageName());
        int appVersion = getAppVersion(this.getApplicationContext());
        Log.i(CommonUtilities.TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(CommonUtilities.TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        Log.i("codeVersion", this.getApplicationContext().getPackageName());
        int currentVersion = getAppVersion(this.getApplicationContext());
        if (registeredVersion != currentVersion) {
            Log.i(CommonUtilities.TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(CommonUtilities.SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.e(CommonUtilities.TAG, msg);
            }
        }.execute(null, null, null);
    }

    // Send an upstream message.
    public void onClick(final View view) {

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    Bundle data = new Bundle();
                    data.putString("my_message", "Hello World");
                    data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
                    String id = Integer.toString(msgId.incrementAndGet());
                    gcm.send(CommonUtilities.SENDER_ID + "@gcm.googleapis.com", id, data);
                    msg = "Sent message";
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.e(CommonUtilities.TAG, msg);
            }
        }.execute(null, null, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(Main.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {

        String url = CommonUtilities.SERVEUR_URL+CommonUtilities.REGISTER_DEVICE_URL;
        String params = "app_id=" + CommonUtilities.APP_ID + "&app_name=" + this.getApplicationContext().getString(R.string.app_name) + "&registration_id=" + regid;

        try {
            post(url, params);
        } catch (IOException e) {
            Log.e(CommonUtilities.TAG, e.toString());
        }

    }

    static void post(String endpoint, String params) throws IOException {

        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }

        byte[] postData = params.getBytes( Charset.forName("UTF-8"));
        int postDataLength = postData.length;
        String request = "http://example.com/index.php";
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString( postDataLength));
        conn.setUseCaches(false);

        try {

            DataOutputStream wr = new DataOutputStream( conn.getOutputStream());
            wr.write( postData );
            wr.close();

            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }

            conn.disconnect();

        } catch (IOException e) {
            Log.e(CommonUtilities.TAG, "");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if(requestCode == FILECHOOSER_RESULTCODE) {

            Uri result = null;

            try{
                if (resultCode != RESULT_OK) {

                    result = null;

                } else {

                    // retrieve from the private variable if the intent is null
                    result = intent == null ? mCapturedImageURI : intent.getData();

                    Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), result);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.JPEG, 35, baos);
                    bm.recycle();
                    bm = null;
                    String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                    encodedImage = encodedImage.replace("\n", "");

                    webView.loadUrl("javascript:if(window.Application) { Application.fireCallback('success', 'openCamera', '"+encodedImage+"'); }");

                    return;
                }
            }
            catch(Exception e) {
                Log.e("Image Capture Error", "Message: " + e.getMessage());
                e.printStackTrace();
            }

            webView.loadUrl("javascript:if(window.Application) { Application.fireCallback('error', 'openCamera'); }");

        }
    }

    public class JsInterface {

        @JavascriptInterface
        public void setIsOnline(String value) {
            final String isOnline = value;
            Main.this.runOnUiThread(new Runnable() {
                public void run() {
                    Main.this.setIsOnline(isOnline);
                }
            });
        }

        @JavascriptInterface
        public void openYoutubePlayer(String value) {
            final String videoId = value;
            Main.this.runOnUiThread(new Runnable() {
                public void run() {
                    Main.this.openYoutubePlayer(videoId);
                }
            });
        }

        @JavascriptInterface
        public void openVimeoPlayer(String value) {
            final String videoId = value;
            Main.this.runOnUiThread(new Runnable() {
                public void run() {
                    Main.this.openVimeoPlayer(videoId);
                }
            });
        }

        @JavascriptInterface
        public void openCamera() {

            try{

                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File externalDataDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM);
                File cameraDataDir = new File(externalDataDir.getAbsolutePath() +
                        File.separator + "browser-photos");
                cameraDataDir.mkdirs();
                String mCameraFilePath = cameraDataDir.getAbsolutePath() + File.separator +
                        System.currentTimeMillis() + ".jpg";
                mCapturedImageURI = Uri.fromFile(new File(mCameraFilePath));

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[] { cameraIntent });

                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            }
            catch(Exception e){
                webView.loadUrl("javascript:if(window.Application) { Application.fireCallback('error', 'openCamera'); }");
            }
        }

        @JavascriptInterface
        public void addToContact(String value) {

            final String json = value;

            Main.this.runOnUiThread(new Runnable() {
                public void run() {

                    try {

                        JSONObject Contact = new JSONObject(json);

                        // Creates a new Intent to insert a contact
                        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                        // Sets the MIME type to match the Contacts Provider
                        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

                        String name = "";
                        if(Contact.has("firstname")) {
                            name = Contact.getString("firstname");
                        }
                        if(Contact.has("lastname")) {
                            if(name.length() > 0) name += " ";
                            name += Contact.getString("lastname");
                        }
                        if(name.length() > 0) {
                            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
                        }

                        if(Contact.has("phone")) {
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, Contact.getString("phone"))
                                .putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
                            ;
                        }
                        if(Contact.has("image_url")) {

//                            intent.putExtra(ContactsContract.Contacts.PHOTO_URI, Contact.getString("image_url")); // Doesn't work

//                            Bitmap bit = MediaStore.Images.Media.getBitmap(Main.this.getContentResolver(), uri);
//                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                            bit.compress(Bitmap.CompressFormat.PNG, 100, stream);
//
//                            ArrayList<ContentValues> data = new ArrayList<ContentValues>();
//
//                            ContentValues row = new ContentValues();
//                            row.put(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
//                            row.put(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray());
//                            data.add(row);
//
//                            intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);


                        }

                        if(Contact.has("street") &&
                            Contact.has("postcode") &&
                            Contact.has("city")) {

                            String address = Contact.getString("street") + ", "
                                + Contact.getString("postcode") + ", "
                                + Contact.getString("city");

                            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, address)
                                .putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                            ;
                        }

                        intent.putExtra("finishActivityOnSaveCompleted", true);

                        startActivity(intent);

                    } catch (JSONException e) {
                        Log.e("JSONException", e.getMessage());
                    } catch (Exception e) {
                        Log.e("JSONException", e.getMessage());
                    }
                }
            });
        }

        @JavascriptInterface
        public void createAdMobView(String params) {

            final String json = params;

            Main.this.runOnUiThread(new Runnable() {
                public void run() {

                    try {

                        JSONObject AdMobParam = new JSONObject(json);

                        if(AdMobParam.has("admobId") && AdMobParam.has("admobType")) {

                            final String admobId = AdMobParam.getString("admobId");
                            final String admobType = AdMobParam.getString("admobType");
                            Main.this.createAdMobView(admobId, admobType);
                        }

                    } catch (JSONException e) {
                        Log.e("JSONException", e.getMessage());
                    } catch (Exception e) {
                        Log.e("JSONException", e.getMessage());
                    }

                }
            });
        }
    }

}

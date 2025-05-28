
package lus.restwo;

import static lus.restwo.BackendHttpService.sendLogsToRemoteServerAsync;
import static lus.restwo.Constants.INTERCOM_URL;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class OpenIntercomActivity extends AppCompatActivity {
    private static final String TAG = "OpenIntercomActivityLog";


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_intercom);
        WebView myWebView = findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        setContentView(myWebView);
        NotificationManager notificationManagerInOpenIntercomActivity = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManagerInOpenIntercomActivity.cancel(Constants.INTERCOM_NOTIFICATION_ID);

        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        myWebView.setWebChromeClient(new WebChromeClient() {

            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        String[] PERMISSIONS = {
                                PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE};
                        try {
                            request.grant(PERMISSIONS);
                            Log.d(TAG, "request.grant(PERMISSIONS) OK;");
                            sendLogsToRemoteServerAsync(TAG + ":request.grant(PERMISSIONS) OK;");
                        } catch (Exception e) {
                            Log.e(TAG, "request.grant(PERMISSIONS) failed, " + e);
                            try {
                                sendLogsToRemoteServerAsync(TAG + ":request.grant(PERMISSIONS) failed, " + e);
                            } catch (Exception ignored) {

                            }
                        }
                    }
                });
            }


            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                try {
                    Log.d(TAG, consoleMessage.message() + " -- From line " +
                            consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                    //BackendHttpService.sendLogsToRemoteServer(consoleMessage.message() + " -- From line " +
                    //       consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                } catch (Exception e) {
                    Log.e(TAG, "onConsoleMessage failed, " + e);
                }
                return true;
            }
        });

        Intent intent = getIntent();
        if (intent.hasExtra("passing_parameters_from_push_to_wss")) {
            String html = "<html><body><h1 style='color:grey;margin-top:150px;'>Ждите ...</h1></body></html>";
            myWebView.loadData(html, "text/html", null);
            String valuefrompush = intent.getStringExtra("passing_parameters_from_push_to_wss");
            Log.d(TAG, "valuefrompush=" + valuefrompush);
            JSONObject objresponse = null;
            try {
                assert valuefrompush != null;
                objresponse = new JSONObject(valuefrompush);
                String to_sip_number = objresponse.getString("to_sip_number").replace("'", "");
                String from_intercom = objresponse.getString("from_intercom").replace("'", "");
                Log.i(TAG, "valuefrompush=" + " to_sip_number=" + to_sip_number + ",from_intercom=" + from_intercom);
                String decodedString = new String(Base64.decode("MXEydzNl", Base64.DEFAULT), StandardCharsets.UTF_8);
                try {
                    sendLogsToRemoteServerAsync(TAG + ": myWebView.loadUrl=" +
                            INTERCOM_URL + "&to_sipaccountuser=" + to_sip_number + ":" +
                            decodedString + "&from_sipintercom=" + from_intercom + "&session_id=" +
                            to_sip_number);
                } catch (Exception e) {
                    Log.e(TAG, "ERROR=" + e.getMessage());
                }
                myWebView.setWebViewClient(new WebViewController());
                myWebView.loadUrl(INTERCOM_URL + "&to_sipaccountuser=" +
                        to_sip_number + ":" + decodedString + "&from_sipintercom=" +
                        from_intercom + "&session_id=" +
                        to_sip_number);
            } catch (JSONException e) {
                Log.i(TAG, "valuefrompush=" + e.getMessage());
                String html2 = "<html><body><h1 style='color:" + "red;margin-top:150px;'>Ошибка ...</h1></body></html>";
                myWebView.loadData(html2, "text/html", null);
                try {
                    sendLogsToRemoteServerAsync(TAG + ":myWebView.loadUrl ERROR;" + e.getMessage());
                } catch (Exception ignored) {
                }
            }
        } else {
            String html = "<html><body><h1 style='color:red;margin-top:150px;'>Нет параметров сообщения ...</h1></body></html>";
            myWebView.loadData(html, "text/html", null);
            try {
                sendLogsToRemoteServerAsync(TAG + ":myWebView.loadUrl NOT OK;");
            } catch (Exception ignored) {
            }
        }
    }


}

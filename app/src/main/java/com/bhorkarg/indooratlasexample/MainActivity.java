package com.bhorkarg.indooratlasexample;

import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.Manifest;
import android.support.v4.app.ActivityCompat;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.util.Random;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import android.provider.Settings.Secure;

public class MainActivity extends AppCompatActivity {

    IALocationManager mLocationManager;

    //final TelephonyManager mTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    //String myAndroidDeviceId = mTelephony.getDeviceId();

    private String android_id;

    HttpClient httpClient = new DefaultHttpClient();
    HttpContext httpContext = new BasicHttpContext();
    HttpPost httpPost = new HttpPost("https://locationanalyzer.herokuapp.com/api/submitLocation");

    //Listener for receiving location updates
    IALocationListener mLocationListener = new IALocationListener() {

        String latitude, longitude;

        @Override
        public void onLocationChanged(IALocation iaLocation) {
            //Location updates will be received here  
            TextView txtLoc = (TextView) findViewById(R.id.txtLocation);
            txtLoc.setText(Double.toString(iaLocation.getLatitude()) + ", " + Double.toString(iaLocation.getLongitude()));

            latitude = Double.toString(iaLocation.getLatitude());
            longitude = Double.toString(iaLocation.getLongitude());

            List<NameValuePair> info = new ArrayList<NameValuePair>();
            info.add(new BasicNameValuePair("id", android_id));
            info.add(new BasicNameValuePair("lat", latitude));
            info.add(new BasicNameValuePair("long", longitude));

            try {
                requestUrl("https://locationanalyzer.herokuapp.com/api/submitLocation", info);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }
    };

    public static void requestUrl(String url, List<NameValuePair> postParameters) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters));
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        try {
            HttpResponse response = httpClient.execute(httpPost);
            // write response to log
            Log.i("Http Post Response:", response.toString());
        } catch (ClientProtocolException e) {
            // Log exception
            e.printStackTrace();
        } catch (IOException e) {
            // Log exception
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions( this, neededPermissions, 1 );

        mLocationManager = IALocationManager.create(this); //create a IALocationManager

        if (android.os.Build.VERSION.SDK_INT > 9) {

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()

                    .permitAll().build();

            StrictMode.setThreadPolicy(policy);

        }

        Random rand = new Random();

        int  n = rand.nextInt(1000000) + 1;
        android_id = Integer.toString(n);

        Thread background = new Thread(new Runnable() {

            private final HttpClient Client = new DefaultHttpClient();
            private String URL = "https://locationanalyzer.herokuapp.com/api/locations";

            public void run() {
                try {

                    String SetServerString = "";
                    HttpGet httpget = new HttpGet(URL);
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();
                    SetServerString = Client.execute(httpget, responseHandler);
                    threadMsg(SetServerString);

                } catch (Throwable t) {
                    // just end the background thread
                    Log.i("Animation", "Thread  exception " + t);
                }
            }

            private void threadMsg(String msg) {

                if (!msg.equals(null) && !msg.equals("")) {
                    Message msgObj = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("message", msg);
                    msgObj.setData(b);
                    handler.sendMessage(msgObj);
                }
            }

            // Define the Handler that receives messages from the thread and update the progress
            private final Handler handler = new Handler() {

                public void handleMessage(Message msg) {

                    String aResponse = msg.getData().getString("message");

                    TextView txtInfo = (TextView) findViewById(R.id.ServerInfo);
                    if ((null != aResponse)) {

                        // ALERT MESSAGE
                        txtInfo.setText(aResponse);
                    }
                    else
                    {

                        // ALERT MESSAGE
                        txtInfo.setText("Nothing");
                    }

                }
            };

        });
        // Start Thread
        background.start();  //After call start method thread called run Method
    }

    protected void onResume() {
        super.onResume();
        //Start requesting location updates. Specify the listener.
        mLocationManager.requestLocationUpdates(IALocationRequest.create(), mLocationListener);

    }

    protected void onPause() {
        //Stop receiving location updates when app paused.
        mLocationManager.removeLocationUpdates(mLocationListener);
        super.onPause();
    }

    protected void onDestroy() {
        mLocationManager.destroy();
        super.onDestroy();
    }
}

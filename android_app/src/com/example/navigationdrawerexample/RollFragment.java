package com.example.navigationdrawerexample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class RollFragment extends Fragment implements SensorEventListener{
	
	private View rootView;
	private String imei;
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	private SensorManager senSensorManager;
	private Sensor senAccelerometer;
	private long lastUpdate = 0;
	private long lastRoll = 0;
	private float last_x, last_y, last_z;
	private static final int SHAKE_THRESHOLD = 600;
	
	public void roll()
	{
		long curTime = System.currentTimeMillis();
		 
        if ((curTime - lastRoll) > 1000) {
			Toast toast = Toast.makeText(rootView.getContext(), "Rolling", Toast.LENGTH_SHORT);
			toast.show();
            ((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(rootView.getWindowToken(), 0);
			new HttpAsyncTask().execute("http://party-dice.appspot.com/roll");
            lastRoll = curTime;
        }

	}
	
	private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
 
        inputStream.close();
        return result;
 
    } 
	
	public String POST(String url){
        InputStream inputStream = null;
        String result = "";
        try {
 
            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();
 
            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);
 
            //String json = "";
 
            // 3. build jsonObject
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("n", ((EditText)rootView.findViewById(R.id.numDice)).getText().toString()));
            pairs.add(new BasicNameValuePair("t", ((EditText)rootView.findViewById(R.id.diceKind)).getText().toString()));
            pairs.add(new BasicNameValuePair("m", ((EditText)rootView.findViewById(R.id.mod)).getText().toString()));
            pairs.add(new BasicNameValuePair("i", settings.getString("imei", "")));
            httpPost.setEntity(new UrlEncodedFormEntity(pairs));
            
            Log.d("Data Req", url);
            
            HttpResponse httpResponse = httpclient.execute(httpPost);
 
            inputStream = httpResponse.getEntity().getContent();
 
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";
 
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
 
        // 11. return result
        return result;
    }
	
	private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        
        String n;
        String t;
        String m;
        
        @Override
        protected String doInBackground(String... urls) {
        	

            n = ((EditText)rootView.findViewById(R.id.numDice)).getText().toString();
            t = ((EditText)rootView.findViewById(R.id.diceKind)).getText().toString();
            m = ((EditText)rootView.findViewById(R.id.mod)).getText().toString();
        	
            return POST(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(rootView.getContext(), "Data Sent", Toast.LENGTH_LONG).show();
            Log.d("Data Rec", result);
            ((TextView) rootView.findViewById(R.id.rollResult)).setText(result);
            ((Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
            
            final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

    	    final Map<String, String> data = new HashMap<String, String>();
    	    data.put("title", "Roll Result");
    	    data.put("body", n + "d" + t + "+" + m + "\n" + result);
    	    final JSONObject jsonData = new JSONObject(data);
    	    final String notificationData = new JSONArray().put(jsonData).toString();

    	    i.putExtra("messageType", "PEBBLE_ALERT");
    	    i.putExtra("sender", "party-dice");
    	    i.putExtra("notificationData", notificationData);

    	    Log.d("About to send a modal alert to Pebble: ", notificationData);
    	    rootView.getContext().sendBroadcast(i);
       }
    }

	public RollFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.fragment_roll, container, false);
		settings =  getActivity().getSharedPreferences(getString(R.string.app_name), 0);
		editor = settings.edit();
        TelephonyManager  tm = ( TelephonyManager )getActivity().getSystemService( Context.TELEPHONY_SERVICE );
		imei = tm.getDeviceId();
		
		senSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
	    senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    if(settings.getBoolean("shake", false))
	    {
	    	senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
	    }
	    else
	    {
	    	senSensorManager.unregisterListener(this);
	    }
		
		((Button) rootView.findViewById(R.id.roller)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	 roll();
	         }
		});
		
		return rootView;
	}
	
	@Override
	public void onPause() {
	    super.onPause();
	    senSensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Sensor mySensor = event.sensor;
		 
	    if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	        float x = event.values[0];
	        float y = event.values[1];
	        float z = event.values[2];
	 
	        long curTime = System.currentTimeMillis();
	 
	        if ((curTime - lastUpdate) > 100) {
	            long diffTime = (curTime - lastUpdate);
	            lastUpdate = curTime;
	 
	            float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;
	 
	            if (speed > SHAKE_THRESHOLD) {
	            	roll();
	            }
	 
	            last_x = x;
	            last_y = y;
	            last_z = z;
	        }
	    }
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

}

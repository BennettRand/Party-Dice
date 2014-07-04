package com.example.navigationdrawerexample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class MeFragment extends Fragment {
	
	private View rootView;
	private String imei;
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	public void saveSettings()
	{
		editor.putString("name", ((EditText)rootView.findViewById(R.id.uName)).getText().toString());
		editor.putString("imei", imei);
		editor.putBoolean("shake", ((Switch)rootView.findViewById(R.id.rollOnShake)).isChecked());
		editor.commit();
		Toast toast = Toast.makeText(rootView.getContext(), "Saved", Toast.LENGTH_SHORT);
		toast.show();
        ((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(rootView.getWindowToken(), 0);
		new HttpAsyncTask().execute("http://party-dice.appspot.com/user");

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
            pairs.add(new BasicNameValuePair("n", settings.getString("name", "")));
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
        @Override
        protected String doInBackground(String... urls) {
 
            return POST(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(rootView.getContext(), "Data Recieved", Toast.LENGTH_LONG).show();
            Log.d("Data Rec", result);
       }
    }
	
	public MeFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.fragment_me, container, false);
		settings =  getActivity().getSharedPreferences(getString(R.string.app_name), 0);
		editor = settings.edit();
        TelephonyManager  tm = ( TelephonyManager )getActivity().getSystemService( Context.TELEPHONY_SERVICE );
		imei = tm.getDeviceId();
		

		((TextView) rootView.findViewById(R.id.imei)).setText(imei);
		((EditText) rootView.findViewById(R.id.uName)).setText(settings.getString("name", ""));
		((Switch) rootView.findViewById(R.id.rollOnShake)).setChecked(settings.getBoolean("shake", false));
		((Button) rootView.findViewById(R.id.meSave)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	 saveSettings();
	         }
		});

		return rootView;
	}

}

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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
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
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class GroupFragment extends Fragment {
	private View rootView;
	private String imei;
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	public void saveSettings()
	{
		String gName = ((EditText)rootView.findViewById(R.id.gName)).getText().toString();
		editor.putString("group", gName);
		editor.putString("imei", imei);
		editor.commit();
		Toast toast = Toast.makeText(rootView.getContext(), "Saved", Toast.LENGTH_SHORT);
		toast.show();
        ((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(rootView.getWindowToken(), 0);
		new HttpAsyncTask().execute();
	
	}
	
	public void refresh()
	{
		Toast.makeText(rootView.getContext(), "Refreshing", Toast.LENGTH_LONG).show();
		new HttpAsyncTask().execute();
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
	
	public String POST(){
	    InputStream inputStream = null;
	    String result = "";
	    try {
	
	        // 1. create HttpClient
	        HttpClient httpclient = new DefaultHttpClient();
	
	        // 2. make POST request to the given URL
	        HttpPost httpPost = new HttpPost("http://party-dice.appspot.com/user");
	
	        //String json = "";
	
	        // 3. build jsonObject
	        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
	        pairs.add(new BasicNameValuePair("n", settings.getString("name", "")));
	        pairs.add(new BasicNameValuePair("g", settings.getString("group", "")));
	        pairs.add(new BasicNameValuePair("i", settings.getString("imei", "")));
	        httpPost.setEntity(new UrlEncodedFormEntity(pairs));
	        
	        Log.d("Data Req", "http://party-dice.appspot.com/user");
	        
	        HttpResponse httpResponse = httpclient.execute(httpPost);
	
	        inputStream = httpResponse.getEntity().getContent();
	
	        if(inputStream != null)
	            result = "["+convertInputStreamToString(inputStream)+",";
	        else
	            result = "Did not work!";
	        
	        httpPost = new HttpPost("http://party-dice.appspot.com/group");
	    	
	        //String json = "";
	
	        // 3. build jsonObject
	        pairs = new ArrayList<NameValuePair>();
	        pairs.add(new BasicNameValuePair("n", settings.getString("group", "")));
	        httpPost.setEntity(new UrlEncodedFormEntity(pairs));
	        
	        Log.d("Data Req", "http://party-dice.appspot.com/group");
	        
	        httpResponse = httpclient.execute(httpPost);
	
	        inputStream = httpResponse.getEntity().getContent();
	
	        if(inputStream != null)
	            result += convertInputStreamToString(inputStream)+"]";
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
	
	        return POST();
	    }
	    // onPostExecute displays the results of the AsyncTask.
	    @Override
	    protected void onPostExecute(String result) {
	        Toast.makeText(rootView.getContext(), "Data Recieved", Toast.LENGTH_LONG).show();
	        Log.d("Data Rec", result);
	        String NAME = "Foo";
	        try {
				JSONArray jsonArr = new JSONArray(result);
				JSONArray group = jsonArr.getJSONArray(1);
				Log.d("Json", group.toString(4));
				List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
		        List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
		        for (int i = 0; i < group.length(); i++) {
		            Map<String, String> curGroupMap = new HashMap<String, String>();
		            groupData.add(curGroupMap);
		            curGroupMap.put(NAME, group.getJSONArray(i).getString(0));
		            List<Map<String, String>> children = new ArrayList<Map<String, String>>();
		            for (int j = group.getJSONArray(i).getJSONArray(1).length()-1; j >= 0; j--) {
		                Map<String, String> curChildMap = new HashMap<String, String>();
		                children.add(curChildMap);
		                
		                JSONArray current = new JSONArray(group.getJSONArray(i).getJSONArray(1).getString(j));
		                
		                if(current.length() > 2)
		                {
		                	curChildMap.put(NAME, current.getString(1)+" = "+current.getString(0)+"\n"+current.getString(2)+" UTC");
		                }
		                else
		                {
		                	curChildMap.put(NAME, current.getString(1)+" = "+current.getString(0));
		                }
		            }
		            childData.add(children);
		        }
		        // Set up our adapter
		        SimpleExpandableListAdapter mAdapter = new SimpleExpandableListAdapter(rootView.getContext(), groupData,
		                android.R.layout.simple_expandable_list_item_1,
		                new String[] { NAME }, new int[] { android.R.id.text1 },
		                childData, android.R.layout.simple_expandable_list_item_2,
		                new String[] { NAME }, new int[] { android.R.id.text1 });
		        ((ExpandableListView) rootView.findViewById(R.id.Members)).setAdapter(mAdapter);
			} catch (JSONException e) {
				Log.d("InputStream", e.getLocalizedMessage());
			}
	   }
}

	public GroupFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.fragment_group, container, false);
		settings =  getActivity().getSharedPreferences(getString(R.string.app_name), 0);
		editor = settings.edit();
        TelephonyManager  tm = ( TelephonyManager )getActivity().getSystemService( Context.TELEPHONY_SERVICE );
		imei = tm.getDeviceId();
		

		((EditText) rootView.findViewById(R.id.gName)).setText(settings.getString("group", ""));
		((Button) rootView.findViewById(R.id.gJoin)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	 saveSettings();
	         }
		});
		((Button) rootView.findViewById(R.id.refresher)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	 refresh();
	         }
		});
		refresh();
		return rootView;
	}

}

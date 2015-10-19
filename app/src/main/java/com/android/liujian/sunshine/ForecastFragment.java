package com.android.liujian.sunshine;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by liujian on 15/10/19.
 * ForecastFragment
 */
public class ForecastFragment extends Fragment{
    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private ListView mForecastListView;
    private ArrayAdapter<String> mWeatherListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mForecastListView = (ListView)view.findViewById(R.id.forecast_weather_list);

        String[] forecastWeatherArray = new String[] {
                "Sunshine - Big sun shine - 28/20",
                "Sunshine - Big sun shine - 28/20",
                "Sunshine - Big sun shine - 28/20",
                "Sunshine - Big sun shine - 28/20",
                "Sunshine - Big sun shine - 28/20",
                "Sunshine - Big sun shine - 28/20",
                "Sunshine - Big sun shine - 28/20",
        };

        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(forecastWeatherArray));
        mWeatherListAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.list_item_forcast,
                R.id.list_item_forecast_textview,
                arrayList);
        mForecastListView.setAdapter(mWeatherListAdapter);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_forecast_refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.forecast_refresh){
            FetchWeatherTask task = new FetchWeatherTask();
            task.execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    private class FetchWeatherTask extends AsyncTask<Void, Void, String>{

        public String[] weatherJsonParser(String json){

            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_MAIN = "main";
            final String OWM_DESCRIPTION = "description";
            final String OWM_DATE = "dt";
            final String OWM_TEMP = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";


            try {
                JSONObject jsonObject = new JSONObject(json);
                JSONArray jsonArray = jsonObject.getJSONArray(OWM_LIST);
                String[] weatherArray = new String[jsonArray.length()];

                for(int i = 0; i < jsonArray.length(); i++){
                    JSONObject listObject = jsonArray.getJSONObject(i);
                    long date = listObject.getLong(OWM_DATE);
                    JSONObject object = listObject.getJSONObject(OWM_TEMP);
                    double temp_min = object.getDouble(OWM_MIN);
                    double temp_max = object.getDouble(OWM_MAX);
                    String tempString = String.valueOf(Math.round(temp_max)) + "/" + String.valueOf(Math.round(temp_min));

                    object = listObject.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    String weather = object.getString(OWM_MAIN);
                    String weatherDescription = object.getString(OWM_DESCRIPTION);

                    weatherArray[i] = String.valueOf(date) + "-" + weather + "-" + weatherDescription + "-" + tempString;
                }

                return weatherArray;

            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection forecastConnection = null;
            BufferedReader reader = null;
            String forecastWeatherString = null;

            String urlString = "http://api.openweathermap.org/data/2.5/forecast/daily?id=524901&units=metric&cnt=7&appid=8f9aa35c830c4bb0ca1b56b180c54bea";

            try {
                URL url = new URL(urlString);
                forecastConnection = (HttpURLConnection)url.openConnection();
                if(forecastConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                    Log.d(LOG_TAG, "connection success..");
                }
//                forecastConnection.setRequestMethod("GET");

                reader = new BufferedReader(new InputStreamReader(forecastConnection.getInputStream()));

                StringBuilder buffer = new StringBuilder();
                String s;

                while ( (s = reader.readLine()) != null){
                    buffer.append(s);
                }

                forecastWeatherString = buffer.toString();


            } catch (IOException e){
                e.printStackTrace();
                Log.e(LOG_TAG, e.getMessage());
            }finally {
                if(reader != null){
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(forecastConnection != null)
                    forecastConnection.disconnect();
            }

            return forecastWeatherString;
        }


        @Override
        protected void onPostExecute(String s) {
            String[] weathers = weatherJsonParser(s);
            if(weathers != null){
                mWeatherListAdapter.clear();
                mWeatherListAdapter.addAll(new ArrayList<>(Arrays.asList(weathers)));
            }
        }
    }


}

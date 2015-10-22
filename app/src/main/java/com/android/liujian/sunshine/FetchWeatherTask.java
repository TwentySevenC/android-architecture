package com.android.liujian.sunshine;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.android.liujian.sunshine.data.WeatherContract;
import com.android.liujian.sunshine.utils.AppConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

/**
 * Created by liujian on 15/10/22.
 *
 */
public class FetchWeatherTask  extends AsyncTask<String, Void, Void> {
    private static final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private Context mContext;


    public FetchWeatherTask(Context context) {
        mContext = context;
    }

/*    private String getReadableDataString(Long time){
        SimpleDateFormat shortedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortedDateFormat.format(time);
    }


    */

    /**
     * Insert a location into database
     *
     * @param locationSetting locationSetting
     * @param cityName        location's name
     * @param lat             location's latitude
     * @param lon             location's longitude
     * @return location id
     */
    private long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId = 0;

        Cursor cursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if(cursor == null ){
            return 0;
        }

        if (cursor.moveToFirst()) {
            /** The city is already in the database */
            int columnIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = cursor.getLong(columnIndex);
        } else {
            ContentValues values = new ContentValues();

            values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri uri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, values);
            locationId = ContentUris.parseId(uri);
        }

        if(cursor != null ) cursor.close();
        return locationId;
    }


    /**
     * Parse the weather json string
     *
     * @param json            weather json information
     * @param locationSetting locationSetting
     */
    private void getWeatherDataFromJson(String json, String locationSetting) {


        final String OWM_WEATHER = "weather";
        final String OWM_MAIN = "main";
        final String OWM_WEATHER_ID = "id";

        /** weather information */
        final String OWM_LIST = "list";

        /** weather temperature*/
        final String OWM_TEMP = "temp";
        final String OWM_TEMP_MAX = "max";
        final String OWM_TEMP_MIN = "min";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WIND_SPEED = "speed";
        final String OWM_DEGREE = "deg";

        /** location information  */
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        /** location coordinate*/
        final String OWM_COORD_LONG = "lon";
        final String OWM_COORD_LAT = "lat";


        try {
            JSONObject jsonObject = new JSONObject(json);

            /** Fetch location information */
            JSONObject cityObject = jsonObject.getJSONObject(OWM_CITY);
            String cityName = cityObject.getString(OWM_CITY_NAME);

            JSONObject coordObject = cityObject.getJSONObject(OWM_COORD);
            double coord_lat = coordObject.getDouble(OWM_COORD_LAT);
            double coord_long = coordObject.getDouble(OWM_COORD_LONG);

            long locationId = addLocation(locationSetting, cityName, coord_lat, coord_long);

            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time();

            /** Fetch weather information */
            JSONArray jsonArray = jsonObject.getJSONArray(OWM_LIST);

            Vector<ContentValues> cVVector = new Vector<>(jsonArray.length());


            for (int i = 0; i < jsonArray.length(); i++) {
                long dateTime;
                double pressure, windSpeed, windDirection, tempHigh, tempLow;
                int humidity;

                String description;
                int weatherId;

                dateTime = dayTime.setJulianDay(julianStartDay + i);

                /** containing all weather information */
                JSONObject listObject = jsonArray.getJSONObject(i);
                pressure = listObject.getDouble(OWM_PRESSURE);
                humidity = listObject.getInt(OWM_HUMIDITY);
                windSpeed = listObject.getDouble(OWM_WIND_SPEED);
                windDirection = listObject.getDouble(OWM_DEGREE);


                /** Get the json object representing temperature*/
                JSONObject tempObject = listObject.getJSONObject(OWM_TEMP);
                tempHigh = tempObject.getDouble(OWM_TEMP_MAX);
                tempLow = tempObject.getDouble(OWM_TEMP_MIN);


                JSONObject weatherObject = listObject.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_MAIN);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                ContentValues values = new ContentValues();

                values.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                values.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                values.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                values.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                values.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, tempHigh);
                values.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, tempLow);
                values.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                values.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                values.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                values.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);

                cVVector.add(values);

            }

            if (cVVector.size() > 0) {
                ContentValues[] weatherValues = new ContentValues[cVVector.size()];
                cVVector.toArray(weatherValues);
                mContext.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, weatherValues);

                Log.d(LOG_TAG, "Fetch weather task completed..");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected Void doInBackground(String... params) {

        if (params.length == 0) {
            return null;
        }

        String locationSetting = params[0];

        String units = "metric";
        int days = 10;


        HttpURLConnection forecastConnection = null;
        BufferedReader reader = null;
        String forecastWeatherString ;


        final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
        final String QUERY_PARAM = "q";
        final String UNIT_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        final String APPID_PARAM = "appid";

        try {

            Uri uri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationSetting)
                    .appendQueryParameter(UNIT_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, String.valueOf(days))
                    .appendQueryParameter(APPID_PARAM, AppConfig.OPEN_WEATHER_APPID)
                    .build();

            Log.d(LOG_TAG, uri.toString());

            URL url = new URL(uri.toString());

            forecastConnection = (HttpURLConnection) url.openConnection();
            forecastConnection.setRequestMethod("GET");
            forecastConnection.connect();

            InputStream inputStream = forecastConnection.getInputStream();

            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }

            forecastWeatherString = buffer.toString();

            getWeatherDataFromJson(forecastWeatherString, locationSetting);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (forecastConnection != null)
                forecastConnection.disconnect();
        }

        return null;
    }

}


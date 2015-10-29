package com.android.liujian.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;

import com.android.liujian.sunshine.MainActivity;
import com.android.liujian.sunshine.R;
import com.android.liujian.sunshine.data.WeatherContract;
import com.android.liujian.sunshine.utils.AppConfig;
import com.android.liujian.sunshine.utils.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

/**
 * Created by liujian on 15/10/27.
 * Handle the transfer of data between a server and an app, using the Android
 * sync adapter framework
 */
public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter{
    private static final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    private static final int SYNC_INTERVAL = 60 * 180;
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    /** ms of one day*/
    private static final int DAY_MILLIS = 1000 * 60 * 60 * 24;
    private static final int NOTIFICATION_FLAG = 100;

    /** Notification weather information projection */
    private static final String[] NOTIFY_WEATHER_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    /** This index must match the projection */
    private static final int COL_WEATHER_CONDITION_ID = 0;
    private static final int COL_WEATHER_MAX_TEMP = 1;
    private static final int COL_WEATHER_MIN_TEMP = 2;
    private static final int COL_WEATHER_DESC = 3;

    //Global variables
    //Define a variable to contain a content resolver instance
    private ContentResolver mContentResolver;

    private Context mContext;


    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
        mContext = context;
    }

    public SunshineSyncAdapter(Context context,
                               boolean autoInitialize,
                               boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

        mContentResolver = context.getContentResolver();
        mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {

        HttpURLConnection forecastConnection = null;
        BufferedReader reader = null;
        String forecastWeatherString ;


        try {

            URL url = createUrl();
            forecastConnection = (HttpURLConnection) url.openConnection();
            forecastConnection.setRequestMethod("GET");
            forecastConnection.connect();

            InputStream inputStream = forecastConnection.getInputStream();

            if (inputStream == null) return;
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) return ;

            forecastWeatherString = buffer.toString();

            getWeatherDataFromJson(forecastWeatherString);

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
    }


    /**
     * Create the url to get the weather forecast
     */
    public URL createUrl(){
        String locationSetting = Utility.getPreferenceLocation(mContext);
        boolean isGpsEnable = Utility.getPreferenceGpsLocation(mContext);


        final String QUERY_PARAM = "q";
        final String UNIT_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        final String APPID_PARAM = "appid";

        final String COORDS_LAT_PARAM = "lat";
        final String COORDS_LON_PARAM = "lon";

        Uri uri ;

        if(isGpsEnable){

            SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String coods_lon = sPref.getString(mContext.getString(R.string.pref_last_location_lon),
                    mContext.getString(R.string.pref_last_location_lon_default));
            String coods_lat = sPref.getString(mContext.getString(R.string.pref_last_location_lat),
                    mContext.getString(R.string.pref_last_location_lat_default));

            uri = Uri.parse(AppConfig.FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(COORDS_LAT_PARAM, coods_lat)
                    .appendQueryParameter(COORDS_LON_PARAM, coods_lon)
                    .appendQueryParameter(UNIT_PARAM, AppConfig.DEFAULT_UNITS)
                    .appendQueryParameter(DAYS_PARAM, String.valueOf(AppConfig.FORECAST_DAYS))
                    .appendQueryParameter(APPID_PARAM, AppConfig.OPEN_WEATHER_APPID)
                    .build();
        }else{

            uri = Uri.parse(AppConfig.FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationSetting)
                    .appendQueryParameter(UNIT_PARAM, AppConfig.DEFAULT_UNITS)
                    .appendQueryParameter(DAYS_PARAM, String.valueOf(AppConfig.FORECAST_DAYS))
                    .appendQueryParameter(APPID_PARAM, AppConfig.OPEN_WEATHER_APPID)
                    .build();
        }

        Log.d(LOG_TAG, uri.toString());

        URL url = null;
        try {
            url = new URL(uri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }


    /**
     * Insert a location into database
     *
     * @param locationSetting locationSetting
     * @param cityName        location's name
     * @param lat             location's latitude
     * @param lon             location's longitude
     * @return location id
     */
    private long addLocation(String locationSetting, String cityName, String country, double lat, double lon) {
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
            values.put(WeatherContract.LocationEntry.COLUMN_COUNTRY_NAME, country);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri uri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, values);
            locationId = ContentUris.parseId(uri);
        }

        cursor.close();
        return locationId;
    }


    /**
     * Parse the weather json string
     *
     * @param json            weather json information
     */
    private void getWeatherDataFromJson(String json) {
        String locationSetting = Utility.getPreferenceLocation(mContext);

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
        final String OWM_COUNTRY = "country";
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
            String country = cityObject.getString(OWM_COUNTRY);

            JSONObject coordObject = cityObject.getJSONObject(OWM_COORD);
            double coord_lat = coordObject.getDouble(OWM_COORD_LAT);
            double coord_long = coordObject.getDouble(OWM_COORD_LONG);

            long locationId = addLocation(locationSetting, cityName, country, coord_lat, coord_long);

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
                mContentResolver.bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, weatherValues);

                /** Delete the history weather information */
                String deleteSelection = WeatherContract.WeatherEntry.COLUMN_DATE + " <= ? ";
                mContentResolver.delete(WeatherContract.WeatherEntry.CONTENT_URI,
                        deleteSelection, new String[]{String.valueOf(dayTime.setJulianDay(julianStartDay - 1))});

//                Log.d(LOG_TAG, "Fetch weather task completed..");
                notifyWeatherForecast();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    /**
     * Notification weather information, Once the weather information was updated..
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void notifyWeatherForecast(){
        boolean isNotifyWeather = Utility.getPreferenceNotification(mContext);
        if(isNotifyWeather){
            Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(Utility.getPreferenceLocation(getContext()), System.currentTimeMillis());
            Cursor cursor = mContentResolver.query(uri, NOTIFY_WEATHER_PROJECTION, null, null, null);

            SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String lastNotifyKey = mContext.getString(R.string.pref_notification_last_notify);
            long lastNotifyTime = sPref.getLong(lastNotifyKey, 0);

            if(cursor == null) return ;

            if(System.currentTimeMillis() - lastNotifyTime >= DAY_MILLIS){

                if(cursor.moveToFirst()){
                    int weatherId = cursor.getInt(COL_WEATHER_CONDITION_ID);
                    int largeIconId = Utility.getArtResourceForWeatherCondition(weatherId);
                    Bitmap largeIcon = BitmapFactory.decodeStream(mContext.getResources().openRawResource(largeIconId));

                    int weatherIconId = Utility.getIconResourceForWeatherCondition(weatherId);
                    Float high = cursor.getFloat(COL_WEATHER_MAX_TEMP);
                    Float low = cursor.getFloat(COL_WEATHER_MIN_TEMP);
                    String desc = cursor.getString(COL_WEATHER_DESC);

                    String forecastStr = String.format(mContext.getString(R.string.notification_forecast_format), desc, high, low);
                    TaskStackBuilder tsb = TaskStackBuilder.create(mContext);
                    tsb.addNextIntent(new Intent(mContext, MainActivity.class));
                    PendingIntent pi = tsb.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);

                    Notification notification = new NotificationCompat.Builder(getContext())
                            .setColor(mContext.getResources().getColor(R.color.sunshine_light_blue))
                            .setSmallIcon(weatherIconId)
                            .setLargeIcon(largeIcon)
                            .setContentTitle(mContext.getString(R.string.app_name))
                            .setContentText(forecastStr)
                            .setContentIntent(pi)
                            .build();

                    NotificationManager nm = (NotificationManager)getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(NOTIFICATION_FLAG, notification);

                    /** Record the last notify time */
                    sPref.edit().putLong(lastNotifyKey, System.currentTimeMillis()).apply();
                }

                cursor.close();
            }

        }

    }


    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

}

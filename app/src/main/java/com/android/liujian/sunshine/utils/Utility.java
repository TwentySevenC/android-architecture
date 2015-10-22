package com.android.liujian.sunshine.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import com.android.liujian.sunshine.R;
import com.android.liujian.sunshine.data.WeatherContract;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by liujian on 15/10/22.
 *
 */
public class Utility {

    public static String getPreferenceLocation(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        return sharedPreferences.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }


    public static boolean isMetric(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        return pref.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }


    public static String formatTemperature(Context context, double data){
        double temp;

        if(isMetric(context)){
            temp = data;
        }else{
            temp = 1.8 * data + 32;
        }

        return String.format("%.0f", temp);
    }


    public static String formatDate(long dateInMillis){
        Date date = new Date(dateInMillis);
        return DateFormat.getDateInstance().format(date);
    }


    public static String formatHighLows(Context context, double high, double low){

        return Utility.formatTemperature(context, high) + "/" +
                Utility.formatTemperature(context, low);
    }


    public static String convertCursorRowToUxFormat(Context context, Cursor cursor){
        int index_data = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
        int index_max_temp = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP);
        int index_min_temp = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP);
        int index_desc = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC);



        String highLows = formatHighLows(context, cursor.getDouble(index_max_temp), cursor.getDouble(index_min_temp));

        return Utility.formatDate(cursor.getLong(index_data)) + " - " + cursor.getString(index_desc) + " - " + highLows;
    }

}

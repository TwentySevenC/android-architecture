package com.android.liujian.sunshine.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by liujian on 15/10/21.
 * Define the table and column names for the database
 */
public class WeatherContract {

    public static final String CONTENT_AUTHORITY = "com.android.liujian.sunshine";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_LOCATION = "location";
    public static final String PATH_WEATHER = "weather";


    /**
     *  Normalize all dates that go into the database to the start of the julian day
     *  at UTC
     */
    public static long normalizeDate(long startDate){
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }


    /** Inner class for location database*/
    public static final class LocationEntry implements BaseColumns{

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();

        public static String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        public static final String TABLE_NAME = "location";

        public static final String COLUMN_CITY_NAME = "location_name";

        /** Column representing location's latitude */
        public static final String COLUMN_COORD_LAT = "coord_lat";

        /** Column representing location's longitude */
        public static final String COLUMN_COORD_LONG = "coord_long";

        /** Location setting */
        public static final String COLUMN_LOCATION_SETTING = "location_setting";

        public static Uri buildLocationUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }


    /** Inner class for weather database */
    public static final class WeatherEntry implements BaseColumns{

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();

        /**
         * MIME type has the format: type/subType
         * Custom MIME type strings, also called "vendor-specific" MIME types, have more complex type
         * and subtype values. The type value is always "vnd.android.cursor.dir" for multiple rows
         * "vnd.android.cursor.item" for a single row
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + "vnd." +CONTENT_AUTHORITY + "." + PATH_WEATHER;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + "vnd." + CONTENT_AUTHORITY + "." + PATH_WEATHER;


        /** Table name*/
        public static final String TABLE_NAME = "weather";

        /** Column with the foreign key into the location table*/
        public static final String COLUMN_LOC_KEY = "location_id";

        /** Date, stored as long in milliseconds since the epoch*/
        public static final String COLUMN_DATE = "date";

        /** Weather id as returned by API, to identify the icon to be used */
        public static final String COLUMN_WEATHER_ID = "weather_id";

        public static final String COLUMN_SHORT_DESC = "short_des";

        /** Min and max temperature for the day (stored as floats)*/
        public static final String COLUMN_MIN_TEMP = "min";
        public static final String COLUMN_MAX_TEMP = "max";

        /** Pressure is stored as float */
        public static final String COLUMN_PRESSURE = "pressure";

        /** Humidity is stored as a float representing percentage (pa)*/
        public static final String COLUMN_HUMIDITY = "humidity";

        /** Windspeed is stored as a float representing wind speed mph */
        public static final String COLUMN_WIND_SPEED = "wind";

        /** Degrees are meteorological degrees */
        public static final String COLUMN_DEGREES = "degrees";

        public static Uri buildWeatherUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildWeatherLocation(String locationSetting){
            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }

        public static Uri buildweatherLocationWithStartDate(String locationSetting, long startDate){
            return CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .appendQueryParameter(COLUMN_DATE, String.valueOf(normalizeDate(startDate)))
                    .build();
        }

        public static Uri buildWeatherLocationWithDate(String locationSetting, long date){
            return CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .appendPath(String.valueOf(normalizeDate(date)))
                    .build();
        }


        public static String getLocationSettingFromUri(Uri uri){
            return uri.getPathSegments().get(1);
        }

        public static long getDateFromUri(Uri uri){
            return Long.parseLong(uri.getPathSegments().get(2));
        }


        public static long getStartDateFromUri(Uri uri){
            String startDate = uri.getQueryParameter(COLUMN_DATE);
            if(null != startDate && startDate.length() > 0){
                return Long.parseLong(startDate);
            }

            return 0;

        }

    }

}

package com.android.liujian.sunshine.data;

import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by liujian on 15/10/21.
 *
 */
public class WeatherContract {


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



    public static final class LocationEntry implements BaseColumns{
        public static final String TABLE_NAME = "location";

        public static final String COLUMN_CITY_NAME = "location_name";

        /** Column representing location's latitude */
        public static final String COLUMN_COORD_LAT = "coord_lat";

        /** Column representing location's longitude */
        public static final String COLUMN_COORD_LONG = "coord_long";

        /** Location setting */
        public static final String COLUMN_LOCATION_SETTING = "location_setting";
    }


    public static final class WeatherEntry implements BaseColumns{
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

        /** Pressure is stored as long */
        public static final String COLUMN_PRESSURE = "pressure";

        /** Humidity is stored as a float representing percentage (pa)*/
        public static final String COLUMN_HUMIDITY = "humidity";

        /** Windspeed is stored as a float representing wind speed mph */
        public static final String COLUMN_WIND_SPEED = "wind";

        /** Degrees are meteorological degrees */
        public static final String COLUMN_DEGREES = "degrees";



    }

}

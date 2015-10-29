package com.android.liujian.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.liujian.sunshine.data.WeatherContract;
import com.android.liujian.sunshine.utils.Utility;

/**
 * Created by liujian on 15/10/23.
 *
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    private static final String FORECAST_SHARE_HASHTAG = " #Sunshine App";

    public static final String DETAIL_URI = "detail_uri";

    private String mForecastStr;
    private ShareActionProvider mShareActionProvider;
    private View mView;
    private Uri mUri;

    private static final int DETAIL_LOADER = 1;

    /** Projection columns*/
    private static final String [] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    /** These indices are tied to FORECAST_COLUMNS, if FORECAST_COLUMNS changes , those must changes too */
    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    private static final int COL_WEATHER_HUMIDITY = 5;
    private static final int COL_WEATHER_WIND_SPEED = 6;
    private static final int COL_WEATHER_WIND_DEGREES = 7;
    private static final int COL_WEATHER_PRESSURE = 8;
    private static final int COL_WEATHER_CONDITION_ID = 9;

    public DetailFragment(){
        setHasOptionsMenu(true);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if(args != null){
            mUri = args.getParcelable(DetailFragment.DETAIL_URI);
        }

    }

    /**
     * A callback method
     * If the location changed, update the weather menu_detail information
     */
    public void onLocationChanged(String newLocation){
        Uri uri = mUri;
        if(null != uri){
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = uri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_detail, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        if(mForecastStr != null){
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_detail, container, false);

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }

    /**
     * Create share forecast intent
     */
    private Intent createShareForecastIntent(){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASHTAG);
        return intent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if(null != mUri){
            return new CursorLoader(getContext(), mUri, FORECAST_COLUMNS, null, null, null);
        }

        return null;

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if( data != null && data.moveToFirst()){

            long date = data.getLong(COL_WEATHER_DATE);
            String desc = data.getString(COL_WEATHER_DESC);
            double high = data.getDouble(COL_WEATHER_MAX_TEMP);
            double low = data.getDouble(COL_WEATHER_MIN_TEMP);
            float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
            float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
            float pressure = data.getFloat(COL_WEATHER_PRESSURE);
            float degrees = data.getFloat(COL_WEATHER_WIND_DEGREES);
            int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);


            /** Share weather information */
            mForecastStr = Utility.formatDate(date) + " - " + desc + " - " + Utility.formatHighLows(getContext(), high, low);
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }

            if(mView == null) return ;

            TextView dayView = (TextView)mView.findViewById(R.id.detail_day_textview);
            dayView.setText(Utility.getDayName(getContext(), date));

            TextView dateView = (TextView)mView.findViewById(R.id.detail_date_textview);
            dateView.setText(Utility.getFormattedMonthDay(getContext(), date));

            TextView highTempView = (TextView)mView.findViewById(R.id.detail_temp_high_textview);
            String highTemp = getContext().getString(R.string.format_temperature, high);
            highTempView.setText(highTemp);

            TextView lowTempView = (TextView)mView.findViewById(R.id.detail_temp_low_textview);
            String lowTemp = getContext().getString(R.string.format_temperature, low);
            lowTempView.setText(lowTemp);

            TextView forecastView = (TextView)mView.findViewById(R.id.detail_forecast_textview);
            forecastView.setText(desc);

            ImageView weatherIcon = (ImageView)mView.findViewById(R.id.detail_weather_icon);
            weatherIcon.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));

            TextView humidityView = (TextView)mView.findViewById(R.id.detail_humidity_textview);
            humidityView.setText(getString(R.string.format_humidity, humidity));

            TextView windView = (TextView)mView.findViewById(R.id.detail_wind_textview);
            windView.setText(Utility.getFormattedWind(getContext(), windSpeed, degrees));

            TextView pressureView = (TextView)mView.findViewById(R.id.detail_pressure_textview);
            pressureView.setText(getString(R.string.format_pressure, pressure));
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}

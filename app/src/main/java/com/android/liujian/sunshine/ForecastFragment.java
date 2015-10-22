package com.android.liujian.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.liujian.sunshine.data.WeatherContract;
import com.android.liujian.sunshine.utils.Utility;

import java.util.ArrayList;

/**
 * Created by liujian on 15/10/19.
 * ForecastFragment
 */
public class ForecastFragment extends Fragment{
    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();

    public ForecastFragment(){

    }


    private ForecastAdapter mWeatherListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView forecastListView = (ListView) rootView.findViewById(R.id.forecast_weather_list);
        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor)mWeatherListAdapter.getItem(position);
                String forecast = Utility.convertCursorRowToUxFormat(getContext(), cursor);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);

                startActivity(intent);
            }
        });

        String locationSetting = Utility.getPreferenceLocation(getContext());

        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC ";
        Uri weatherUri = WeatherContract.WeatherEntry.buildweatherLocationWithStartDate(locationSetting, System.currentTimeMillis());

        Cursor cursor = getContext().getContentResolver().query(weatherUri, null, null, null, sortOrder);

        mWeatherListAdapter = new ForecastAdapter(getContext(), cursor, 0);

        forecastListView.setAdapter(mWeatherListAdapter);

        return rootView;
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
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the location from sharedPreferences and update weather information
     */
    public void updateWeather(){
        String location = Utility.getPreferenceLocation(getContext());
        FetchWeatherTask task = new FetchWeatherTask(getContext());
        task.execute(location);
    }


    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }


}

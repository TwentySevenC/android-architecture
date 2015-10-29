package com.android.liujian.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.liujian.sunshine.sync.SunshineSyncAdapter;
import com.android.liujian.sunshine.utils.Utility;


public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback{
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final String DETAIL_FRAGMENT_TAG = "df_tag";

    private boolean mTwoPane;
    private String mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /*if(savedInstanceState == null){
            FragmentManager fm = getSupportFragmentManager();

            fm.beginTransaction()
                    .add(R.id.forecast_container, new ForecastFragment())
                    .commit();
        }*/

        if(findViewById(R.id.weather_detail_container) != null){
            /** The screen's small width is bigger than 600dp, so the main layout is master-menu_detail pattern*/
            mTwoPane = true;

            if(savedInstanceState == null){
                FragmentManager fm = getSupportFragmentManager();
                fm.beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailFragment(), DETAIL_FRAGMENT_TAG)
                        .commit();
            }

        }else{
            mTwoPane = false;
            getSupportActionBar().setElevation(8.0f);
        }

        ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
        ff.setUseTodayLayout(!mTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this);

    }


    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferenceLocation(this);

        if(location != null && ! location.equals(mLocation)){
            FragmentManager fm = getSupportFragmentManager();
            ForecastFragment ff = (ForecastFragment)fm.findFragmentById(R.id.fragment_forecast);
            if(ff != null){
                ff.onLocationChanged();
            }

            DetailFragment df = (DetailFragment)fm.findFragmentByTag(DETAIL_FRAGMENT_TAG);
            if(df != null){
                df.onLocationChanged(location);
            }
            mLocation = location;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onItemSelected(Uri weatherUri) {
        if(mTwoPane){
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, weatherUri);

            DetailFragment df = new DetailFragment();
            df.setArguments(args);

            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.weather_detail_container, df, DETAIL_FRAGMENT_TAG)
                    .commit();
        }else{

            Intent intent = new Intent(this, DetailActivity.class);
            intent.setData(weatherUri);
            startActivity(intent);

        }
    }
}

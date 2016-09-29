package com.android.liujian.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.textservice.TextInfo;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.liujian.sunshine.utils.Utility;

/**
 * Created by liujian on 15/10/22.
 *
 */
public class ForecastAdapter extends CursorAdapter {
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE = 1;

    private static final int VIEW_TYPE_COUNT = 2;

    private Context mContext;
    private boolean mIsUseTodayLayout;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mContext = context;
    }


    public void setUseTodayLayout(Boolean useTodayLayout){
        mIsUseTodayLayout = useTodayLayout;
    }


    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mIsUseTodayLayout)? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int position = cursor.getPosition();
        int type = getItemViewType(position);
        int layoutId = -1;

        if(type == VIEW_TYPE_FUTURE){
            layoutId = R.layout.list_item_forcast;
        }else if(type == VIEW_TYPE_TODAY){
            layoutId = R.layout.list_item_forecast_today;
        }

        View view = LayoutInflater.from(mContext).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        if(type == VIEW_TYPE_TODAY){
            viewHolder.location = (TextView)view.findViewById(R.id.list_item_location_textview);
        }

        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder)view.getTag();

        int viewType = getItemViewType(cursor.getPosition());
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);

        switch (viewType){
            case VIEW_TYPE_TODAY:
                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
                String city = cursor.getString(ForecastFragment.COL_LOCATION_CITY);
                String country = cursor.getString(ForecastFragment.COL_LOCATION_COUNTRY);
                viewHolder.location.setText(city + ", " + country);
                break;
            case VIEW_TYPE_FUTURE:
                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
                break;
        }


        /** Set weather date */
        long date = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, date));

        /** Set weather description */
        viewHolder.forecastView.setText(cursor.getString(ForecastFragment.COL_WEATHER_DESC));

        /** Set high temperature */
        double highTemp = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        viewHolder.highTempView.setText(Utility.formatTemperature(context, highTemp));

        /** Set low temperature */
        double lowTemp = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        viewHolder.lowTempView.setText(Utility.formatTemperature(context, lowTemp));

    }


    /**
     *  Use the ViewHolder pattern
     */
    private static class ViewHolder{

        public final ImageView iconView;
        public final TextView dateView ;
        public final TextView forecastView ;
        public final TextView highTempView ;
        public final TextView lowTempView;
        public TextView location = null;

        public ViewHolder(View view){
            iconView = (ImageView)view.findViewById(R.id.list_item_icon);
            dateView = (TextView)view.findViewById(R.id.list_item_date_textview);
            forecastView = (TextView)view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView)view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView)view.findViewById(R.id.list_item_low_textview);
        }

    }

}

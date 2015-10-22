package com.android.liujian.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by liujian on 15/10/21.
 *
 */
public class WeatherProvider extends ContentProvider{
    private static final String LOG_TAG = WeatherProvider.class.getSimpleName();

    private static final UriMatcher sUriMatcher = buildUriMather();
    private WeatherDbHelper mDbHelper;

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 200;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;


    static {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();

        /** Sql string: weather INNER JOIN location ON weather.location_id = location._ID */
        sWeatherByLocationSettingQueryBuilder.setTables(WeatherContract.WeatherEntry.TABLE_NAME +
                " INNER JOIN " + WeatherContract.LocationEntry.TABLE_NAME +
                " ON " + WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                " = " + WeatherContract.LocationEntry.TABLE_NAME + "." + WeatherContract.LocationEntry._ID);

    }

    /** location.location_setting = ? */
    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." +
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    /** location.location_setting = ? AND weather.date = ? */
    private static final String sLocationSettingAndDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." +
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.TABLE_NAME + "." +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";

    /** location.location_setting = ? AND weather.date >= ? */
    private static final String sLocationSettingAndStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." +
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.TABLE_NAME + "." +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";


    /** Builder the Uri matcher */
    public static UriMatcher buildUriMather() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, WeatherContract.PATH_LOCATION , LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);

        return matcher;
    }


    @Override
    public boolean onCreate() {
        mDbHelper = new WeatherDbHelper(getContext());
        return true;
    }


    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOder){
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if(startDate == 0){
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        }else{
            selection = sLocationSettingAndStartDateSelection;
            selectionArgs = new String[]{locationSetting, String.valueOf(startDate)};
        }
        return sWeatherByLocationSettingQueryBuilder.query(
                mDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null, null,
                sortOder);
    }


    private Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder){
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        String selection = sLocationSettingAndDateSelection;
        String[] selectionArgs = new String[]{locationSetting, String.valueOf(date)};

        return sWeatherByLocationSettingQueryBuilder.query(
                mDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null, null,
                sortOrder);
    }



    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int match = sUriMatcher.match(uri);
        Cursor rtnCursor;

        switch (match){

            case WEATHER_WITH_LOCATION_AND_DATE:
                rtnCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;

            case WEATHER_WITH_LOCATION:
                rtnCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;

            case WEATHER:
                rtnCursor = mDbHelper.getReadableDatabase()
                        .query(WeatherContract.WeatherEntry.TABLE_NAME,
                                projection,
                                selection,
                                selectionArgs,
                                null, null,
                                sortOrder);
                break;

            case LOCATION:
                rtnCursor = mDbHelper.getReadableDatabase()
                        .query(WeatherContract.LocationEntry.TABLE_NAME,
                                projection,
                                selection,
                                selectionArgs,
                                null, null,
                                sortOrder);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri : " + uri.toString());
        }

        rtnCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return rtnCursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        String type ;

        switch (match){

            case WEATHER:
                type = WeatherContract.WeatherEntry.CONTENT_TYPE;
                break;
            case LOCATION:
                type = WeatherContract.LocationEntry.CONTENT_TYPE;
                break;
            case WEATHER_WITH_LOCATION:
                type = WeatherContract.WeatherEntry.CONTENT_TYPE;
                break;
            case WEATHER_WITH_LOCATION_AND_DATE:
                type = WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri : " + uri.toString());
        }


        return type;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        Uri rtnUri = null;
        switch (sUriMatcher.match(uri)){
            case WEATHER:
                normalizeDate(values);
                long rowId = database.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if(rowId > 0){
                    rtnUri = WeatherContract.WeatherEntry.buildWeatherUri(rowId);
                }else if(rowId == -1){
                    throw new SQLiteException("Failed to insert values into table weather." + uri.toString());
                }
                break;
            case LOCATION:
                long id = database.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
                if(id > 0){
                    rtnUri = WeatherContract.LocationEntry.buildLocationUri(id);
                }else if(id == -1){
                    throw new SQLiteException("Failed to insert values into table location." + uri.toString());
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operation.." + uri.toString());
        }
        getContext().getContentResolver().notifyChange(uri, null);
        database.close();
        return rtnUri;
    }


    private void normalizeDate(ContentValues values){
        if(values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)){
            long date = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(date));
        }
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rows;
        switch (sUriMatcher.match(uri)){
            case WEATHER:
                rows = database.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                if(rows == -1){
                    throw new SQLiteException("Failed to delete values from table weather..");
                }
                break;
            case LOCATION:
                rows = database.delete(WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                if(rows == -1){
                    throw new SQLiteException("Failed to delete values from table location..");
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operation.." + uri.toString());
        }

        database.close();
        if(rows != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rows;
        switch (sUriMatcher.match(uri)){
            case WEATHER:
                rows = database.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION:
                rows = database.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operation.." + uri.toString());
        }

        database.close();

        if(rows != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }


    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)){
            case WEATHER:
                database.beginTransaction();
                int rtnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long id = database.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (id != -1) {
                            rtnCount++;
                        }
                    }
                    database.setTransactionSuccessful();   /** Marks the current transaction as successful*/
                } finally {
                    database.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return rtnCount;
            default:
                break;
        }

        return super.bulkInsert(uri, values);
    }
}

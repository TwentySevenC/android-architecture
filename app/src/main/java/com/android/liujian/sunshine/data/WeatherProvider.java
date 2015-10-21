package com.android.liujian.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by liujian on 15/10/21.
 *
 */
public class WeatherProvider extends ContentProvider{
    private static final String LOG_TAG = WeatherProvider.class.getSimpleName();

    private static UriMatcher sUriMatcher = buildUriMather();

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCTION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 200;


    public static UriMatcher buildUriMather() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}

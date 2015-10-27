package com.android.liujian.sunshine.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

/**
 * Created by liujian on 15/10/27.
 * Handle the transfer of data between a server and an app, using the Android
 * sync adapter framework
 */
public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter{
    private static final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    //Global variables
    //Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    public SunshineSyncAdapter(Context context,
                               boolean autoInitialize,
                               boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {

    }
}

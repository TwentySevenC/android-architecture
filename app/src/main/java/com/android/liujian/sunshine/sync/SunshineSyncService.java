package com.android.liujian.sunshine.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by liujian on 15/10/27.
 *
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowfing the sync adapter framework to
 * call onPerformSync().
 */
public class SunshineSyncService extends Service{
    //Storage for an instance of the sync adapter
    private static SunshineSyncAdapter sSunshineSyncAdapter = null;

    //Object to use a thread-safe lock
    private static final Object sSunshineSyncAdapterLock = new Object();

    /**
     * Instantiate the sync adapter
     */
    @Override
    public void onCreate() {
        /**
         * Create the sync adapter as a singleton
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized (sSunshineSyncAdapterLock){
            if(sSunshineSyncAdapter == null){
                sSunshineSyncAdapter = new SunshineSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    /**
     * Return an object that allows the system to invoke the sync adapter
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        /**
         * Get the object that allows external processes to call
         * onPerformSync(). The Object  is created in the base class
         * code when the SyncAdapter constructors call super()
         */
        return sSunshineSyncAdapter.getSyncAdapterBinder();
    }
}

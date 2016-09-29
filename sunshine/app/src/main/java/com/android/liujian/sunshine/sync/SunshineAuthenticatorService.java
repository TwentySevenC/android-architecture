package com.android.liujian.sunshine.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by liujian on 15/10/27.
 *
 * A bound Service that instantiates the authenticator
 * when started.
 *
 */
public class SunshineAuthenticatorService extends Service{
    // Instance field that stores the authenticator object
    private SunshineAuthenticator mSunshineAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mSunshineAuthenticator = new SunshineAuthenticator(this);
    }


    /**
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mSunshineAuthenticator.getIBinder();
    }
}

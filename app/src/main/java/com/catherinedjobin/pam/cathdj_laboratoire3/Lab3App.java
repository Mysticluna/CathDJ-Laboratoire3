package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.app.Application;

public class Lab3App extends Application {

    @Override
    public synchronized void onCreate() {
        super.onCreate();
        BluetoothConnectionManager.initialize(this);
    }
}

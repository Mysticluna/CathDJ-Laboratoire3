package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.os.Process;
import android.widget.Toast;

public class Lab3App extends Application {

    public final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    public void onCreate() {
        super.onCreate();

        // On vérifie si l'appareil supporte le bluetooth
        // Il renvoi un message d'erreur à l'utilisateur
        if (this.btAdapter == null) {
            Toast.makeText(this, "L'appareil ne supporte pas le bluetooth", Toast.LENGTH_LONG)
                 .show();

            // On tue le processus de l'application.
            android.os.Process.killProcess(Process.myPid());
        }

    }

}

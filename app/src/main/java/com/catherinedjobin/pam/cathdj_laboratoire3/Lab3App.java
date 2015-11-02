package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.widget.Toast;


/**
 * Created by Catherine on 2015-10-22.
 */
public class Lab3App extends Application {

    public final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    public void onCreate() {
        super.onCreate();

        // On vérifie si l'appareil supporte le bluetooth
        // Il renvoi un message d'erreur à l'utilisateur
        if (btAdapter == null)  {
            Toast.makeText(this, "L'appareil ne supporte pas le bluetooth", Toast.LENGTH_LONG).show();
        }

    }

}

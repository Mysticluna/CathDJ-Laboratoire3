package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Process;
import android.widget.Toast;

public class Lab3App extends Application {

    private BluetoothAdapter btAdapter;

    public BluetoothAdapter getBtAdapter() {
        return this.btAdapter;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Sur Android 4.3, méthode l'accès du bluetooth par rapport aux versions précédentes.
        // Voir: https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        } else {
            BluetoothManager bluetoothManager =
                ((BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE));
            if (bluetoothManager != null) {
                this.btAdapter = bluetoothManager.getAdapter();
            }
        }
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

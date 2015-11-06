package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Process;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class Lab3App extends Application {

    private final static UUID btUUID = UUID.fromString("466fca24-823d-11e5-8bcf-feff819cdc9f");
    private BluetoothAdapter btAdapter;

    public synchronized BluetoothAdapter getBtAdapter() {
        return this.btAdapter;

    }

    @Override
    public synchronized void onCreate() {
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

    /**
     * Fonction pour connecter à un appareil bluetooth distant à l'aide de son adresse MAC.
     *
     * @param address
     *     L'adresse MAC de l'appareil à connecter
     *
     * @return Un socket bluetooth connecté.
     *
     * @throws IOException
     *     s'il y a une lors de la connection
     * @throws IllegalArgumentException
     *     Si l'addresse MAC est invalide
     */
    public synchronized BluetoothSocket connectToDevice(String address)
        throws IOException, IllegalArgumentException {
        BluetoothSocket socket = null;
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            BluetoothDevice device = this.btAdapter.getRemoteDevice(address);
            if (device != null) {
                if (this.btAdapter.isDiscovering()) {
                    this.btAdapter.cancelDiscovery();
                }
                socket = device.createRfcommSocketToServiceRecord(btUUID);
                socket.connect();
            }
        } else {
            throw new IllegalArgumentException("Addresse: " + address + " est invalide");
        }
        return socket;
    }

}

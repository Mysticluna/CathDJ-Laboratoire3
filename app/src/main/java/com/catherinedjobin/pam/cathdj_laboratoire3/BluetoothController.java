package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.io.IOException;
import java.util.UUID;

/**
 * Singleton pour grouper les fonction de bluetooth
 */
public final class BluetoothController {

    private static final String TAG = BluetoothController.class.getCanonicalName();
    private final static UUID bluetoothUUID =
            UUID.fromString("466fca24-823d-11e5-8bcf-feff819cdc9f");
    private static volatile BluetoothController instance;
    private static BluetoothManager bluetoothManager;
    private static BluetoothAdapter bluetoothAdapter;
    private static Context context;
    private static boolean isInitialized = false;
    private final String appName;
    private volatile BluetoothServerSocket serverSocket;

    private BluetoothController() {
        this.appName = context.getString(R.string.app_name);
        try {
            this.serverSocket =
                    bluetoothAdapter.listenUsingRfcommWithServiceRecord(this.appName,
                                                                        bluetoothUUID
                                                                       );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized BluetoothAdapter getBluetoothAdapter() {
        if (isInitialized()) {
            return bluetoothAdapter;
        } else {
            throw new IllegalStateException(TAG + "wasn't initialized");
        }
    }

    public static synchronized BluetoothController getInstance() {
        if (isInitialized()) {
            if (instance == null) {
                instance = new BluetoothController();
            }
            return instance;
        } else {
            throw new IllegalStateException(TAG + "wasn't initialized");
        }
    }

    public static synchronized void initialize(Context applicationContext) {
        if (!isInitialized()) {
            context = applicationContext;
            // Sur Android 4.3, méthode l'accès du bluetooth par rapport aux versions précédentes.
            // Voir: https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
            if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            } else {
                bluetoothManager =
                        ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE));
                bluetoothAdapter = bluetoothManager.getAdapter();
            }
            // On vérifie si l'appareil supporte le bluetooth
            // Il renvoi un message d'erreur à l'utilisateur
            if (bluetoothAdapter == null) {
                throw new IllegalStateException("Bluetooth is not supported on this device");
            }
            isInitialized = true;
        } else {
            throw new IllegalStateException(TAG + "cannot be initialized twice");
        }
    }

    public static synchronized boolean isInitialized() {
        return isInitialized;
    }

    public static synchronized boolean isBluetoothSupported() {
        if (isInitialized()) {
            return (bluetoothAdapter != null);
        } else {
            throw new IllegalStateException(TAG + "wasn't initialized");
        }
    }

    /**
     * Fonction pour rendre l'appareil repérable par les autres.
     */
    public static void makeDiscoverable(Context context) {
        Intent intentDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intentDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        context.startActivity(intentDiscoverable);
    }

    public synchronized BluetoothServerSocket getServerSocket() {
        return this.serverSocket;
    }
}

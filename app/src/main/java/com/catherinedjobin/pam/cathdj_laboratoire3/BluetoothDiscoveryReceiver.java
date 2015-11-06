package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.R.layout;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.ArrayAdapter;

import java.util.Set;

public class BluetoothDiscoveryReceiver extends BroadcastReceiver {

    /**
     * Interface pour recevoir des
     */
    public interface BluetoothDiscoveryListener {

        void onDiscoveryStarted();

        void onDiscoveryFinished();
    }

    private final Context mContext;
    private final ArrayAdapter<String> mNewDeviceArrayAdapter;
    private final ArrayAdapter<String> mPairedDeviceArrayAdapter;
    private final BluetoothDiscoveryListener mListener;

    public BluetoothDiscoveryReceiver(Context context, BluetoothDiscoveryListener listener) {
        this.mContext = context;
        this.mListener = listener;

        this.mNewDeviceArrayAdapter =
            new ArrayAdapter<>(context, layout.simple_list_item_1);
        this.mPairedDeviceArrayAdapter =
            new ArrayAdapter<>(context, layout.simple_list_item_1);
    }

    public ArrayAdapter<String> getNewDeviceArrayAdapter() {
        return this.mNewDeviceArrayAdapter;
    }

    public ArrayAdapter<String> getPairedDeviceArrayAdapter() {
        return this.mPairedDeviceArrayAdapter;
    }

    public void searchPairedDevices() {
        // On vérifie si un appareil à proximité est déjà reconnu par notre cellulaire/tablette
        Set<BluetoothDevice> pairedDevices =
            ((Lab3App) this.mContext.getApplicationContext()).getBtAdapter().getBondedDevices();

        // Vide le contenu du ArrayAdapter avant de faire la recherche. Cela évite ainsi les
        // conflits.
        this.mPairedDeviceArrayAdapter.clear();

        if (pairedDevices.size() > 0) {

            for (BluetoothDevice device : pairedDevices) {
                // On ajoute le nom et l'addresse de l'appareil dans un arrayAdapter, ensuite on le
                // montre dans un listView
                this.mPairedDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }

        } else {
            // S'il n'y a pas d'appareil de connecté, nous ajoutons un message "d'erreur"
            // TODO : Mettre une ressource
            String noDevices = "Oups, il n'y a pas d'appareil de connecté à celui-ci.";
            this.mPairedDeviceArrayAdapter.add(noDevices);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // Quand on découvre un nouvel appareil:
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // On va chercher l'objet via l'intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // On ajoute l'appareil à la liste seulement s'il n'est déjà authentifier.
            // On montre dans la liste view le nom et l'adresse.
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                this.mNewDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }

        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            this.mListener.onDiscoveryStarted();
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            this.mListener.onDiscoveryFinished();
            if (this.mNewDeviceArrayAdapter.getCount() == 0) {
                String noDevices = "Il n'y a pas d'appareils à proximité";
                this.mNewDeviceArrayAdapter.add(noDevices);
            }

        }

    }
}


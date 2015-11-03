package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.ArrayAdapter;

import java.util.Set;

/**
 * Created by Catherine on 2015-10-22.
 */
public class BluetoothDiscoveryReceiver extends BroadcastReceiver {

    private Context mContext;
    private ArrayAdapter<String> mNewDeviceArrayAdapter;
    private ArrayAdapter<String> mPairedDeviceArrayAdapter;

    public BluetoothDiscoveryReceiver(Context context) {
        mContext = context;
        mNewDeviceArrayAdapter =
                new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
        mPairedDeviceArrayAdapter =
                new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
    }

    public ArrayAdapter<String> getNewDeviceArrayAdapter() {
        return mNewDeviceArrayAdapter;
    }

    public ArrayAdapter<String> getPairedDeviceArrayAdapter() {
        return mPairedDeviceArrayAdapter;
    }

    public void searchPairedDevices() {
        // On vérifie si un appareil à proximité est déjà reconnu par notre cellulaire/tablette
        Set<BluetoothDevice> pairedDevices =
                ((Lab3App) mContext.getApplicationContext()).btAdapter.getBondedDevices();

        // Vide le contenu du ArrayAdapter avant de faire la recherche. Cela évite ainsi les conflits.
        mPairedDeviceArrayAdapter.clear();

        if (pairedDevices.size() > 0) {

            for (BluetoothDevice device : pairedDevices) {
                // On ajoute le nom et l'addresse de l'appareil dans un arrayAdapter, ensuite on le montre dans un listView
                mPairedDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }

        } else {
            // S'il n'y a pas d'appareil de connecté, nous ajoutons un message "d'erreur"
            // TODO : Mettre une ressource
            String noDevices = "Oups, il n'y a pas d'appareil de connecté à celui-ci.";
            mPairedDeviceArrayAdapter.add(noDevices);
        }
    }

    // Fonction pour rendre l'appareil repérable par les autres.
    public void beDiscoverable() {
        Intent intentDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intentDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        mContext.startActivity(intentDiscoverable);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Quand on découvre un nouvel appareil:
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // On va chercher l'objet via l'intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // On se crée une liste view et on montre le nom et l'adresse
            mNewDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            if (mNewDeviceArrayAdapter.getCount() == 0) {
                String noDevices = "Il n'y a pas d'appareils à proximité";
                mNewDeviceArrayAdapter.add(noDevices);
            }

        }

    }
}


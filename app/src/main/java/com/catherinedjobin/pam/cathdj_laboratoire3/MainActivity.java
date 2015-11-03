package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private BluetoothDiscoveryReceiver btDiscovery;
    private static final int REQUEST_ENABLE_BT = 69;
    private static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // On demande l'autorisation d'activer le bluetooth, s'il est installé.
        // Si l'appareil n'a pas le bluetooth, l'application plante directement au démarrage.
        if ((!((Lab3App) this.getApplication()).btAdapter.isEnabled())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            btDiscovery = new BluetoothDiscoveryReceiver(this);

            // On enregistre le receiver quand l'appareil en recherche d'autres
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            // Désenregistre dans le onDestroy()
            this.registerReceiver(btDiscovery, filter);

            // Affiche une liste des appareils déjà connecté à l'appareil présent
            btDiscovery.searchPairedDevices();
            ListView lvPairedDevice = (ListView) findViewById(R.id.lvDevicePaired);
            lvPairedDevice.setAdapter(btDiscovery.getPairedDeviceArrayAdapter());
            lvPairedDevice.setOnItemClickListener(mDeviceClickListener);

            // Affiche une liste des appareils non-connectés à l'appareil présent, mais tout près de celui-ci
            ListView lvDiscoveredDevice = (ListView) findViewById(R.id.lvDeviceDiscovered);
            lvDiscoveredDevice.setAdapter(btDiscovery.getNewDeviceArrayAdapter());
            lvDiscoveredDevice.setOnItemClickListener(mDeviceClickListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Désenregistre le btDiscovery
        this.unregisterReceiver(btDiscovery);
    }

    /**
     * On se crée un OnItemClickListener pour être en mesure de choisir l'appareil auquel se connecté.
     */

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // On annule la recherche des appareils à proximité, car on ne veut pas que ça consomme trop d'énergie!
            ((Lab3App) MainActivity.this.getApplication()).btAdapter.cancelDiscovery();

            // On recherche l'adresse MAC de l'appareil
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // On crée le résultat de l'intent et on inclu l'adresse MAC
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // On établie le résultat et on fini cette activité
            setResult(Activity.RESULT_OK, intent);
        }
    };
}

package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private BluetoothDiscoveryReceiver btDiscovery;
    private static final int REQUEST_ENABLE_BT = 69;

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

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            // Désenregistre dans le onDestroy()
            this.registerReceiver(btDiscovery, filter);

            // Affiche une liste des appareils déjà connecté à l'appareil présent
            btDiscovery.searchPairedDevices();
            ListView lvPairedDevice = (ListView) findViewById(R.id.lvDevicePaired);
            lvPairedDevice.setAdapter(btDiscovery.getPairedDeviceArrayAdapter());

            // Affiche une liste des appareils non-connectés à l'appareil présent, mais tout près de celui-ci
            ListView lvDiscoveredDevice = (ListView) findViewById(R.id.lvDeviceDiscovered);
            lvDiscoveredDevice.setAdapter(btDiscovery.getNewDeviceArrayAdapter());

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

}

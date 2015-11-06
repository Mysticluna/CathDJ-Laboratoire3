package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.catherinedjobin.pam.cathdj_laboratoire3.BluetoothDiscoveryReceiver
    .BluetoothDiscoveryListener;

public class SearchDeviceActivity extends AppCompatActivity
    implements BluetoothDiscoveryListener {

    public static final int REQUEST_ENABLE_BT = 69;
    public static final int REQUEST_SELECT_DEVICE = 70;
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    /**
     * On se crée un OnItemClickListener pour être en mesure de choisir l'appareil auquel se
     * connecté.
     */
    private final AdapterView.OnItemClickListener mDeviceClickListener =
        new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                // On annule la recherche des appareils à proximité, car on ne veut pas que ça
                // consomme trop d'énergie!
                ((Lab3App) SearchDeviceActivity.this.getApplication()).getBtAdapter()
                                                                      .cancelDiscovery();

                // On recherche l'adresse MAC de l'appareil
                String info = ((TextView) v).getText().toString();
                String address = info.substring(info.length() - 17);

                // On crée le résultat de l'intent et on inclue l'adresse MAC
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

                // On établie le résultat et on fini cette activité
                SearchDeviceActivity.this.setResult(Activity.RESULT_OK, intent);
                SearchDeviceActivity.this.finish();
            }
        };
    private ProgressBar discoveryProgressBar;
    private BluetoothDiscoveryReceiver btDiscovery;
    private ListView lvDiscoveredDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_search_device);
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        // On demande l'autorisation d'activer le bluetooth, s'il est installé.
        // Si l'appareil n'a pas le bluetooth, l'application plante directement au démarrage.
        if ((!((Lab3App) this.getApplication()).getBtAdapter().isEnabled())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            this.btDiscovery = new BluetoothDiscoveryReceiver(this, this);

            // On enregistre le receiver pour des notifications quand l'appareil en recherche
            // d'autres,
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            // Quand la découverte commence
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            // Quand la découverte est finie.
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            // Désenregistre dans le onDestroy()
            this.registerReceiver(this.btDiscovery, filter);

            // Affiche une liste des appareils déjà connecté à l'appareil présent
            this.btDiscovery.searchPairedDevices();
            ListView lvPairedDevice = (ListView) this.findViewById(R.id.lvDevicePaired);
            lvPairedDevice.setAdapter(this.btDiscovery.getPairedDeviceArrayAdapter());
            lvPairedDevice.setOnItemClickListener(this.mDeviceClickListener);

            // Affiche une liste des appareils non-connectés à l'appareil présent, mais tout près de
            // celui-ci
            this.lvDiscoveredDevice = (ListView) this.findViewById(R.id.lvDeviceDiscovered);
            this.lvDiscoveredDevice.setAdapter(this.btDiscovery.getNewDeviceArrayAdapter());
            this.lvDiscoveredDevice.setOnItemClickListener(this.mDeviceClickListener);

            filter = new IntentFilter();
            this.registerReceiver(this.btDiscovery, filter);

            // On commence la recherche des appareils à proximité
            this.doDiscovery();

        }
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.discoveryProgressBar = ((ProgressBar) this.findViewById(R.id.progressBar));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Si on est déjà en recherche d'appareil, on l'annule.
        if (((Lab3App) this.getApplication()).getBtAdapter().isDiscovering()) {
            ((Lab3App) this.getApplication()).getBtAdapter().cancelDiscovery();
        }
        // Désenregistre le btDiscovery
        this.unregisterReceiver(this.btDiscovery);
    }

    /**
     * Fonction
     */
    private void doDiscovery() {
        // Si on est déjà en recherche d'appareil, on l'annule.
        if (((Lab3App) this.getApplication()).getBtAdapter().isDiscovering()) {
            ((Lab3App) this.getApplication()).getBtAdapter().cancelDiscovery();
        }

        // Ensuite, on demande de recherche avec le btAdapter
        ((Lab3App) this.getApplication()).getBtAdapter().startDiscovery();
    }

    @Override
    public void onDiscoveryStarted() {
        // Affiche Une barre de progression afin de présenter la recherche
        this.discoveryProgressBar.setVisibility(View.VISIBLE);
        this.lvDiscoveredDevice.setVisibility(View.GONE);
    }

    @Override
    public void onDiscoveryFinished() {
        this.discoveryProgressBar.setVisibility(View.GONE);
        this.lvDiscoveredDevice.setVisibility(View.VISIBLE);
        this.setTitle(this.getString(R.string.title_activity_search_device));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.setResult(RESULT_CANCELED);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

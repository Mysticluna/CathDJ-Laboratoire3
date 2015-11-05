package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY_PICK = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
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

    public void onClickSendImage(View V) {
        Intent galleryPickIntent = new Intent(Intent.ACTION_PICK);
        galleryPickIntent.setType("image/*");
        this.startActivityForResult(galleryPickIntent, REQUEST_GALLERY_PICK);
    }

    public void onClickOpenSearchDevice(View v) {
        this.startActivityForResult(new Intent(this, SearchDeviceActivity.class),
                                    SearchDeviceActivity.REQUEST_SELECT_DEVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((resultCode == RESULT_OK) && (data != null)) {
            switch (requestCode) {
                case SearchDeviceActivity.REQUEST_SELECT_DEVICE:
                    // TODO: Connections
                    String address =
                        data.getStringExtra(SearchDeviceActivity.EXTRA_DEVICE_ADDRESS);
                    Toast.makeText(this, "Addresse de l'appareil: " + address,
                                   Toast.LENGTH_LONG).show();
                    break;
                case REQUEST_GALLERY_PICK:
                    // TODO: Afficher l'image et le de partage
                    Toast.makeText(this, "Uri de l'image: " + data.getData(), Toast.LENGTH_LONG)
                         .show();
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            // TODO: String ressources
            Toast.makeText(this, "Operation annulée", Toast.LENGTH_LONG).show();
        }

    }
}

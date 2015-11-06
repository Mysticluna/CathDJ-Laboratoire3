package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.v4.graphics.BitmapCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_SEND_IMAGE = 1337;
    private BluetoothSocket clientSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);
        if (((Lab3App) this.getApplication()).getBtAdapter() != null) {
            ((Lab3App) this.getApplication()).startServer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ferme la connection si elle existe.
        if (this.clientSocket != null) {
            try {
                this.clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
        switch (item.getItemId()) {
            case R.id.action_discoverable:
                ((Lab3App) this.getApplication()).makeDiscoverable(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((resultCode == RESULT_OK) && (data != null)) {
            switch (requestCode) {
                case SearchDeviceActivity.REQUEST_SELECT_DEVICE:
                    String address =
                        data.getStringExtra(SearchDeviceActivity.EXTRA_DEVICE_ADDRESS);
                    if (address != null) {
                        if (this.clientSocket != null) {
                            try {
                                this.clientSocket.close();
                                this.clientSocket = null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            this.clientSocket =
                                ((Lab3App) this.getApplication()).connectToDevice(address);
                            Toast.makeText(this, "La connection réussie", Toast.LENGTH_SHORT)
                                 .show();

                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "La connection a échouée", Toast.LENGTH_SHORT)
                                 .show();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Adresse MAC invalide", Toast.LENGTH_SHORT)
                                 .show();
                        }
                    }
                    break;
                case REQUEST_SEND_IMAGE:
                    // TODO: Afficher l'image
                    Toast.makeText(this, "Uri de l'image: " + data.getData(), Toast.LENGTH_SHORT)
                         .show();
                    OutputStream outStream = null;
                    try {
                        Bitmap imageBitmap =
                            Media.getBitmap(this.getContentResolver(), data.getData());
                        if ((imageBitmap != null) && (this.clientSocket != null)) {
                            outStream = this.clientSocket.getOutputStream();
                            int byteCount = BitmapCompat.getAllocationByteCount(imageBitmap);
                            ByteBuffer buffer = ByteBuffer.allocate(byteCount);
                            imageBitmap.copyPixelsToBuffer(buffer);
                            outStream.write(buffer.array());
                            outStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this,
                                       "Le chargement de l'image à échoué",
                                       Toast.LENGTH_SHORT)
                             .show();
                    } finally {
                        if (outStream != null) {
                            try {
                                outStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "L'envoi a échoué", Toast.LENGTH_SHORT)
                                     .show();
                            }
                        }
                        // On ferme et supprime le socket.
                        // N.B: Le finalizer de BluetoothSocket appelle close().
                        this.clientSocket = null;
                        ((Lab3App) this.getApplication()).startServer();
                    }
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            // TODO: String ressources
            Toast.makeText(this, "L'opération a été annulée", Toast.LENGTH_SHORT).show();
        }

    }

    public void onClickSendImage(View v) {
        if (this.clientSocket == null) {
            Toast.makeText(this,
                           "Non connecté. Veuillez-vous connecter à un appareil",
                           Toast.LENGTH_LONG)
                 .show();
        } else {
            Intent galleryPickIntent = new Intent(Intent.ACTION_PICK);
            galleryPickIntent.setType("image/*");
            this.startActivityForResult(galleryPickIntent, REQUEST_SEND_IMAGE);
        }
    }

    public void onClickOpenSearchDevice(View v) {
        this.startActivityForResult(new Intent(this, SearchDeviceActivity.class),
                                    SearchDeviceActivity.REQUEST_SELECT_DEVICE);
    }
}

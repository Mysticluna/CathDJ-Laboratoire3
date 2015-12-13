package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;

public class MainActivity extends AppCompatActivity
        implements BluetoothConnectionManager.BluetoothConnectionHandler {

    private static final int REQUEST_SEND_IMAGE = 1337;
    private static final int MEGABYTE_BYTE_COUNT = 1024;
    private volatile BluetoothSocket bluetoothSocket;

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
        switch (item.getItemId()) {
            case R.id.action_discoverable:
                BluetoothConnectionManager.makeDiscoverable(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        BluetoothConnectionManager.getInstance().startServer(this);
    }

    @Override
    protected synchronized void onStop() {
        super.onStop();
        if (this.bluetoothSocket != null) {
            this.bluetoothSocket = null;
        } else {
            BluetoothConnectionManager.getInstance().stopServer();
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
                        BluetoothSocket testSocket = null;
                        OutputStreamWriter writer = null;
                        try {
                            testSocket = BluetoothConnectionManager.getInstance()
                                                                   .connectToDevice(address);

                            writer = new OutputStreamWriter(testSocket.getOutputStream());
                            writer.write("Hello");
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this,
                                           R.string.message_error_connection_failed,
                                           Toast.LENGTH_SHORT
                                          ).show();
                        } finally {
                            if (writer != null) {
                                try {
                                    writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;
                case REQUEST_SEND_IMAGE:
                    // TODO: Afficher l'image
                    Toast.makeText(this, "Uri de l'image: " + data.getData(), Toast.LENGTH_SHORT)
                         .show();
                    Toast.makeText(this,
                                   R.string.message_info_connection_success,
                                   Toast.LENGTH_SHORT
                                  ).show();
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            // TODO: String ressources
            Toast.makeText(this, R.string.message_info_operation_cancelled, Toast.LENGTH_SHORT)
                 .show();
        }

    }

    @Override
    public synchronized void onAccept(BluetoothSocket socket) throws IOException {
        if (socket != null) {
            this.bluetoothSocket = socket;
            InputStream inputStream = null;
            CharBuffer buffer = CharBuffer.allocate(MEGABYTE_BYTE_COUNT);
            int c = 0;
            try {
                inputStream = this.bluetoothSocket.getInputStream();
                while (c != -1) {
                    if (!buffer.hasRemaining()) {
                        buffer = CharBuffer.allocate(buffer.capacity() + MEGABYTE_BYTE_COUNT)
                                           .put(buffer);
                    }
                    c = inputStream.read();
                    buffer.put((char) c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (this.bluetoothSocket != null) {
                    // Supprime le socket. Ferme aussi le socket
                    this.bluetoothSocket = null;
                }
                final String result = String.valueOf(buffer.array());
                Log.d("Test", result);
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
                        BluetoothConnectionManager.getInstance().startServer(MainActivity.this);
                    }
                });
            }
        }
    }

    public synchronized void onClickSendImage(View v) {
        if (this.bluetoothSocket == null) {
            Toast.makeText(this,
                           R.string.message_error_not_connected,
                           Toast.LENGTH_LONG
                          )
                 .show();
        } else {
            Intent galleryPickIntent = new Intent(Intent.ACTION_PICK);
            galleryPickIntent.setType("image/*");
            this.startActivityForResult(galleryPickIntent, REQUEST_SEND_IMAGE);
        }
    }

    public synchronized void onClickOpenSearchDevice(View v) {
        this.startActivityForResult(new Intent(this, SearchDeviceActivity.class),
                                    SearchDeviceActivity.REQUEST_SELECT_DEVICE
                                   );
    }

}

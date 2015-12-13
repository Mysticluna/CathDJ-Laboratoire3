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
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements BluetoothConnectionManager.BluetoothConnectionHandler {

    private static final int REQUEST_SEND_IMAGE = 1337;
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
                        try {
                            testSocket = BluetoothConnectionManager.getInstance()
                                                                   .connectToDevice(address);
                            OutputStream stream = testSocket.getOutputStream();
                            stream.write("Hello".getBytes());
                            stream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this,
                                           R.string.message_error_connection_failed,
                                           Toast.LENGTH_SHORT
                                          ).show();
                        } finally {
                            if (testSocket != null) {
                                try {
                                    testSocket.close();
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
            InputStream stream = null;
            try {
                stream = this.bluetoothSocket.getInputStream();
                byte[] bytes = new byte[1024 * 1024];
                Arrays.fill(bytes, (byte) 0);
                StringBuilder builder = new StringBuilder();
                int count = stream.read(bytes);
                while (count != -1) {
                    builder.append(Arrays.toString(bytes));
                    count = stream.read(bytes);
                }
                Log.d("TEST", builder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
            this.bluetoothSocket = null;
        }
    }

    public void onClickSendImage(View v) {
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

    public void onClickOpenSearchDevice(View v) {
        this.startActivityForResult(new Intent(this, SearchDeviceActivity.class),
                                    SearchDeviceActivity.REQUEST_SELECT_DEVICE
                                   );
    }

}

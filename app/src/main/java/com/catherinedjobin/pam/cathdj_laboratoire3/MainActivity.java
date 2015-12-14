package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.v4.graphics.BitmapCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity
        implements BluetoothConnectionManager.BluetoothConnectionHandler {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_SEND_IMAGE = 1337;
    private static final int MEGABYTE_BYTE_COUNT = 1024 * 1024;
    private static final int RGBA_PIXEL_BYTE_COUNT = 4;
    /**
     * Synchronize block object
     */
    private final Object lock = new Object();
    private ViewSwitcher viewSwitcher;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);
        synchronized (this.lock) {
            this.viewSwitcher = (ViewSwitcher) this.findViewById(R.id.main_view_switcher);
            this.viewSwitcher.setInAnimation(this, android.R.anim.fade_in);
            this.viewSwitcher.setOutAnimation(this, android.R.anim.fade_out);
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
    protected void onStop() {
        super.onStop();
        BluetoothConnectionManager.getInstance().stopServer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        synchronized (this.lock) {
            if ((resultCode == RESULT_OK) && (data != null)) {
                switch (requestCode) {
                    case SearchDeviceActivity.REQUEST_SELECT_DEVICE:
                        this.address =
                                data.getStringExtra(SearchDeviceActivity.EXTRA_DEVICE_ADDRESS);

                        break;
                    case REQUEST_SEND_IMAGE:
                        try {
                            this.viewSwitcher.showNext();
                            Bitmap bitmap =
                                    Media.getBitmap(this.getContentResolver(), data.getData());
                            int byteCount = BitmapCompat.getAllocationByteCount(bitmap);
                            ByteBuffer buffer = ByteBuffer.allocate(byteCount);
                            bitmap.copyPixelsToBuffer(buffer);
                            Log.d(TAG, "Buffer: " + Arrays.toString(buffer.array()));
                            buffer.rewind();
                            BluetoothConnectionManager.getInstance()
                                                      .connectToDevice(this.address, this, buffer);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(this,
                                           R.string.message_error_image_loading_failed,
                                           Toast.LENGTH_SHORT
                                          ).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this,
                                           R.string.message_error_connection_failed,
                                           Toast.LENGTH_SHORT
                                          ).show();

                        } finally {
                            this.viewSwitcher.reset();
                            this.viewSwitcher.showNext();
                        }
                        break;
                    default:
                        break;
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.message_info_operation_cancelled, Toast.LENGTH_SHORT)
                     .show();
            }
        }

    }

    @Override
    public void onAccept(BluetoothSocket socket) throws IOException {
        if (socket != null) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (MainActivity.this.lock) {
                        MainActivity.this.viewSwitcher.showNext();
                    }
                }
            });
            InputStream socketStream = null;
            ByteBuffer buffer = null;
            try {
                socketStream = socket.getInputStream();
                // On alloue un buffer d'un megabyte au départ
                buffer = ByteBuffer.allocate(MEGABYTE_BYTE_COUNT);
                int count = 0;
                byte[] pixel = new byte[RGBA_PIXEL_BYTE_COUNT];
                do {
                    if (!buffer.hasRemaining()) {
                        // on grossi le buffer d'un megabyte si on a pas assez d'espace.
                        buffer = ByteBuffer.allocate(buffer.capacity() + MEGABYTE_BYTE_COUNT)
                                           .put(buffer);
                    }
                    count = socketStream.read(pixel);
                    buffer.put(pixel);
                } while (count == RGBA_PIXEL_BYTE_COUNT);
            } catch (IOException e) {
                if (!e.getMessage().contains("bt socket closed, read return: -1")) {
                    e.printStackTrace();
                }
            } finally {
                if (socketStream != null) {
                    socketStream.close();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (buffer != null) {
                    Log.d(TAG, "Buffer: " + Arrays.toString(buffer.array()));
                    // FIXME: BROKEN... :(
                    Bitmap bitmap = BitmapFactory.decodeByteArray(buffer.array(),
                                                                  buffer.arrayOffset(),
                                                                  buffer.position()
                                                                 );
                    if (bitmap != null) {
                        String title = this.getString(R.string.app_name) + " "
                                       + Calendar.getInstance().toString();
                        // Sauvegarde de l'image
                        Media.insertImage(this.getContentResolver(), bitmap, title, null);
                    } else {
                        this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                               R.string.message_error_image_sending_failed,
                                               Toast.LENGTH_SHORT
                                              ).show();
                            }
                        });

                    }
                }
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (MainActivity.this.lock) {
                            MainActivity.this.viewSwitcher.reset();
                            MainActivity.this.viewSwitcher.showNext();
                        }
                        BluetoothConnectionManager.getInstance().startServer(MainActivity.this);
                    }
                });
            }
        }
    }

    @Override
    public void onConnect(BluetoothSocket socket, final ByteBuffer buffer) {
        final ProgressBar progressBar = ((ProgressBar) this.findViewById(R.id.main_progress));
        OutputStream socketStream = null;
        try {
            socketStream = socket.getOutputStream();
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (MainActivity.this.lock) {
                        MainActivity.this.viewSwitcher.showNext();
                        progressBar.setIndeterminate(false);
                        progressBar.setMax(buffer.limit());
                    }
                }
            });
            // Tableau servant de tête de lecture.
            // On essaie de lire un pixel RGBA soit 4 bytes.
            byte[] pixel = new byte[RGBA_PIXEL_BYTE_COUNT];
            while (buffer.hasRemaining()) {
                // Mesure de précaution au cas où un pixel du buffer est plus petit que 4 bytes.
                // Cette mesure est prise au cas ou le bitmap provient d'un format compressé ou
                // qu'il ne contient pas de canal alpha.
                if (buffer.remaining() < RGBA_PIXEL_BYTE_COUNT) {
                    pixel = new byte[buffer.remaining()];
                }
                // Charge le "pixel" sur le stream
                synchronized (this.lock) {
                    buffer.get(pixel);
                    socketStream.write(pixel);
                }
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (MainActivity.this.lock) {
                            progressBar.setProgress(buffer.position());
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socketStream != null) {
                try {
                    socketStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (MainActivity.this.lock) {
                        MainActivity.this.address = null;
                        MainActivity.this.viewSwitcher.reset();
                        progressBar.setProgress(0);
                        progressBar.setIndeterminate(true);
                        MainActivity.this.viewSwitcher.showNext();
                    }
                    BluetoothConnectionManager.getInstance().startServer(MainActivity.this);
                }
            });
        }

    }

    public void onClickSendImage(View v) {
        synchronized (this.lock) {
            if (this.address == null) {
                Toast.makeText(this,
                               R.string.message_error_no_device,
                               Toast.LENGTH_LONG
                              )
                     .show();
            } else {
                Intent galleryPickIntent = new Intent(Intent.ACTION_PICK);
                galleryPickIntent.setType("image/*");
                this.startActivityForResult(galleryPickIntent, REQUEST_SEND_IMAGE);
            }
        }
    }

    public synchronized void onClickOpenSearchDevice(View v) {
        BluetoothConnectionManager.getInstance().stopServer();
        this.startActivityForResult(new Intent(this, SearchDeviceActivity.class),
                                    SearchDeviceActivity.REQUEST_SELECT_DEVICE
                                   );
    }

}

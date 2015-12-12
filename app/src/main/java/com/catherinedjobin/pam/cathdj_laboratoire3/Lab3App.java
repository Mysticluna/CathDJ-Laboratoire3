package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Lab3App extends Application {

    private final static UUID btUUID =
            UUID.fromString("466fca24-823d-11e5-8bcf-feff819cdc9f");
    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
    private BluetoothAdapter btAdapter;
    private volatile BluetoothServerSocket serverSocket;
    private final Runnable acceptTask = new Runnable() {
        @Override
        public synchronized void run() {
            // TODO: Unifier l'accept socket et client socket de main activity.
            BluetoothSocket acceptSocket = null;
            try {
                String name = Lab3App.this.getString(R.string.app_name);
                Lab3App.this.serverSocket =
                        BluetoothController.getBluetoothAdapter()
                                           .listenUsingRfcommWithServiceRecord(name, btUUID);
                acceptSocket = Lab3App.this.serverSocket.accept();
                Lab3App.this.serverSocket.close();
                Bitmap bitmap = BitmapFactory.decodeStream(acceptSocket.getInputStream());
                File downloadDir =
                        Lab3App.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if ((downloadDir != null) && (downloadDir.mkdir() || downloadDir.isDirectory())) {
                    String date = DateFormat.getMediumDateFormat(Lab3App.this)
                                            .format(Calendar.getInstance().getTime());
                    File file = new File(downloadDir, date + ".jpeg");
                    final FileOutputStream stream = new FileOutputStream(file);
                    if (bitmap.compress(CompressFormat.JPEG, 100, stream)) {
                        stream.flush();
                        MediaScannerConnection
                                .scanFile(Lab3App.this,
                                          new String[]{file.toString()},
                                          null,
                                          new OnScanCompletedListener() {
                                              @Override
                                              public void onScanCompleted(String path, Uri uri) {
                                                  try {
                                                      stream.close();
                                                  } catch (IOException e) {
                                                      e.printStackTrace();
                                                  }
                                              }
                                          }
                                         );

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (acceptSocket != null) {
                    try {
                        acceptSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    public synchronized void onCreate() {
        super.onCreate();
        BluetoothController.initialize(this);
        BluetoothController bluetoothController1 =
                BluetoothController.getInstance();
        BluetoothController bluetoothController2 =
                BluetoothController.getInstance();
        if (BuildConfig.DEBUG && !bluetoothController1.equals(bluetoothController2)) {
            throw new AssertionError(bluetoothController1 + "!=" +
                                     bluetoothController2);
        }

    }

    /**
     * Fonction pour connecter à un appareil bluetooth distant à l'aide de son adresse MAC.
     *
     * @param address
     *         L'adresse MAC de l'appareil à connecter
     *
     * @return Un socket bluetooth connecté.
     *
     * @throws IOException
     *         s'il y a une lors de la connection
     * @throws IllegalArgumentException
     *         Si l'addresse MAC est invalide
     */
    public synchronized BluetoothSocket connectToDevice(String address)
            throws IOException, IllegalArgumentException {
        this.stopServer();
        BluetoothSocket socket = null;
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            BluetoothDevice device = this.btAdapter.getRemoteDevice(address);
            if (device != null) {
                if (this.btAdapter.isDiscovering()) {
                    this.btAdapter.cancelDiscovery();
                }
                socket = device.createRfcommSocketToServiceRecord(btUUID);
                socket.connect();
            }
        } else {
            throw new IllegalArgumentException("Addresse: " + address + " est invalide");
        }
        return socket;
    }

    public synchronized void startServer() {
        this.threadExecutor.submit(this.acceptTask);
    }

    public synchronized void stopServer() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

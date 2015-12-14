package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe qui gère les fonctions de connection Bluetooth
 */
public final class BluetoothConnectionManager {

    /**
     * Interface pour déléguer la gestion des connection au Bluetooth.
     */
    public interface BluetoothConnectionHandler {

        /**
         * Callback sur requête de connection au serveur.
         *
         * @param socket
         *         Un {@link BluetoothSocket} connecté produit de
         *         {@link BluetoothServerSocket#accept()}
         */
        void onAccept(BluetoothSocket socket) throws IOException;

        /**
         * Callback sur de connection au client.
         *
         * @param socket
         *         Un {@link BluetoothSocket} connecté
         * @param data
         *         Un {@link ByteBuffer} qui représente les données à envoyer
         */
        void onConnect(BluetoothSocket socket, ByteBuffer data);
    }

    private static final class BluetoothThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(0);
        private final StringBuilder nameBuilder;

        public BluetoothThreadFactory(String name) {
            this.nameBuilder = new StringBuilder(name);
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            this.nameBuilder.append(" #").append(this.counter.incrementAndGet());
            Thread result = new Thread(r, this.nameBuilder.toString());
            this.nameBuilder.delete(this.nameBuilder.lastIndexOf(" #"), this.nameBuilder.length());
            return result;
        }
    }

    private static final String TAG = BluetoothConnectionManager.class.getCanonicalName();
    private final static UUID bluetoothUUID =
            UUID.fromString("466fca24-823d-11e5-8bcf-feff819cdc9f");
    private static volatile BluetoothConnectionManager instance;
    private static BluetoothAdapter bluetoothAdapter;
    private static Context context;
    private static boolean isInitialized = false;
    private final String appName;
    private BluetoothServerSocket serverSocket;
    private ExecutorService executorService;

    private BluetoothConnectionManager() {
        this.appName = context.getString(R.string.app_name);
    }

    /**
     * @return L'{@link BluetoothAdapter} de l'appareil
     */
    public static synchronized BluetoothAdapter getBluetoothAdapter() {
        if (isInitialized()) {
            return bluetoothAdapter;
        } else {
            throw new IllegalStateException(TAG + "wasn't initialized");
        }
    }

    /**
     * Accesseur de l'instance du {@link BluetoothConnectionManager}
     *
     * @return L'instance du {@link BluetoothConnectionManager}
     */
    public static synchronized BluetoothConnectionManager getInstance() {
        if (isInitialized()) {
            if (instance == null) {
                instance = new BluetoothConnectionManager();
            }
            return instance;
        } else {
            throw new IllegalStateException(TAG + "wasn't initialized");
        }
    }

    /**
     * Initialize le {@link BluetoothConnectionManager} et vérifie si
     * l'appareil supporte le bluetooth
     *
     * @param applicationContext
     *         le context du {@link BluetoothConnectionManager}
     */
    public static synchronized void initialize(Context applicationContext) {
        if (!isInitialized()) {
            context = applicationContext;
            // Sur Android 4.3, méthode l'accès du bluetooth par rapport aux versions précédentes.
            // Voir: https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
            if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            } else {
                BluetoothManager bluetoothManager =
                        ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE));
                bluetoothAdapter = bluetoothManager.getAdapter();
            }
            // On vérifie si l'appareil supporte le bluetooth
            if (bluetoothAdapter == null) {
                String error = context.getString(R.string.message_error_device_not_supported);
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                throw new IllegalStateException(error);
            }
            // On active le bluetooth si c'est pas fait.
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
            isInitialized = true;
        } else {
            throw new IllegalStateException(TAG + "cannot be initialized twice");
        }
    }

    public static synchronized boolean isInitialized() {
        return isInitialized;
    }

    public static synchronized boolean isBluetoothSupported() {
        if (isInitialized()) {
            return (bluetoothAdapter != null);
        } else {
            throw new IllegalStateException(TAG + "wasn't initialized");
        }
    }

    /**
     * Fonction pour rendre l'appareil repérable par les autres.
     */
    public static void makeDiscoverable(Context context) {
        Intent intentDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intentDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        context.startActivity(intentDiscoverable);
    }

    /**
     * Accesseur du {@link BluetoothServerSocket} du {@link BluetoothConnectionManager}. Crée et
     * initialise un si nécéssaire
     *
     * @return Le {@link BluetoothServerSocket} du {@link BluetoothConnectionManager}.
     *
     * @throws IOException
     *         pour les mêmes raison que
     *         {@link BluetoothAdapter#listenUsingRfcommWithServiceRecord(String, UUID)}
     */
    public synchronized BluetoothServerSocket getServerSocket() throws IOException {
        if (this.serverSocket == null) {
            this.serverSocket =
                    BluetoothConnectionManager.getBluetoothAdapter()
                                              .listenUsingRfcommWithServiceRecord(this.appName,
                                                                                  bluetoothUUID
                                                                                 );
        }
        return this.serverSocket;
    }

    /**
     * Accesseur de  l'{@link ExecutorService} du {@link BluetoothConnectionManager}.
     * L'{@link ExecutorService} sert de Threadpool pour l'application.
     *
     * @return L'{@link ExecutorService} du {@link BluetoothConnectionManager}
     */
    public synchronized ExecutorService getExecutorService() {
        if (this.executorService == null) {
            this.executorService =
                    Executors.newCachedThreadPool(new BluetoothThreadFactory("Bluetooth Thread"));
        }
        return this.executorService;
    }

    /**
     * Démarre le serveur bluetooth et attend une connection.
     *
     * @param handler
     *         Délégué qui gère la réception des client du serveur
     *
     * @see BluetoothConnectionHandler
     */
    public synchronized void startServer(final BluetoothConnectionHandler handler) {
        this.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    BluetoothSocket socket = BluetoothConnectionManager.this.getServerSocket()
                                                                            .accept();
                    BluetoothConnectionManager.this.stopServer();
                    handler.onAccept(socket);
                } catch (IOException e) {
                    if (e.getMessage().compareTo("Operation Canceled") != 0) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Stop le serveur bluetooth
     */
    public synchronized void stopServer() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                this.serverSocket = null;
            }
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
    public synchronized void connectToDevice(String address,
                                             final BluetoothConnectionHandler handler,
                                             final ByteBuffer data)
            throws IOException, IllegalArgumentException {
        this.stopServer();
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(address);
            if (device != null) {
                if (getBluetoothAdapter().isDiscovering()) {
                    getBluetoothAdapter().cancelDiscovery();
                }
                final BluetoothSocket socket =
                        device.createRfcommSocketToServiceRecord(bluetoothUUID);
                socket.connect();
                this.getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        handler.onConnect(socket, data);
                    }
                });
            }
        } else {
            throw new IllegalArgumentException("Addresse: " + address + " est invalide");
        }
    }

}

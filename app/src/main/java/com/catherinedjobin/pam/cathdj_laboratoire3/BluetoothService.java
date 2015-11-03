package com.catherinedjobin.pam.cathdj_laboratoire3;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Catherine on 2015-11-03.
 */
public class BluetoothService {

    private BluetoothServerSocket btServerSocket;
    private BluetoothSocket btSocket;
    private BluetoothDevice btDevice;

    private InputStream inStream;
    private OutputStream outStream;

    private Context mContext;
    private Handler mHandler;

    private static final String NAME = "service_name";
    private static final UUID mUUID = UUID.fromString("466fca24-823d-11e5-8bcf-feff819cdc9f");
    private static final int MESSAGE_READ = 666;

    public BluetoothService(Context context) {
        mContext = context;


    }

    /**
     *
     */
    public class Server extends Thread {

        public Server() {

            // On crée un object temporaire pour stocker l'information du BtServerSocket
            BluetoothServerSocket tmp = null;

            try {
                tmp = ((Lab3App) mContext.getApplicationContext()).btAdapter.listenUsingRfcommWithServiceRecord(NAME, mUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            btServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket btSocket = null;

            // On écoute le socket jusqu'à temps que l'on ait une réponse ou bien qu'une erreur arrive.
            while (true) {
                try {
                    btSocket = btServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                // Si une connexion a été acceptée
                if (btSocket != null) {
                    // On gère les connexions
                    new Manager(btSocket);

                    // On essaie de fermer la connexion serveur. Try/Catch, si jamais il y a une erreur.
                    try {
                        btServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Annule l'écoute du socket et arrête le thread.
        public void cancel() {
            try {
                btServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     */
    public class Connected extends Thread {
        public Connected(BluetoothDevice device) {
            // On crée un objet temporaire pour le btSocket
            BluetoothSocket tmp = null;
            btDevice = device;

            // btSocket se connecte avec le btDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(mUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            btSocket = tmp;
        }

        public void run() {
            // On arrête la découverte des appareils, sinon la connexion sera ralentie.
            ((Lab3App) mContext.getApplicationContext()).btAdapter.cancelDiscovery();

            // On essaie de se connecter au socket, jusqu'à ce qu'il y ait une exception.
            try {
                btSocket.connect();
            } catch (IOException connectE) {

                // Si on est incapable de se connecter, on ferme le socket et on sort.
                try {
                    btSocket.close();
                } catch (IOException closeE) {
                    closeE.printStackTrace();
                }

                connectE.printStackTrace();
                return;
            }

            new Manager(btSocket);
        }

        // Annule la connexion présente et ferme le socket
        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     */
    public class Manager extends Thread {
        public Manager(BluetoothSocket socket) {
            btSocket = socket;
            // On crée un objet temporaire pour le InputStream et le OutputStream
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;

        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // On écoute le InputStream jusqu'à ce qu'il y ait une exception
            while (true) {
                try {
                    // On lit le InputStream
                    bytes = inStream.read(buffer);

                    // On envoie les bytes à l'activité
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

            }
        }

        // On appel cette fonction pour envoyer des informations à l'appareil
        public void write(byte[] bytes) {
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // On appel cette fonction pour fermer les connexions
        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

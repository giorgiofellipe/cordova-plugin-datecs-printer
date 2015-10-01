package com.giorgiofellipe.datecsprinter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;
import java.net.Socket;
import java.net.UnknownHostException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.ProgressDialog;
import android.widget.Toast;
import android.util.Log;
import android.content.Intent;
import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.datecs.api.BuildInfo;
import com.datecs.api.printer.PrinterInformation;
import com.datecs.api.printer.Printer;
import com.datecs.api.printer.ProtocolAdapter;

public class DatecsSDKWrapper {
    private static final String LOG_TAG = "BluetoothPrinter";
    private Printer mPrinter;
    private ProtocolAdapter mProtocolAdapter;
    private BluetoothSocket mBluetoothSocket;
    private boolean mRestart;
    private String mAddress;
    private CallbackContext mConnectCallbackContext;
    private ProgressDialog mDialog;
    private CordovaInterface mCordova;

    /**
     * Interface de eventos da Impressora
     */
    private final ProtocolAdapter.ChannelListener mChannelListener = new ProtocolAdapter.ChannelListener() {
        @Override
        public void onReadEncryptedCard() {
            // TODO: onReadEncryptedCard
        }

        @Override
        public void onReadCard() {
            // TODO: onReadCard
        }

        @Override
        public void onReadBarcode() {
            // TODO: onReadBarcode
        }

        @Override
        public void onPaperReady(boolean state) {
            if (state) {
                showToast("Papel ok");
            } else {
                showToast("Sem papel");
            }
        }

        @Override
        public void onOverHeated(boolean state) {
            if (state) {
                showToast("Superaquecimento");
            }
        }

        @Override
        public void onLowBattery(boolean state) {
            if (state) {
                showToast("Pouca bateria");
            }
        }
    };

    /**
     * Busca todos os dispositivos Bluetooth pareados com o device
     *
     * @param callbackContext
     */
    protected void getBluetoothPairedDevices(CallbackContext callbackContext) {
        BluetoothAdapter mBluetoothAdapter = null;
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                callbackContext.error("Adaptador Bluetooth não disponível");
                return;
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.mCordova.getActivity().startActivityForResult(enableBluetooth, 0);
            }
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                JSONArray json = new JSONArray();
                for (BluetoothDevice device : pairedDevices) {
                    Hashtable map = new Hashtable();
                    map.put("type", device.getType());
                    map.put("address", device.getAddress());
                    map.put("name", device.getName());
                    JSONObject jObj = new JSONObject(map);
                    json.put(jObj);
                }
                callbackContext.success(json);
            } else {
                callbackContext.error("Nenhum dispositivo Bluetooth encontrado");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
    }

    /**
     * Seta em memória o endereço da impressora cuja conexão está sendo estabelecida
     *
     * @param address
     */
    protected void setAddress(String address) {
        mAddress = address;
    }

    public void setCordova(CordovaInterface cordova) {
        mCordova = cordova;
    }

    /**
     * Valida o endereço da impressora e efetua a conexão
     *
     * @param callbackContext
     */
    protected void connect(CallbackContext callbackContext) {
        mConnectCallbackContext = callbackContext;
        closeActiveConnections();
        if (BluetoothAdapter.checkBluetoothAddress(mAddress)) {
            establishBluetoothConnection(mAddress, callbackContext);
        }
    }

    /**
     * Encerra todas as conexões com impressoras e dispositivos Bluetooth ativas
     */
    private synchronized void closeActiveConnections() {
        closePrinterConnection();
        closeBluetoothConnection();
    }

    /**
     * Encerra a conexão com a impressora
     */
    private synchronized void closePrinterConnection() {
        if (mPrinter != null) {
            mPrinter.release();
        }

        if (mProtocolAdapter != null) {
            mProtocolAdapter.release();
        }
    }

    /**
     * Finaliza o socket Bluetooth e encerra todas as conexões
     */
    private synchronized void closeBluetoothConnection() {
        BluetoothSocket socket = mBluetoothSocket;
        mBluetoothSocket = null;
        if (socket != null) {
            try {
                Thread.sleep(50);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Efetiva a conexão com o dispositivo Bluetooth
     *
     * @param address
     * @param callbackContext
     */
    private void establishBluetoothConnection(final String address, final CallbackContext callbackContext) {
        runJob(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = adapter.getRemoteDevice(address);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                InputStream in = null;
                OutputStream out = null;
                adapter.cancelDiscovery();

                try {
                    mBluetoothSocket = createBluetoothSocket(device, uuid, callbackContext);
                    Thread.sleep(50);
                    mBluetoothSocket.connect();
                    in = mBluetoothSocket.getInputStream();
                    out = mBluetoothSocket.getOutputStream();
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Falha ao conectar: " + e.getMessage(), false);
                    return;
                }

                try {
                    initializePrinter(in, out, callbackContext);
                    showToast("Impressora Conectada!");
                } catch (IOException e) {
                    e.printStackTrace();
                    showError("Falha ao inicializar: " + e.getMessage(), false);
                    return;
                }
            }
        }, "Impressora", "Conectando..");
    }

    /**
     * Cria um socket Bluetooth
     *
     * @param device
     * @param uuid
     * @param callbackContext
     * @return BluetoothSocket
     * @throws IOException
     */
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device, UUID uuid, final CallbackContext callbackContext) throws IOException {
        try {
            Method method = device.getClass().getMethod("createRfcommSocketToServiceRecord", new Class[] { UUID.class });
            return (BluetoothSocket) method.invoke(device, uuid);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Falha ao criar comunicação: " + e.getMessage(), false);
        }
        return device.createRfcommSocketToServiceRecord(uuid);
    }

    /**
     * Inicializa a troca de dados com a impressora
     * @param inputStream
     * @param outputStream
     * @param callbackContext
     * @throws IOException
     */
    protected void initializePrinter(InputStream inputStream, OutputStream outputStream, CallbackContext callbackContext) throws IOException {
        mProtocolAdapter = new ProtocolAdapter(inputStream, outputStream);
        if (mProtocolAdapter.isProtocolEnabled()) {
            final ProtocolAdapter.Channel channel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);
            channel.setListener(mChannelListener);
            // Create new event pulling thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        try {
                            channel.pullEvent();
                        } catch (IOException e) {
                            e.printStackTrace();
                            showError(e.getMessage(), mRestart);
                            break;
                        }
                    }
                }
            }).start();
            mPrinter = new Printer(channel.getInputStream(), channel.getOutputStream());
        } else {
            mPrinter = new Printer(mProtocolAdapter.getRawInputStream(), mProtocolAdapter.getRawOutputStream());
        }
        callbackContext.success();
    }

    /**
     * Alimenta papel à impressora (rola papel em branco)
     *
     * @param linesQuantity
     */
    private void feedPaper(int linesQuantity) {
        if (linesQuantity < 0 || linesQuantity > 255) {
            mConnectCallbackContext.error("A quantidade de linhas deve estar entre 0 e 255")
        }
        try {
            mPrinter.feedPaper(linesQuantity);
        } catch (Exception e) {
            mConnectCallbackContext.error("Erro ao alimentar papel à impressora: " + e.getMessage());
        }
        mConnectCallbackContext.success();
    }

    /**
     * Wrapper para criação de Threads
     *
     * @param job
     * @param jobTitle
     * @param jobName
     */
    private void runJob(final Runnable job, final String jobTitle, final String jobName) {
        // Start the job from main thread
        mCordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Progress dialog available due job execution
                final ProgressDialog dialog = new ProgressDialog(mCordova.getActivity());
                dialog.setTitle(jobTitle);
                dialog.setMessage(jobName);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            job.run();
                        } finally {
                            dialog.dismiss();
                        }
                    }
                });
                t.start();
            }
        });
    }

    /**
     * Exibe Toast de erro
     *
     * @param text
     * @param resetConnection
     */
    private void showError(final String text, boolean resetConnection) {
        mCordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mCordova.getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
        if (resetConnection) {
            connect(mConnectCallbackContext);
        }
    }

    /**
     * Exibe mensagem Toast
     *
     * @param text
     */
    private void showToast(final String text) {
        mCordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mCordova.getActivity().isFinishing()) {
                    Toast.makeText(mCordova.getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
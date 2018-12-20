package com.giorgiofellipe.datecsprinter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Exception;
import java.util.Hashtable;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.net.Socket;
import java.net.UnknownHostException;
import java.lang.reflect.Method;

import android.app.Application;
import android.app.Activity;
import android.app.ProgressDialog;
import android.widget.Toast;
import android.util.Log;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.datecs.api.BuildInfo;
import com.datecs.api.printer.ProtocolAdapter;

public class DatecsSDKWrapper {
    private static final String LOG_TAG = "BluetoothPrinter";
    private Printer mPrinter;
    private ProtocolAdapter mProtocolAdapter;
    private BluetoothSocket mBluetoothSocket;
    private boolean mRestart;
    private String mAddress;
    private CallbackContext mConnectCallbackContext;
    private CallbackContext mCallbackContext;
    private ProgressDialog mDialog;
    private CordovaInterface mCordova;
    private CordovaWebView mWebView;
    private final Application app;

    /**
     * Interface de eventos da Impressora
     */
    private final ProtocolAdapter.PrinterListener mChannelListener = new ProtocolAdapter.PrinterListener() {
        @Override
        public void onPaperStateChanged(boolean hasNoPaper) {
            if (hasNoPaper) {
                sendStatusUpdate(true, false);
                showToast(DatecsUtil.getStringFromStringResource(app, "no_paper"));
            } else {
                sendStatusUpdate(true, true);
                showToast(DatecsUtil.getStringFromStringResource(app, "paper_ok"));
            }
        }

        @Override
        public void onThermalHeadStateChanged(boolean overheated) {
            if (overheated) {
                closeActiveConnections();
                sendStatusUpdate(false, false);
                showToast(DatecsUtil.getStringFromStringResource(app, "overheating"));
            }
        }

        @Override
        public void onBatteryStateChanged(boolean lowBattery) {
            sendStatusUpdate(true, true, lowBattery);
            showToast(DatecsUtil.getStringFromStringResource(app, "low_battery"));
        }
    };

    private Map<Integer, String> errorCode = new HashMap<Integer, String>();

    public DatecsSDKWrapper(CordovaInterface cordova) {
        mCordova = cordova;
        app = cordova.getActivity().getApplication();

        this.errorCode.put(1, DatecsUtil.getStringFromStringResource(app, "err_no_bt_adapter"));
        this.errorCode.put(2, DatecsUtil.getStringFromStringResource(app, "err_no_bt_device"));
        this.errorCode.put(3, DatecsUtil.getStringFromStringResource(app, "err_lines_number"));
        this.errorCode.put(4, DatecsUtil.getStringFromStringResource(app, "err_feed_paper"));
        this.errorCode.put(5, DatecsUtil.getStringFromStringResource(app, "err_print"));
        this.errorCode.put(6, DatecsUtil.getStringFromStringResource(app, "err_fetch_st"));
        this.errorCode.put(7, DatecsUtil.getStringFromStringResource(app, "err_fetch_tmp"));
        this.errorCode.put(8, DatecsUtil.getStringFromStringResource(app, "err_print_barcode"));
        this.errorCode.put(9, DatecsUtil.getStringFromStringResource(app, "err_print_test"));
        this.errorCode.put(10, DatecsUtil.getStringFromStringResource(app, "err_set_barcode"));
        this.errorCode.put(11, DatecsUtil.getStringFromStringResource(app, "err_print_img"));
        this.errorCode.put(12, DatecsUtil.getStringFromStringResource(app, "err_print_rect"));
        this.errorCode.put(13, DatecsUtil.getStringFromStringResource(app, "err_print_rect"));
        this.errorCode.put(14, DatecsUtil.getStringFromStringResource(app, "err_print_rect"));
        this.errorCode.put(15, DatecsUtil.getStringFromStringResource(app, "err_print_rect"));
        this.errorCode.put(16, DatecsUtil.getStringFromStringResource(app, "err_print_rect"));
        this.errorCode.put(17, DatecsUtil.getStringFromStringResource(app, "err_print_rect"));
        this.errorCode.put(18, DatecsUtil.getStringFromStringResource(app, "failed_to_connect"));
        this.errorCode.put(19, DatecsUtil.getStringFromStringResource(app, "err_bt_socket"));
        this.errorCode.put(20, DatecsUtil.getStringFromStringResource(app, "failed_to_initialize"));
        this.errorCode.put(21, DatecsUtil.getStringFromStringResource(app, "err_write"));
        this.errorCode.put(22, DatecsUtil.getStringFromStringResource(app, "err_print_qrcode"));
    }

    private JSONObject getErrorByCode(int code) {
        return this.getErrorByCode(code, null);
    }

    private JSONObject getErrorByCode(int code, Exception exception) {
        JSONObject json = new JSONObject();
        try {
            json.put("errorCode", code);
            json.put("message", errorCode.get(code));
            if (exception != null) {
                json.put("exception", exception.getMessage());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            showToast(e.getMessage());
        }
        return json;
    }

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
                callbackContext.error(this.getErrorByCode(1));
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
                    int deviceType = 0;
                    try {
                        Method method = device.getClass().getMethod("getType");
                        if (method != null) {
                            deviceType = (Integer) method.invoke(device);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    map.put("type", deviceType);
                    map.put("address", device.getAddress());
                    map.put("name", device.getName());
                    String deviceAlias = device.getName();
                    try {
                        Method method = device.getClass().getMethod("getAliasName");
                        if (method != null) {
                            deviceAlias = (String) method.invoke(device);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    map.put("aliasName", deviceAlias);
                    JSONObject jObj = new JSONObject(map);
                    json.put(jObj);
                }
                callbackContext.success(json);
            } else {
                callbackContext.error(this.getErrorByCode(2));
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

    protected void setWebView(CordovaWebView webView) {
        mWebView = webView;
    }

    // public void setCordova(CordovaInterface cordova) {
    //     mCordova = cordova;
    // }

    /**
     * CallbackContext de cada requisição, que efetivamente recebe os retornos dos métodos
     *
     * @param callbackContext
     */
    public void setCallbackContext(CallbackContext callbackContext) {
        mCallbackContext = callbackContext;
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
    public synchronized void closeActiveConnections() {
        closePrinterConnection();
        closeBluetoothConnection();
    }

    /**
     * Encerra a conexão com a impressora
     */
    private synchronized void closePrinterConnection() {
        if (mPrinter != null) {
            mPrinter.close();
        }

        if (mProtocolAdapter != null) {
            mProtocolAdapter.close();
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
        final DatecsSDKWrapper sdk = this;
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
                } catch (IOException e) {
                    //fallback
                    try {
                        mBluetoothSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device, 1);
                        Thread.sleep(50);
                        mBluetoothSocket.connect();
                        in = mBluetoothSocket.getInputStream();
                        out = mBluetoothSocket.getOutputStream();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        callbackContext.error(sdk.getErrorByCode(18, ex));
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(sdk.getErrorByCode(18, e));
                    return;
                }

                try {
                    initializePrinter(in, out, callbackContext);
                    showToast(DatecsUtil.getStringFromStringResource(app, "printer_connected"));
                    sendStatusUpdate(true);
                } catch (IOException e) {
                    e.printStackTrace();
                    callbackContext.error(sdk.getErrorByCode(20));
                    return;
                }
            }
        }, DatecsUtil.getStringFromStringResource(app, "printer"), DatecsUtil.getStringFromStringResource(app, "connecting"));
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
            sendStatusUpdate(false);
            callbackContext.error(this.getErrorByCode(19));
            showError(DatecsUtil.getStringFromStringResource(app, "failed_to_comm") + ": " + e.getMessage(), false);
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
            mProtocolAdapter.setPrinterListener(mChannelListener);
            
            final ProtocolAdapter.Channel channel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);
            
            mPrinter = new Printer(channel.getInputStream(), channel.getOutputStream());
        } else {
            mPrinter = new Printer(mProtocolAdapter.getRawInputStream(), mProtocolAdapter.getRawOutputStream());
        }


        mPrinter.setConnectionListener(new Printer.ConnectionListener() {
            @Override
            public void onDisconnect() {
                sendStatusUpdate(false);
            }
        });
        callbackContext.success();
    }

    /**
     * Alimenta papel à impressora (rola papel em branco)
     *
     * @param linesQuantity
     */
    public void feedPaper(int linesQuantity) {
        if (linesQuantity < 0 || linesQuantity > 255) {
            mCallbackContext.error(this.getErrorByCode(3));
        }
        try {
            mPrinter.feedPaper(linesQuantity);
            mPrinter.flush();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(4, e));
        }
    }

    /**
     * Print text expecting markup formatting tags (default encoding is ISO-8859-1)
     *
     * @param text
     */
    public void printTaggedText(String text) {
        printTaggedText(text, "ISO-8859-1");
    }

    /**
     * Print text expecting markup formatting tags and a defined charset
     *
     * @param text
     * @param charset
     */
    public void printTaggedText(String text, String charset) {
        try {
            mPrinter.printTaggedText(text, charset);
            mPrinter.flush();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            mCallbackContext.error(this.getErrorByCode(5, e));
        }
    }

    /**
     * Converts HEX String into byte array and write
     *
     * @param String
     */
    public void writeHex(String s) {
        write(DatecsUtil.hexStringToByteArray(s));
    }

    /**
     * Writes all bytes from the specified byte array to this printer
     *
     * @param byte[]
     */
    public void write(byte[] b) {
        try {
            mPrinter.write(b);
            mPrinter.flush();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(21, e));
        }
    }

    /**
     * Return what is the Printer current status
     */
    public void getStatus() {
        try {
            int status = mPrinter.getStatus();
            mCallbackContext.success(status);
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(6, e));
        }
    }

    /**
     * Return Printer's head temperature
     */
    public void getTemperature() {
        try {
            int temperature = mPrinter.getTemperature();
            mCallbackContext.success(temperature);
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(7, e));
        }
    }

    public void setBarcode(int align, boolean small, int scale, int hri, int height) {
        try {
            mPrinter.setBarcode(align, small, scale, hri, height);
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(10, e));
        }
    }

    /**
     * Print a Barcode
     *
     * @param type
     * @param data
     */
    public void printBarcode(int type, String data) {
        try {
            mPrinter.printBarcode(type, data);
            mPrinter.flush();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(8, e));
        }
    }

    /**
     * Print a QRCode
     *
     * @param size - the size of symbol, value in {1, 4, 6, 8, 10, 12, 14}
     * @param eccLv - the error collection control level, where 1: L (7%), 2: M (15%), 3: Q (25%), 4: H (30%)
     * @param data - the QRCode data. The data must be between 1 and 448 symbols long.
     */
    public void printQRCode(int size, int eccLv, String data) {
        try {
            mPrinter.printQRCode(size, eccLv, data);
            mPrinter.flush();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(22, e));
        }
    }


    /**
     * Print a selftest page
     */
    public void printSelfTest() {
        try {
            mPrinter.printSelfTest();
            mPrinter.flush();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(9, e));
        }
    }

    public void drawPageRectangle(int x, int y, int width, int height, int fillMode) {
        try {
            mPrinter.drawPageRectangle(x, y, width, height, fillMode);
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(12, e));
        }
    }

    public void drawPageFrame(int x, int y, int width, int height, int fillMode, int thickness) {
        try {
            mPrinter.drawPageFrame(x, y, width, height, fillMode, thickness);
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(16, e));
        }
    }

    public void selectStandardMode() {
        try {
            mPrinter.selectStandardMode();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(13, e));
        }
    }

    public void selectPageMode() {
        try {
            mPrinter.selectPageMode();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(14, e));
        }
    }

    public void printPage() {
        try {
            mPrinter.printPage();
            mPrinter.flush();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(17, e));
        }
    }

    public void setPageRegion(int x, int y, int width, int height, int direction) {
        try {
            mPrinter.setPageRegion(x, y, width, height, direction);
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(15, e));
        }
    }


    /**
     * Print an image
     *
     * @param image String (BASE64 encoded image)
     * @param width
     * @param height
     * @param align
     */
    public void printImage(String image, int width, int height, int align) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            byte[] decodedByte = Base64.decode(image, 0);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
            final int imgWidth = bitmap.getWidth();
            final int imgHeight = bitmap.getHeight();
            final int[] argb = new int[imgWidth * imgHeight];

            bitmap.getPixels(argb, 0, imgWidth, 0, 0, imgWidth, imgHeight);
            bitmap.recycle();

            mPrinter.printImage(argb, width, height, align, true);
            mPrinter.flush();
            mCallbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackContext.error(this.getErrorByCode(11, e));
        }
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
        //we'l ignore toasts at the moment
    //    mCordova.getActivity().runOnUiThread(new Runnable() {
    //        @Override
    //        public void run() {
    //            Toast.makeText(mCordova.getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    //        }
    //    });
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
        //we'l ignore toasts at the moment
//        mCordova.getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (!mCordova.getActivity().isFinishing()) {
//                    Toast.makeText(mCordova.getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param connection status
     */
    private void sendStatusUpdate(boolean isConnected, boolean hasPaper, boolean lowBattery) {
        final Intent intent = new Intent("DatecsPrinter.connectionStatus");

        Bundle b = new Bundle();
        b.putBoolean("isConnected", isConnected);
        b.putBoolean("hasPaper", hasPaper);
        b.putBoolean("lowBattery", lowBattery);
        intent.putExtras(b);

        LocalBroadcastManager.getInstance(mWebView.getContext()).sendBroadcastSync(intent);
    }
    
    private void sendStatusUpdate(boolean isConnected, boolean hasPaper) {
        this.sendStatusUpdate(isConnected, hasPaper, false);
    }

    private void sendStatusUpdate(boolean isConnected) {
        this.sendStatusUpdate(isConnected, true, false);
    }
}

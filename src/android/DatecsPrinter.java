package com.giorgiofellipe.datecsprinter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;
import java.net.Socket;
import java.net.UnknownHostException;
import java.lang.reflect.Method;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

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
import com.datecs.api.card.FinancialCard;
import com.datecs.api.printer.PrinterInformation;
import com.datecs.api.printer.Printer;
import com.datecs.api.printer.ProtocolAdapter;

public class DatecsPrinter extends CordovaPlugin {
	private static final String LOG_TAG = "BluetoothPrinter";
	private Printer mPrinter;
	private ProtocolAdapter mProtocolAdapter;
	private BluetoothSocket mBluetoothSocket;
	private boolean mRestart;
	private String mAddress;
	private CallbackContext mConnectCallbackContext;
	private ProgressDialog mDialog;
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
				toast("Papel ok");
			} else {
				toast("Sem papel");
			}
		}

		@Override
		public void onOverHeated(boolean state) {
			if (state) {
				toast("Superaquecimento");
			}
		}

		@Override
		public void onLowBattery(boolean state) {
			if (state) {
				toast("Pouca bateria");
			}
		}
	};

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("teste")) {
			this.getTeste(callbackContext);
		} else if (action.equals("listBluetoothDevices")) {
			this.getBluetoothPairedDevices(callbackContext);
		} else if (action.equals("connect")) {
			this.setAddress(args.getString(0));
			this.connect(callbackContext);
		} else {
			return false;
		}
		return true;
	}

	protected void getTeste(CallbackContext callbackContext) {
		JSONArray json = new JSONArray();
		json.put("teste");
		json.put("outro");
		json.put("novo");
		callbackContext.success(json);
	}

	protected void getBluetoothPairedDevices(CallbackContext callbackContext) {
		BluetoothAdapter mBluetoothAdapter = null;
		String errMsg = null;
		try {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				errMsg = "No bluetooth adapter available";
				Log.e(LOG_TAG, errMsg);
				callbackContext.error(errMsg);
				return;
			}
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
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
				callbackContext.error("No Bluetooth Device Found");
			}
		} catch (Exception e) {
			errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}
	}

	protected void setAddress(String address) {
		mAddress = address;
	}

	protected void connect(CallbackContext callbackContext) {
		closeActiveConnection();
		if (BluetoothAdapter.checkBluetoothAddress(mAddress)) {
			establishBluetoothConnection(mAddress, callbackContext);
		}
	}

	private synchronized void closeActiveConnection() {
		closePrinterConnection();
		closeBluetoothConnection();
	}

	private synchronized void closePrinterConnection() {
		if (mPrinter != null) {
			mPrinter.release();
		}

		if (mProtocolAdapter != null) {
			mProtocolAdapter.release();
		}
	}

	private synchronized void closeBluetoothConnection() {
		BluetoothSocket s = mBluetoothSocket;
		mBluetoothSocket = null;
		if (s != null) {
			try {
				Thread.sleep(50);
				s.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void establishBluetoothConnection(final String address, final CallbackContext callbackContext) {
		doJob(new Runnable() {
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
					error("Falha ao conectar: " + e.getMessage(), false);
					return;
				}

				try {
					initPrinter(in, out, callbackContext);
					toast("Impressora Conectada!");
				} catch (IOException e) {
					e.printStackTrace();
					error("Falha ao inicializar: " + e.getMessage(), false);
					return;
				}
			}
		}, "Impressora", "Conectando..");
	}

	private BluetoothSocket createBluetoothSocket(BluetoothDevice device, UUID uuid, final CallbackContext callbackContext) throws IOException {
		try {
			final Method m = device.getClass().getMethod("createRfcommSocketToServiceRecord", new Class[] { UUID.class });
			return (BluetoothSocket) m.invoke(device, uuid);
		} catch (Exception e) {
			e.printStackTrace();
			error("Falha ao criar comunicação: " + e.getMessage(), false);
		}
		return device.createRfcommSocketToServiceRecord(uuid);
	}

	protected void initPrinter(InputStream inputStream, OutputStream outputStream, CallbackContext callbackContext) throws IOException {
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
							error(e.getMessage(), mRestart);
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

	private void doJob(final Runnable job, final String jobTitle, final String jobName) {
		// Start the job from main thread
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// Progress dialog available due job execution
				final ProgressDialog dialog = new ProgressDialog(cordova.getActivity());
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

	private void error(final String text, boolean resetConnection) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(cordova.getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
			}
		});
		if (resetConnection) {
			connect(mConnectCallbackContext);
		}
	}

	private void toast(final String text) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!cordova.getActivity().isFinishing()) {
					Toast.makeText(cordova.getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
}
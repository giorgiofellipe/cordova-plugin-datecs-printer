package com.giorgiofellipe.datecsprinter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class DatecsPrinter extends CordovaPlugin {
	private static final BluetoothConnector bluetoothConnector = new BluetoothConnector();

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("teste")) {
			this.getTeste(callbackContext);
		} else if (action.equals("listBluetoothDevices")) {
			bluetoothConnector.getBluetoothPairedDevices(callbackContext);
		} else if (action.equals("connect")) {
			bluetoothConnector.setAddress(args.getString(0));
			bluetoothConnector.connect(callbackContext);
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
}
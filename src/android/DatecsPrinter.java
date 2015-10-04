package com.giorgiofellipe.datecsprinter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class DatecsPrinter extends CordovaPlugin {
	private static final DatecsSDKWrapper printer = new DatecsSDKWrapper();
	private enum Option {
		listBluetoothDevices,
				connect,
				disconnect,
				feedPaper,
				printText,
				getStatus,
				getTemperature,
				printBarcode,
				printImage,
				printLogo,
				printSelfTest;
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		printer.setCordova(cordova);
		printer.setCallbackContext(callbackContext);

		Option option = null;
		try {
			option = Option.valueOf(action);
		} catch (Exception e) {
			return false;
		}
		switch (option) {
			case listBluetoothDevices:
				printer.getBluetoothPairedDevices(callbackContext);
				break;
			case connect:
				printer.setAddress(args.getString(0));
				printer.connect(callbackContext);
				break;
			case disconnect:
				try {
					printer.closeActiveConnections();
					callbackContext.success("Impressora desconectada");
				} catch (Exception e) {
					callbackContext.error("Erro ao desconectar impressora: " + e.getMessage());
				}
				break;
			case feedPaper:
				printer.feedPaper(args.getInt(0));
				break;
			case printText:
				String text = args.getString(0);
				String charset = args.getString(1);
				printer.printTaggedText(text, charset);
				break;
			case getStatus:
				printer.getStatus();
				break;
			case getTemperature:
				printer.getTemperature();
				break;
			case setBarcode:
				int align = args.getInt(0);
				boolean small = args.getBoolean(1);
				int scale = args.getInt(2);
				int hri = args.getInt(3);
				int height = args.getInt(4);
				printer.setBarcode(align, small, scale, hri, height);
				break;
			case printBarcode:
				int type = args.getInt(0);
				String data = args.getString(1);
				printer.printBarcode(type, data);
				break;
			case printImage:
				break;
			case printLogo:
				break;
			case printSelfTest:
				printer.printSelfTest();
				break;
		}
		return true;
	}
}
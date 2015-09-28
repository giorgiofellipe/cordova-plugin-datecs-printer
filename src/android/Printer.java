package com.giorgiofellipe.datecsprinter;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class Printer extends CordovaPlugin {

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		switch (action) {
			case: "teste":
				getTeste(callbackContext);
				return true;
				break;
		}
		return false;
	}

	protected void getTeste(CallbackContext callbackContext) {
		JSONArray json = new JSONArray();
		json.put('teste');
		callbackContext.success(json);
	}
}
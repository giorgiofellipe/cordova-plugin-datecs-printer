package com.giorgiofellipe.datecsprinter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class Printer extends CordovaPlugin {

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("teste")) {
				getTeste(callbackContext);
				return true;
		}
		return false;
	}

	protected void getTeste(CallbackContext callbackContext) {
		JSONArray json = new JSONArray();
		json.put("teste");
		callbackContext.success(json);
	}
}
package com.giorgiofellipe.datecsprinter;

import android.content.Context;

public class DatecsUtil {
  public static int getResource(Context context, String resourceName, String resourceType) {
    return context.getResources().getIdentifier(resourceName, resourceType, context.getPackageName());
  }

  public static int getStringResource(Context context, String resourceName) {
    return DatecsUtil.getResource(context, resourceName, "string");
  }

  public static String getStringFromStringResource(Context context, String resourceName) {
    return context.getString(DatecsUtil.getStringResource(context, resourceName));
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }
}

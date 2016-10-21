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
}

package com.giorgiofellipe.datecsprinter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

public class DatecsUtil {
  private static int[] p0 = new int[]{0, 128};
  private static int[] p1 = new int[]{0, 64};
  private static int[] p2 = new int[]{0, 32};
  private static int[] p3 = new int[]{0, 16};
  private static int[] p4 = new int[]{0, 8};
  private static int[] p5 = new int[]{0, 4};
  private static int[] p6 = new int[]{0, 2};

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

  public static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    float scaleWidth = (float)w / (float)width;
    float scaleHeight = (float)h / (float)height;
    Matrix matrix = new Matrix();
    matrix.postScale(scaleWidth, scaleHeight);
    Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    return resizedBitmap;
  }

  public static Bitmap toGrayscale(Bitmap bmpOriginal) {
    int height = bmpOriginal.getHeight();
    int width = bmpOriginal.getWidth();
    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bmpGrayscale);
    Paint paint = new Paint();
    ColorMatrix cm = new ColorMatrix();
    cm.setSaturation(0.0F);
    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
    paint.setColorFilter(f);
    c.drawBitmap(bmpOriginal, 0.0F, 0.0F, paint);
    return bmpGrayscale;
  }

  public static byte[] thresholdToBWPic(Bitmap mBitmap) {
    int[] pixels = new int[mBitmap.getWidth() * mBitmap.getHeight()];
    byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight()];
    mBitmap.getPixels(pixels, 0, mBitmap.getWidth(), 0, 0, mBitmap.getWidth(), mBitmap.getHeight());
    format_K_threshold(pixels, mBitmap.getWidth(), mBitmap.getHeight(), data);
    return data;
  }

  private static void format_K_threshold(int[] orgpixels, int xsize, int ysize, byte[] despixels) {
    int graytotal = 0;
    boolean grayave = true;
    int k = 0;

    int i;
    int j;
    int gray;
    for(i = 0; i < ysize; ++i) {
      for(j = 0; j < xsize; ++j) {
        gray = orgpixels[k] & 255;
        graytotal += gray;
        ++k;
      }
    }

    int var10 = graytotal / ysize / xsize;
    k = 0;

    for(i = 0; i < ysize; ++i) {
      for(j = 0; j < xsize; ++j) {
        gray = orgpixels[k] & 255;
        if(gray > var10) {
          despixels[k] = 0;
        } else {
          despixels[k] = 1;
        }

        ++k;
      }
    }

  }

  public static byte[] eachLinePixToCmd(byte[] src, int nWidth, int nMode) {
    int nHeight = src.length / nWidth;
    int nBytesPerLine = nWidth / 8;
    byte[] data = new byte[nHeight * (8 + nBytesPerLine)];
    boolean offset = false;
    int k = 0;

    for(int i = 0; i < nHeight; ++i) {
      int var10 = i * (8 + nBytesPerLine);
      data[var10 + 0] = 29;
      data[var10 + 1] = 118;
      data[var10 + 2] = 48;
      data[var10 + 3] = (byte)(nMode & 1);
      data[var10 + 4] = (byte)(nBytesPerLine % 256);
      data[var10 + 5] = (byte)(nBytesPerLine / 256);
      data[var10 + 6] = 1;
      data[var10 + 7] = 0;

      for(int j = 0; j < nBytesPerLine; ++j) {
        data[var10 + 8 + j] = (byte)(p0[src[k]] + p1[src[k + 1]] + p2[src[k + 2]] + p3[src[k + 3]] + p4[src[k + 4]] + p5[src[k + 5]] + p6[src[k + 6]] + src[k + 7]);
        k += 8;
      }
    }

    return data;
  }
}

package app.color.madness;

import android.graphics.Bitmap;

public class Mad {
  static {
    System.loadLibrary("madness");
  }

  public static final int MAX_SENSITIVITY = 25;
  public static final int MIN_SENSITIVITY = 3;

  public static native int palette(Bitmap bitmap, int sensitivity);

  public static int safePalette(Bitmap bitmap, int sensitivity) {
    sensitivity = Math.min(sensitivity, MAX_SENSITIVITY);
    sensitivity = Math.max(sensitivity, MIN_SENSITIVITY);
    return palette(bitmap, sensitivity);
  }
}

package app.color.utils;

import android.content.Context;
import android.widget.Toast;

public class Toaster {
  public static void showLong(Context context, int resId) {
    if (context != null) {
      Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
    }
  }

  public static void showShort(Context context, String msg) {
    if (context != null) {
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
  }

  public static void showShort(Context context, int resId, Object... objects) {
    if (context != null) {
      showShort(context, context.getString(resId, objects));
    }
  }

  public static void showShort(Context context, int resId) {
    if (context != null) {
      showShort(context, context.getString(resId));
    }
  }
}

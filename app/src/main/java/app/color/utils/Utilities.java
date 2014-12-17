package app.color.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.Log;
import app.color.BuildConfig;
import app.color.model.AppEntry;
import com.crashlytics.android.Crashlytics;
import com.squareup.picasso.Cache;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * Various utilities shared amongst the Launcher's classes.
 */
public final class Utilities {
  public static final float ICON_RESIZE_FACTOR = 0.8f;
  static int sIconSize = -1;
  static int sIconHeight = -1;

  static final Paint sPaint = new Paint();
  static final Rect sBounds = new Rect();
  static final Rect sOldBounds = new Rect();
  static final Canvas sCanvas = new Canvas();

  static {
    sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
        Paint.FILTER_BITMAP_FLAG));
  }

  /**
   * Returns a Bitmap representing the thumbnail of the specified Bitmap.
   * The size of the thumbnail is defined by the dimension
   * android.R.dimen.launcher_application_icon_size.
   * <p/>
   * This method is not thread-safe and should be invoked on the UI thread only.
   *
   * @param context The application's context.
   * @param bitmap  The bitmap to get a thumbnail of.
   * @return A thumbnail for the specified bitmap or the bitmap itself if the
   * thumbnail could not be created.
   */
  public static Bitmap createBitmapThumbnail(Context context, Bitmap bitmap) {
    if (sIconSize == -1) {
      final Resources resources = context.getResources();
      sIconSize = sIconHeight = (int) ((int) resources.getDimension(
          android.R.dimen.app_icon_size) * ICON_RESIZE_FACTOR);
    }

    int width = sIconSize;
    int height = sIconHeight;

    final int bitmapWidth = bitmap.getWidth();
    final int bitmapHeight = bitmap.getHeight();

    if (width > 0 && height > 0 && (width < bitmapWidth || height < bitmapHeight)) {
      final float ratio = (float) bitmapWidth / bitmapHeight;

      if (bitmapWidth > bitmapHeight) {
        height = (int) (width / ratio);
      } else if (bitmapHeight > bitmapWidth) {
        width = (int) (height * ratio);
      }

      final Bitmap.Config c = (width == sIconSize && height == sIconHeight) ?
          bitmap.getConfig() : Bitmap.Config.ARGB_8888;
      final Bitmap thumb = Bitmap.createBitmap(sIconSize, sIconHeight, c);
      final Canvas canvas = sCanvas;
      final Paint paint = sPaint;
      canvas.setBitmap(thumb);
      paint.setDither(false);
      paint.setFilterBitmap(true);
      sBounds.set((sIconSize - width) / 2, (sIconHeight - height) / 2, width, height);
      sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
      canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);
      return thumb;
    }

    return bitmap;
  }

  /**
   * Copies {@code newLength} elements from {@code original} into a new array.
   * If {@code newLength} is greater than {@code original.length}, the result is padded
   * with the value {@code null}.
   *
   * @param original  the original array
   * @param newLength the length of the new array
   * @return the new array
   * @throws NegativeArraySizeException if {@code newLength < 0}
   * @throws NullPointerException       if {@code original == null}
   * @since 1.6
   */
  public static <T> T[] copyOf(T[] original, int newLength) {
    if (original == null) {
      throw new NullPointerException("original == null");
    }
    if (newLength < 0) {
      throw new NegativeArraySizeException(Integer.toString(newLength));
    }
    return copyOfRange(original, 0, newLength);
  }

  /**
   * Copies elements from {@code original} into a new array, from indexes start (inclusive) to
   * end (exclusive). The original order of elements is preserved.
   * If {@code end} is greater than {@code original.length}, the result is padded
   * with the value {@code null}.
   *
   * @param original the original array
   * @param start    the start index, inclusive
   * @param end      the end index, exclusive
   * @return the new array
   * @throws ArrayIndexOutOfBoundsException if {@code start < 0 || start > original.length}
   * @throws IllegalArgumentException       if {@code start > end}
   * @throws NullPointerException           if {@code original == null}
   * @since 1.6
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] copyOfRange(T[] original, int start, int end) {
    int originalLength = original.length; // For exception priority compatibility.
    if (start > end) {
      throw new IllegalArgumentException();
    }
    if (start < 0 || start > originalLength) {
      throw new ArrayIndexOutOfBoundsException();
    }
    int resultLength = end - start;
    int copyLength = Math.min(resultLength, originalLength - start);
    T[] result = (T[]) Array.newInstance(original.getClass().getComponentType(), resultLength);
    System.arraycopy(original, start, result, 0, copyLength);
    return result;
  }

  public static Bitmap drawableToBitmap(Drawable drawable) {
    final int width = drawable.getIntrinsicWidth();
    final int height = drawable.getIntrinsicHeight();
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return bitmap;
  }

  public static Bitmap resolveIcon(Context context, AppEntry tag) {
    PackageManager pm = context.getPackageManager();
    try {
      final ComponentName activityName = new ComponentName(tag.packageName, tag.className);
      Drawable iconDrawable = pm.getActivityIcon(activityName);
      return drawableToBitmap(iconDrawable);
    } catch (PackageManager.NameNotFoundException e) {
      Utilities.report(e);
    }
    return null;
  }

  private static Picasso sPicasso;

  public static Picasso getPicasso(Context context) {
    if (sPicasso != null) {
      return sPicasso;
    }

    context = context.getApplicationContext();

    final Cache cache = new LruCache(context);
    final Picasso.Builder builder = new Picasso.Builder(context)
        .memoryCache(cache);

    sPicasso = builder.build();
    return sPicasso;
  }

  public static Bitmap retrieveIcon(Context context, String id) {
    File dst = getIconFile(context, id);
    if (!dst.exists()) {
      return null;
    }
    return BitmapFactory.decodeFile(dst.toString());
  }

  public static void saveIcon(Context context, String id, Bitmap icon) {
    File dst = getIconFile(context, id);
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(dst);
      icon.compress(Bitmap.CompressFormat.PNG, 100, out);
    } catch (FileNotFoundException e) {
      Utilities.report(e);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          Utilities.report(e);
        }
      }
    }
  }

  private static File getIconFile(Context context, String id) {
    return new File(context.getCacheDir(), "icon_" + id.replaceAll("/", ""));
  }

  private static final boolean PRNT_STACK = BuildConfig.DEBUG;

  public static void report(Throwable e) {
    if (e == null) {
      return;
    }
    if (PRNT_STACK) {
      e.printStackTrace();
    } else if (e.getMessage() != null) {
      Log.e("app.color", e.getMessage());
    }
    Crashlytics.logException(e);
  }

}
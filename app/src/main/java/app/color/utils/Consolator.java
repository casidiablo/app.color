package app.color.utils;

import android.content.Context;
import android.os.Vibrator;

public class Consolator {
  private static Consolator instance;
  private final Vibrator mVibrator;
  static final int HAPTIC_FEEDBACK = 40;
  static final int HAPTIC_FEEDBACK_SHORT = 20;

  private Consolator(Context context) {
    mVibrator = (Vibrator) context.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
  }

  public static Consolator get(Context context) {
    if (instance == null) {
      instance = new Consolator(context);
    }
    return instance;
  }

  public void vibrate() {
    if (mVibrator != null) {
      mVibrator.vibrate(HAPTIC_FEEDBACK);
    }
  }

  public void vibrateShort() {
    if (mVibrator != null) {
      mVibrator.vibrate(HAPTIC_FEEDBACK_SHORT);
    }
  }
}

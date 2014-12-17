package app.color.view;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.SqlAdapter;

import java.util.ArrayList;

import app.color.R;
import app.color.model.AppEntry;
import app.color.utils.GestureManager;
import app.color.utils.Toaster;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CreateGestureView extends RelativeLayout {
  public static final float LENGTH_THRESHOLD = 120.0f;
  public static final double GOOD_ENOUGH_SCORE = 4.0;
  private final GestureManager mGesturesManager;
  private final AppEntry mAppEntry;
  private OnGestureFinishedListener mOnGestureFinishedListener;
  private Gesture mLastGesture;

  public CreateGestureView(final Context context, final AppEntry appEntry, Bitmap icon) {
    super(context);
    inflate(context, R.layout.create_new_gesture, this);
    ButterKnife.inject(this);
    mAppEntry = appEntry;

    mGesturesManager = GestureManager.getInstance(context);


    TextView appLabel = (TextView) findViewById(R.id.gesture_app_label);
    appLabel.setText(getContext().getString(R.string.assign_gesture_to, appEntry.label));

    BitmapDrawable drawable = new BitmapDrawable(getResources(), icon);
    appLabel.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);

    GestureOverlayView gestureOverlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
    gestureOverlay.addOnGestureListener(new GesturesProcessor());

    setFocusable(true);
    requestFocus();
  }

  @OnClick(R.id.confirm_gesture) void confirmGesture() {
    if (mLastGesture == null) {
      Toaster.showShort(getContext(), R.string.no_gesture_drawn);
      return;
    }

    ArrayList<Prediction> recognize = mGesturesManager.recognize(mLastGesture);
    if (recognize != null && !recognize.isEmpty()) {
      Prediction prediction = recognize.get(0);
      // this gesture is too similar to
      if (prediction.score > GOOD_ENOUGH_SCORE && !prediction.name.equalsIgnoreCase(mAppEntry.id)) {
        SqlAdapter adapter = Persistence.getAdapter(getContext());
        AppEntry found = adapter.findFirst(AppEntry.class, "_id = ?", new String[]{prediction.name});
        if (found != null) {
          Toaster.showShort(getContext(), R.string.gesture_already_used_for, found.label);
          return;
        } else {
          mGesturesManager.delete(prediction.name);
        }
      }
    }

    mGesturesManager.save(mAppEntry.id, mLastGesture);

    if (mOnGestureFinishedListener != null) {
      Toaster.showLong(getContext(), R.string.gesture_successful);
      mOnGestureFinishedListener.onGestureFinished(this);
    }
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      if (event.getAction() == KeyEvent.ACTION_UP) {
        mOnGestureFinishedListener.onGestureFinished(this);
      }
      return true;
    }
    return super.dispatchKeyEvent(event);
  }

  public void setOnGestureFinishedListener(OnGestureFinishedListener onGestureFinishedListener) {
    mOnGestureFinishedListener = onGestureFinishedListener;
  }

  private class GesturesProcessor implements GestureOverlayView.OnGestureListener {
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
      mLastGesture = null;
    }

    public void onGesture(GestureOverlayView overlay, MotionEvent event) {
    }

    public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
      mLastGesture = overlay.getGesture();
      if (mLastGesture.getLength() < LENGTH_THRESHOLD) {
        overlay.clear(false);
      }
    }

    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
    }
  }

  public interface OnGestureFinishedListener {
    void onGestureFinished(CreateGestureView view);
  }
}

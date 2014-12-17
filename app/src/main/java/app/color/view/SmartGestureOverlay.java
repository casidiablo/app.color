package app.color.view;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import app.color.R;
import app.color.utils.GestureManager;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class SmartGestureOverlay extends RelativeLayout {
  private final OnGestureDetected mOnGestureDetected;
  private final GestureManager mGestureManager;
  @InjectView(R.id.gestures_overlay) GestureOverlayView mGestureOverlayView;

  public SmartGestureOverlay(Context context, final OnGestureDetected onGestureDetected) {
    super(context);
    mOnGestureDetected = onGestureDetected;
    inflate(context, R.layout.smart_gesture_overlay, this);
    ButterKnife.inject(this);

    mGestureOverlayView.addOnGestureListener(new GesturesProcessor());
    mGestureManager = GestureManager.getInstance(context);
  }

  @OnClick(R.id.admin_gestures) void onAdminGestures() {
    mOnGestureDetected.onConfigGestures();
  }

  public interface OnGestureDetected {
    void onNothingDetected();

    void onAppDetected(String id);

    void onConfigGestures();
  }

  private class GesturesProcessor implements GestureOverlayView.OnGestureListener {

    @Override
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {

    }

    @Override
    public void onGesture(GestureOverlayView overlay, MotionEvent event) {

    }

    @Override
    public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
      Gesture gesture = mGestureOverlayView.getGesture();
      if (gesture.getLength() < CreateGestureView.LENGTH_THRESHOLD) {
        overlay.clear(false);
        return;
      }
      ArrayList<Prediction> recognize = mGestureManager.recognize(gesture);
      if (recognize != null && !recognize.isEmpty()) {
        Prediction bestMatch = recognize.get(0);
        if (bestMatch.score > CreateGestureView.GOOD_ENOUGH_SCORE) {
          mOnGestureDetected.onAppDetected(bestMatch.name);
          return;
        }
      }

      mOnGestureDetected.onNothingDetected();
    }

    @Override
    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {

    }
  }
}

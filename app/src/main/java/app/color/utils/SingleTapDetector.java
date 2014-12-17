package app.color.utils;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class SingleTapDetector {
  /**
   * The listener that is used to notify when gestures occur.
   * If you want to listen for all the different gestures then implement
   * this interface.
   */
  public interface OnSingleTapListener {

    /**
     * Notified when a tap occurs with the up {@link MotionEvent}
     * that triggered it.
     *
     * @param e The up motion event that completed the first tap
     * @return true if the event is consumed, else false
     */
    boolean onSingleTapUp(MotionEvent e);
  }

  private int mTouchSlopSquare;

  private final OnSingleTapListener mListener;

  private boolean mAlwaysInTapRegion;

  private MotionEvent mCurrentDownEvent;

  private float mLastFocusX;
  private float mLastFocusY;
  private float mDownFocusX;
  private float mDownFocusY;

  /**
   * Creates a SingleTapDetector with the supplied listener.
   * You may only use this constructor from a UI thread (this is the usual situation).
   *
   * @param context  the application's context
   * @param listener the listener invoked for all the callbacks, this must
   *                 not be null.
   * @throws NullPointerException if {@code listener} is null.
   * @see android.os.Handler#Handler()
   */
  public SingleTapDetector(Context context, OnSingleTapListener listener) {
    mListener = listener;
    init(context);
  }

  private void init(Context context) {
    if (mListener == null) {
      throw new NullPointerException("OnSingleTapListener must not be null");
    }

    // Fallback to support pre-donuts releases
    int touchSlop;
    if (context == null) {
      //noinspection deprecation
      touchSlop = ViewConfiguration.getTouchSlop();
    } else {
      final ViewConfiguration configuration = ViewConfiguration.get(context);
      touchSlop = configuration.getScaledTouchSlop();
    }
    mTouchSlopSquare = touchSlop * touchSlop;
  }

  /**
   * Analyzes the given motion event and if applicable triggers the
   * appropriate callbacks on the {@link SingleTapDetector.OnSingleTapListener} supplied.
   *
   * @param ev The current motion event.
   * @return true if the {@link SingleTapDetector.OnSingleTapListener} consumed the event,
   * else false.
   */
  public boolean onTouchEvent(MotionEvent ev) {
    final int action = ev.getAction();

    final boolean pointerUp =
        (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP;
    final int skipIndex = pointerUp ? ev.getActionIndex() : -1;

    // Determine focal point
    float sumX = 0, sumY = 0;
    final int count = ev.getPointerCount();
    for (int i = 0; i < count; i++) {
      if (skipIndex == i) continue;
      sumX += ev.getX(i);
      sumY += ev.getY(i);
    }
    final int div = pointerUp ? count - 1 : count;
    final float focusX = sumX / div;
    final float focusY = sumY / div;

    boolean handled = false;

    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_POINTER_DOWN:
        mDownFocusX = mLastFocusX = focusX;
        mDownFocusY = mLastFocusY = focusY;
        // Cancel long press and taps
        cancelTaps();
        break;

      case MotionEvent.ACTION_POINTER_UP:
        mDownFocusX = mLastFocusX = focusX;
        mDownFocusY = mLastFocusY = focusY;
        break;

      case MotionEvent.ACTION_DOWN:
        mDownFocusX = mLastFocusX = focusX;
        mDownFocusY = mLastFocusY = focusY;
        if (mCurrentDownEvent != null) {
          mCurrentDownEvent.recycle();
        }
        mCurrentDownEvent = MotionEvent.obtain(ev);
        mAlwaysInTapRegion = true;
        break;

      case MotionEvent.ACTION_MOVE:
        final float scrollX = mLastFocusX - focusX;
        final float scrollY = mLastFocusY - focusY;
        if (mAlwaysInTapRegion) {
          final int deltaX = (int) (focusX - mDownFocusX);
          final int deltaY = (int) (focusY - mDownFocusY);
          int distance = (deltaX * deltaX) + (deltaY * deltaY);
          if (distance > mTouchSlopSquare) {
            mLastFocusX = focusX;
            mLastFocusY = focusY;
            mAlwaysInTapRegion = false;
          }
        } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
          mLastFocusX = focusX;
          mLastFocusY = focusY;
        }
        break;

      case MotionEvent.ACTION_UP:
        if (mAlwaysInTapRegion) {
          handled = mListener.onSingleTapUp(ev);
        }
        break;

      case MotionEvent.ACTION_CANCEL:
        cancel();
        break;
    }

    return handled;
  }

  private void cancel() {
    mAlwaysInTapRegion = false;
  }

  private void cancelTaps() {
    mAlwaysInTapRegion = false;
  }
}

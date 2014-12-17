package app.color.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

import app.color.R;
import app.color.glue.AnimatorListenerAdapter;

public class FingerView extends View {

  private static final float INVALID_POSITION = Float.MIN_VALUE;
  public static final int SWIPE_DURATION = 1000;
  private static final long TOUCH_DURATION = 300;
  private final Bitmap mFinger;
  private final GestureDetector mGestureDetector;
  private float mFingerX = INVALID_POSITION;
  private float mFingerY = INVALID_POSITION;
  private float mSizeScale = 1.f;
  private final Matrix mScaleMatrix = new Matrix();
  private boolean mTouchinRight;
  private ObjectAnimator mSwipeAnimation;
  private AnimatorSet mTouchAnimation;
  private SwipeListener mSwipeListener;
  private int[] mTouchBoundaries;

  @SuppressWarnings("UnusedDeclaration")
  public FingerView(Context context) {
    this(context, null);
  }

  public FingerView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FingerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    mFinger = BitmapFactory.decodeResource(getResources(), R.drawable.finger);
    GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
        if (mSwipeListener != null) {
          mSwipeListener.onSwipe(motionEvent.getX() > motionEvent2.getX());
        }
        return true;
      }
    };
    mGestureDetector = new GestureDetector(context, gestureListener);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mFingerX != INVALID_POSITION && mFingerY != INVALID_POSITION) {
      canvas.save();
      canvas.translate(mFingerX, mFingerY);
      if (mTouchinRight) {
        canvas.rotate(270);
      }
      mScaleMatrix.setScale(mSizeScale, mSizeScale);
      canvas.drawBitmap(mFinger, mScaleMatrix, null);
      canvas.restore();
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mTouchBoundaries == null) {
      return false;
    }

    boolean isInsideBoundaries = event.getX() > mTouchBoundaries[0] && event.getY() > mTouchBoundaries[1];
    if (isInsideBoundaries) {
      mGestureDetector.onTouchEvent(event);
      return true;
    }
    return false;
  }

  public void swipeToLeft(int yOffset, final OnSwipeListener onSwipeListener) {
    mFingerY = yOffset;

    if (mSwipeAnimation != null) {
      mSwipeAnimation.cancel();
    }

    mSwipeAnimation = ObjectAnimator.ofFloat(this, "FingerX",
        getWidth(), -mFinger.getWidth());
    mSwipeAnimation.setDuration(SWIPE_DURATION);
    mSwipeAnimation.setInterpolator(new AccelerateInterpolator());
    mSwipeAnimation.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animator) {
        super.onAnimationEnd(animator);
        onSwipeListener.onSwipeComplete();
      }
    });
    mSwipeAnimation.start();
  }

  public void path(List<Pair<Float, Float>> toTouch, final TouchListener touchListener, boolean drag) {
    float fromX = getWidth() * 1.2f;
    float fromY = toTouch.get(0).second;

    if (mTouchAnimation != null) {
      mTouchAnimation.cancel();
    }

    mTouchAnimation = new AnimatorSet();

    int touchsSize = toTouch.size();
    List<Animator> animators = new ArrayList<Animator>();


    for (int i = 0; i < touchsSize; i++) {
      // translate animation
      AnimatorSet internalAnimator = new AnimatorSet();
      Pair<Float, Float> coords = toTouch.get(i);
      Float toX = coords.first;
      ObjectAnimator fingerX = ObjectAnimator.ofFloat(this, "FingerX", fromX, toX);
      int duration = drag ? SWIPE_DURATION / 2 : SWIPE_DURATION;
      fingerX.setDuration(duration);

      Float toY = coords.second;
      ObjectAnimator fingerY = ObjectAnimator.ofFloat(this, "FingerY", fromY, toY);
      fingerY.setDuration(duration);

      internalAnimator.playTogether(fingerX, fingerY);

      animators.add(internalAnimator);

      final int position = i;
      if (position == 0 && drag) {
        //touch down
        Animator touchDown = getTouchDown();
        touchDown.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animator) {
            touchListener.onTouched(position);
          }
        });
        animators.add(touchDown);
      } else if (drag) {
        internalAnimator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animator) {
            touchListener.onTouched(position);
          }
        });
      }

      // touch animation
      if (!drag) {
        Animator touchAnimator = getTouchAnimator();
        touchAnimator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animator) {
            touchListener.onTouched(position);
          }
        });
        animators.add(touchAnimator);
      }

      fromX = toX;
      fromY = toY;
    }

    if (drag) {
      //touch up
      Animator touchDown = getTouchUp();
      animators.add(touchDown);
    }

    // exit
    ObjectAnimator exitAnimator = ObjectAnimator.ofFloat(this, "FingerX", fromX, getWidth());
    exitAnimator.setDuration(SWIPE_DURATION);
    exitAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animator) {
        clear();
      }
    });
    animators.add(exitAnimator);

    mTouchAnimation.playSequentially(animators);
    mTouchAnimation.start();
  }

  private void clear() {
    mTouchinRight = false;
    mFingerX = INVALID_POSITION;
    mFingerY = INVALID_POSITION;
    mSizeScale = 1.f;
  }

  Animator getTouchAnimator() {
    ObjectAnimator touchAnimation = ObjectAnimator.ofFloat(this, "SizeScale", 1.f, 0.9f, 1.f);
    touchAnimation.setDuration(TOUCH_DURATION);
    return touchAnimation;
  }

  Animator getTouchDown() {
    ObjectAnimator touchAnimation = ObjectAnimator.ofFloat(this, "SizeScale", 1.f, 0.9f);
    touchAnimation.setDuration(TOUCH_DURATION);
    return touchAnimation;
  }

  Animator getTouchUp() {
    ObjectAnimator touchAnimation = ObjectAnimator.ofFloat(this, "SizeScale", 0.9f, 1.f);
    touchAnimation.setDuration(TOUCH_DURATION);
    return touchAnimation;
  }

  public void cancelAllAnimations() {
    if (mSwipeAnimation != null) {
      mSwipeAnimation.cancel();
      mSwipeAnimation = null;
    }

    if (mTouchAnimation != null) {
      mTouchAnimation.cancel();
      mTouchAnimation = null;
    }

    clear();
  }

  public void setSwipeListener(SwipeListener swipeListener) {
    mSwipeListener = swipeListener;
  }

  public void setTouchBoundaries(int[] location) {
    mTouchBoundaries = location;
  }

  public interface OnSwipeListener {
    void onSwipeComplete();
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setFingerX(float x) {
    mFingerX = x;
    postInvalidateOnAnimation();
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setSizeScale(float scale) {
    mSizeScale = scale;
    postInvalidateOnAnimation();
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setFingerY(float y) {
    mFingerY = y;
    postInvalidateOnAnimation();
  }

  public interface TouchListener {
    void onTouched(int position);
  }

  public interface SwipeListener {
    void onSwipe(boolean toLeft);
  }

}

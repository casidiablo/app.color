package app.color.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import app.color.utils.Consolator;
import app.color.R;
import app.color.model.UserPrefs;
import app.color.utils.ColorHelper;

public class BubbleView extends View {

  static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint CLEAR_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint ARC_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint HANDLE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

  static final long RESTORE_DURATION = 500;
  static final float TARGET_FACTOR = 0.5f;

  static {
    PAINT.setColor(ColorHelper.getFlatColor(ColorHelper.WHITE_INDEX));
    CLEAR_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    ARC_PAINT.setStyle(Paint.Style.STROKE);
  }

  static final int MAX_ALPHA = 0xaa;
  static final RectF RECT = new RectF();

  OnBubbleChangeListener mBubbleListener;

  Bitmap mBubbleBitmap;
  Canvas mBubbleCanvas;
  float mTopBubble;
  float mCenterX;
  int mLastWidth = -1;
  boolean mWidthGrew;

  final float mFireTarget;
  boolean mDisable;

  final float mRadius;
  final float mInnerRadius;
  final float mRadiusClear;
  final float mCirclesCenterDistance;
  int mGravity;
  final RectF mRectHandle = new RectF();
  UserPrefs mUserPrefs;

  boolean mDisableHandle = false;

  public BubbleView(Context context, int position) {
    super(context);
    mGravity = position;
    Resources res = getResources();
    DisplayMetrics displayMetrics = res.getDisplayMetrics();
    mFireTarget = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) * TARGET_FACTOR;


    float progressStroke = res.getDimension(R.dimen.progress_stroke_width);
    float progressInnerPadding = res.getDimension(R.dimen.progress_inner_padding);
    mRadius = res.getDimension(R.dimen.progress_radius);
    mInnerRadius = mRadius - progressInnerPadding - progressStroke / 2;
    mRadiusClear = mRadius * 1.5f;
    mCirclesCenterDistance = mRadius + mRadiusClear;

    clear();

    ARC_PAINT.setStrokeWidth(progressStroke);
    HANDLE_PAINT.setColor(res.getColor(R.color.handle_color));
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (mLastWidth != -1) {
      mWidthGrew = mLastWidth < w;
    }
    mLastWidth = w;
    clear();
    mBubbleBitmap = Bitmap.createBitmap(w, (int) mRadius * 2, Bitmap.Config.ARGB_8888);
    mBubbleCanvas = new Canvas(mBubbleBitmap);
    float heightFactor = mUserPrefs.activeAreaHeight / 100f;
    float activeAreaHeight = h * heightFactor;
    switch (mUserPrefs.handleGravity) {
      case Gravity.TOP:
        mTopBubble = Math.max(0, activeAreaHeight / 2 - mBubbleBitmap.getHeight() / 2);
        break;
      case Gravity.CENTER_VERTICAL:
        mTopBubble = h / 2 - mBubbleBitmap.getHeight() / 2;
        break;
      case Gravity.BOTTOM:
        mTopBubble = Math.min(h, h - activeAreaHeight / 2 - mBubbleBitmap.getHeight() / 2);
        break;
    }
    mRectHandle.set(0, 0, w, mRadius * 1.5f);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (mWidthGrew) {
      drawBubble();
      canvas.drawBitmap(mBubbleBitmap, 0, mTopBubble, null);
    } else if (!mDisableHandle && mUserPrefs != null && mUserPrefs.showActiveArea) {
      float dx = canvas.getWidth() / 3f;
      float dy = canvas.getHeight() / 2 - mRectHandle.height() / 2;
      canvas.save();
      if (isLeft()) {
        canvas.translate(-dx, dy);
      } else {
        canvas.translate(dx, dy);
      }
      canvas.drawRoundRect(mRectHandle, mRectHandle.height() / 2,
          mRectHandle.height() / 2, HANDLE_PAINT);
      canvas.restore();
    }
  }

  // TODO test multitouch
  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (mDisable) {
      return super.dispatchTouchEvent(event);
    }

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (mUserPrefs != null && mUserPrefs.hapticFeedback) {
          Consolator.get(getContext()).vibrate();
        }
        clear();
        if (mBubbleListener != null) {
          mBubbleListener.onStartTouching();
        }
        break;
      case MotionEvent.ACTION_MOVE:
        updateCenterX(event.getRawX());
        break;
      case MotionEvent.ACTION_UP:
        if (mBubbleListener != null && (isLeft() ? mCenterX > mFireTarget : mCenterX < mFireTarget)) {
          mDisable = true;
          mBubbleListener.onBubbleComplete();
          clear();
          return true;
        }

        RestoreAnimation animation = new RestoreAnimation(mCenterX);
        animation.setInterpolator(new AccelerateInterpolator());
        animation.setDuration(RESTORE_DURATION);
        animation.setAnimationListener(mAnimationListener);
        startAnimation(animation);
        break;
    }
    return true;
  }

  void drawBubble() {
    mBubbleCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

    float centerX = realX(mCenterX);
    float cx = Math.min(mFireTarget, centerX);
    float cy = mBubbleBitmap.getHeight() / 2;
    mBubbleCanvas.drawCircle(realX(cx), cy, mRadius, PAINT);

    float cx2 = (cx - mRadius) / 2;
    double angle = Math.asin((cx - cx2) / mCirclesCenterDistance);

    float relativeCY = (int) (Math.cos(angle) * mCirclesCenterDistance);
    float cy2 = cy + relativeCY;
    if (cy2 - mRadiusClear > cy) {
      float cy3 = cy - relativeCY;
      float cx3 = (cx - mRadius) / 2;

      float innerRectDy = (float) (Math.cos(angle) * mRadius);
      RECT.left = 0;
      RECT.top = cy - innerRectDy;
      RECT.right = cx;
      RECT.bottom = cy + innerRectDy;

      fixRect(RECT);

      mBubbleCanvas.drawRect(RECT, PAINT);
      mBubbleCanvas.drawCircle(realX(cx2), cy2, mRadiusClear, CLEAR_PAINT);
      mBubbleCanvas.drawCircle(realX(cx3), cy3, mRadiusClear, CLEAR_PAINT);
    }

    if (centerX >= 0) {
      RECT.left = cx - mInnerRadius;
      RECT.top = cy - mInnerRadius;
      RECT.right = RECT.left + mInnerRadius * 2;
      RECT.bottom = RECT.top + mInnerRadius * 2;

      fixRect(RECT);

      float completeSweep = Math.min(360, 360 * centerX / mFireTarget);
      float startAngle = (isLeft() ? 360 : 180) - completeSweep;

      float colors = ColorHelper.PALETTE_SIZE - 1;// ignore white
      float sweepPerColor = 360 / colors;
      for (int i = 0; i < colors; i++) {
        int colorIndex = i;
        if (i == ColorHelper.WHITE_INDEX) {
          colorIndex = ColorHelper.BLACK_INDEX;
        }

        float internalStartAngle = startAngle + i * sweepPerColor;
        float relativeSweep = sweepPerColor * (i + 1);
        float partialSweep = sweepPerColor;
        if (relativeSweep > completeSweep) {
          partialSweep = completeSweep % sweepPerColor;
        }

        ARC_PAINT.setColor(ColorHelper.getFlatColor(colorIndex));
        mBubbleCanvas.drawArc(RECT, internalStartAngle, partialSweep, false, ARC_PAINT);

        if (relativeSweep > completeSweep) {
          break;
        }
      }
    }
  }

  private void fixRect(RectF rect) {
    if (!isLeft()) {
      float left = rect.left;
      float right = rect.right;
      rect.left = realX(right);
      rect.right = realX(left);
    }
  }

  // inverts X to make it depend on orientation
  private float realX(float x) {
    return isLeft() ? x : mLastWidth - x;
  }

  private boolean isLeft() {
    return mGravity == Gravity.LEFT;
  }

  void clear() {
    mCenterX = realX(-mRadius);
  }

  void updateCenterX(float srcX) {
    srcX = realX(srcX);

    float minX = -mRadius;
    float range = mFireTarget - minX;

    float relativePosition = srcX * range / mFireTarget;
    mCenterX = realX((int) (minX + relativePosition));

    if (mBubbleListener != null) {
      int currentAlpha = Math.max(0, Math.min((int) (srcX * MAX_ALPHA / mFireTarget), MAX_ALPHA));
      mBubbleListener.onBubbleChange(currentAlpha);
    }

    invalidate(0, (int) mTopBubble, mBubbleBitmap.getWidth(), (int) mTopBubble + mBubbleBitmap.getHeight());
  }

  public void setOnBubbleChange(OnBubbleChangeListener onBubbleChangeListener) {
    mBubbleListener = onBubbleChangeListener;
  }

  final Animation.AnimationListener mAnimationListener = new Animation.AnimationListener() {
    @Override
    public void onAnimationStart(Animation animation) {
      mDisableHandle = true;
      mDisable = true;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
      clear();
      if (mBubbleListener != null) {
        mBubbleListener.onCancel();
      }
      postDelayed(new Runnable() {
        @Override
        public void run() {
          mDisableHandle = false;
          invalidate();
        }
      }, 100);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
  };

  public void enable() {
    mDisable = false;
  }

  public void updateUserPrefs(UserPrefs userPrefs) {
    mUserPrefs = userPrefs;
    invalidate();
  }

  public interface OnBubbleChangeListener {
    void onBubbleChange(int newTransparency);

    void onBubbleComplete();

    void onStartTouching();

    void onCancel();
  }

  class RestoreAnimation extends Animation {
    final float mFromX;
    final float mLength;

    RestoreAnimation(float fromX) {
      mFromX = fromX;
      mLength = realX(fromX) + mRadius;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
      super.applyTransformation(interpolatedTime, t);
      float dx = mLength * interpolatedTime;
      if (!isLeft()) {
        dx *= -1;
      }
      float currentPosition = mFromX - dx;
      updateCenterX(currentPosition);
    }
  }

  public void updateGravity(int gravity) {
    mGravity = gravity;
    invalidate();
  }
}

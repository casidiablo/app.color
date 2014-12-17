package app.color.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import app.color.R;

public class CellPhoneView extends View {
  static final float PHONE_RATIO = .51369863f;
  static final float HARD_BUTTON_FACTOR = 0.12f;
  static final float SPEAKER_RADIUS_FACTOR = 0.06f;
  static final float SCREEN_RADIUS_FACTOR = 0.03f;
  final Paint mPaint;
  final Paint mStrokePaint;
  final int mPhoneBg;
  final int mPhoneScreen;
  final int mHardButtonColor;
  final int mSpeakerColor;
  final int mCameraColor, mCamera2Color, mCamera3Color;
  final int mSensorColor;
  final float mScreenSidePadding;
  final RectF mPhoneRect = new RectF();
  final RectF mScreenRect = new RectF();
  final RectF mHardButton = new RectF();
  final RectF mSpeaker = new RectF();
  private final List<ScreenLayer> mScreenLayers = new ArrayList<ScreenLayer>();

  float mSensorsPadding;
  float mSensorRadius;

  float mCameraRadius, mCamera2Radius, mCamera3Radius;

  @SuppressWarnings("UnusedDeclaration")
  public CellPhoneView(Context context) {
    this(context, null);
  }

  public CellPhoneView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CellPhoneView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mStrokePaint.setStyle(Paint.Style.STROKE);

    Resources res = getResources();

    mPhoneBg = res.getColor(R.color.phone_bg);
    mPhoneScreen = res.getColor(R.color.phone_screen);
    mHardButtonColor = res.getColor(R.color.phone_hard_button);
    mSpeakerColor = res.getColor(R.color.speaker);
    mSensorColor = res.getColor(R.color.sensors);
    mCameraColor = res.getColor(R.color.camera);
    mCamera2Color = res.getColor(R.color.camera2);
    mCamera3Color = res.getColor(R.color.camera3);

    mScreenSidePadding = res.getDimension(R.dimen.screen_side_padding);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int requestedWidth = MeasureSpec.getSize(widthMeasureSpec);
    int requestedHeight = MeasureSpec.getSize(heightMeasureSpec);

    int width;
    int height;
    if (requestedHeight == 0 || requestedWidth / requestedHeight > PHONE_RATIO) {
      width = requestedWidth;
      height = (int) (width / PHONE_RATIO);
    } else {
      height = requestedHeight;
      width = (int) (height * PHONE_RATIO);
    }

    setMeasuredDimension(width, height);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    float width = Integer.valueOf(w).floatValue();
    float height = Integer.valueOf(h).floatValue();
    float viewRatio = width / height;
    if (viewRatio > PHONE_RATIO) {
      float cellWidth = height * PHONE_RATIO;
      float left = width / 2 - cellWidth / 2;
      mPhoneRect.set(left, 0, left + cellWidth, h);
    } else {
      float cellHeight = width / PHONE_RATIO;
      float top = height / 2 - cellHeight / 2;
      mPhoneRect.set(0, top, width, top + cellHeight);
    }

    float topPadding = mPhoneRect.height() / 12;
    mScreenRect.set(mPhoneRect.left + mScreenSidePadding,
        mPhoneRect.top + topPadding, mPhoneRect.right - mScreenSidePadding,
        mPhoneRect.bottom - topPadding);

    float buttonWidth = mPhoneRect.width() / 4;
    float bottomSpace = mPhoneRect.bottom - mScreenRect.bottom;
    float buttonHeight = bottomSpace / 3;
    float buttonLeft = mPhoneRect.centerX() - buttonWidth / 2;
    float buttonTop = mPhoneRect.bottom - bottomSpace / 2 - buttonHeight / 2;
    mHardButton.set(buttonLeft, buttonTop,
        buttonLeft + buttonWidth, buttonTop + buttonHeight);

    float speakerWidth = mPhoneRect.width() / 4.5f;
    float topSpace = mScreenRect.top;
    float speakerHeight = topSpace / 6;
    float speakerLeft = mPhoneRect.centerX() - speakerWidth / 2;
    float speakerTop = topSpace / 2 - speakerHeight / 2;
    mSpeaker.set(speakerLeft, speakerTop,
        speakerLeft + speakerWidth, speakerTop + speakerHeight);

    mSensorRadius = mPhoneRect.width() / 40;
    mSensorsPadding = mSensorRadius * 1.5f;
    mCameraRadius = mPhoneRect.width() / 30;
    mCamera2Radius = mPhoneRect.width() / 40;
    mCamera3Radius = mPhoneRect.width() / 50;

    invalidateScreenLayers();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    mPaint.setColor(mPhoneBg);
    canvas.drawRoundRect(mPhoneRect, mPhoneRect.width() / 6, mPhoneRect.height() / 12, mPaint);

    mPaint.setColor(mPhoneScreen);
    float screenRadiusFactor = mHardButton.width() * SCREEN_RADIUS_FACTOR;
    canvas.drawRoundRect(mScreenRect, screenRadiusFactor, screenRadiusFactor, mPaint);

    mStrokePaint.setColor(mHardButtonColor);
    mStrokePaint.setStrokeWidth(2);
    float hardButtonRadius = mHardButton.width() * HARD_BUTTON_FACTOR;
    canvas.drawRoundRect(mHardButton, hardButtonRadius, hardButtonRadius, mStrokePaint);

    mPaint.setColor(mSpeakerColor);
    float speakerRadius = mHardButton.width() * SPEAKER_RADIUS_FACTOR;
    canvas.drawRoundRect(mSpeaker, speakerRadius, speakerRadius, mPaint);

    mPaint.setColor(mSpeakerColor);
    float cxSensor1 = mSpeaker.right + mSensorsPadding;
    canvas.drawCircle(cxSensor1, mSpeaker.centerY(), mSensorRadius, mPaint);
    canvas.drawCircle(cxSensor1 + mSensorsPadding / 2 + mSensorRadius * 2, mSpeaker.centerY(), mSensorRadius, mPaint);

    mPaint.setColor(mCameraColor);
    float cxCamera = mPhoneRect.right - mPhoneRect.width() / 6 + mCameraRadius;
    canvas.drawCircle(cxCamera, mSpeaker.bottom, mCameraRadius, mPaint);

    mPaint.setColor(mCamera2Color);
    canvas.drawCircle(cxCamera, mSpeaker.bottom, mCamera2Radius, mPaint);

    mPaint.setColor(mCamera3Color);
    canvas.drawCircle(cxCamera, mSpeaker.bottom, mCamera3Radius, mPaint);

    int screenLayersLength = mScreenLayers.size();
    if (screenLayersLength > 0) {
      canvas.clipRect(mScreenRect);

      int saveCount = canvas.save();
      canvas.translate(mScreenRect.left, mScreenRect.top);

      for (ScreenLayer screenLayer : mScreenLayers) {
        screenLayer.draw(canvas, mScreenRect.width(), mScreenRect.height());
      }

      canvas.restoreToCount(saveCount);
    }
  }

  public void addScreenLayer(ScreenLayer screenLayer) {
    mScreenLayers.add(screenLayer);
  }

  public void invalidateScreenLayers() {
    int screenLayersLength = mScreenLayers.size();
    if (screenLayersLength > 0) {
      for (ScreenLayer screenLayer : mScreenLayers) {
        screenLayer.invalidate(mScreenRect.width(), mScreenRect.height());
      }
    }
    invalidate();
  }

  public void removeScreenLayer(ScreenLayer overlayLayer) {
    mScreenLayers.remove(overlayLayer);
  }

  public float getScreenX() {
    return mScreenRect.left;
  }

  public float getScreenY() {
    return mScreenRect.top;
  }

  public interface ScreenLayer {
    void draw(Canvas canvas, float width, float height);

    void invalidate(float screenWidth, float screenHeight);
  }
}

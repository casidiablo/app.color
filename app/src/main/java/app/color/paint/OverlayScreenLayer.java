package app.color.paint;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import app.color.R;
import app.color.view.CellPhoneView;
import app.color.view.FingerView;

public class OverlayScreenLayer implements CellPhoneView.ScreenLayer {
  final int mTargetAlpha;
  final int mPlainColor;
  private final int mRed;
  private final int mGreen;
  private final int mBlue;
  private final View mParentView;
  int mCurrentAlpha;

  public OverlayScreenLayer(View parentView) {
    mParentView = parentView;
    Resources res = parentView.getResources();
    int overlayColor = res.getColor(R.color.overlay_view_bg);
    mTargetAlpha = Color.alpha(overlayColor);
    mRed = Color.red(overlayColor);
    mGreen = Color.green(overlayColor);
    mBlue = Color.blue(overlayColor);
    mPlainColor = Color.argb(0, mRed, mGreen, mBlue);
  }

  @Override
  public void draw(Canvas canvas, float width, float height) {
    canvas.drawColor(Color.argb(mCurrentAlpha, mRed, mGreen, mBlue));
  }

  @Override
  public void invalidate(float screenWidth, float screenHeight) {

  }

  public void fadeIn() {
    ObjectAnimator.ofInt(this, "CurrentAlpha", 0, mTargetAlpha)
        .setDuration(FingerView.SWIPE_DURATION)
        .start();
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setCurrentAlpha(int currentAlpha) {
    mCurrentAlpha = currentAlpha;
    mParentView.postInvalidateOnAnimation();
  }

  public void show() {
    mCurrentAlpha = mTargetAlpha;
    mParentView.postInvalidateOnAnimation();
  }
}

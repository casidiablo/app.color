package app.color.paint;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import app.color.R;
import app.color.view.CellPhoneView;

public class BackgroundScreenLayer implements CellPhoneView.ScreenLayer {
  static final float ACTION_BAR_FACTOR = .085f;
  static final float ICON_FACTOR = .7f;

  final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  final Paint mTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  final RectF mActionBar = new RectF();
  final RectF mIcon = new RectF();
  final RectF mImage = new RectF();
  final RectF mOverflow = new RectF();
  final RectF mLabel = new RectF();
  final RectF mSubLabel = new RectF();
  final Path mTitlePath = new Path();
  final int mActionBarColor;
  final int mItemColor;
  final int mSubLabelColor;

  public BackgroundScreenLayer(Context context) {
    Resources res = context.getResources();
    mActionBarColor = res.getColor(R.color.cartoon_action_bar);
    mItemColor = res.getColor(R.color.cartoon_items);
    mSubLabelColor = res.getColor(R.color.cartoon_sublabel);

    mTitlePaint.setColor(mItemColor);
    mTitlePaint.setStyle(Paint.Style.STROKE);
    mTitlePaint.setStrokeWidth(res.getDimension(R.dimen.check_mark_stroke) / 2);
    mTitlePaint.setStrokeCap(Paint.Cap.ROUND);
    mTitlePaint.setStrokeJoin(Paint.Join.ROUND);
  }

  @Override
  public void draw(Canvas canvas, float width, float height) {
    // draw action bar
    mPaint.setColor(mActionBarColor);
    canvas.drawRect(mActionBar, mPaint);

    // draw icon
    mPaint.setColor(mItemColor);
    canvas.drawRect(mIcon, mPaint);

    // draw title
    canvas.drawPath(mTitlePath, mTitlePaint);

    // draw overflow
    int overflowCount = canvas.save();

    mPaint.setColor(mItemColor);

    float overflowHeight = mOverflow.height();
    int overflowSpacing = (int) ((mActionBar.height() - overflowHeight * 3) / 4);

    float overflowMargin = overflowHeight / 2;
    canvas.translate(0, overflowSpacing + overflowHeight);
    for (int i = 0; i < 3; i++) {
      canvas.drawRect(mOverflow, mPaint);
      canvas.translate(0, overflowMargin + overflowHeight);
    }
    canvas.restoreToCount(overflowCount);

    // draw images
    int imagesCount = canvas.save();
    float radius = mImage.height() / 5;
    for (int i = 0; i < 10; i++) {
      mPaint.setColor(mItemColor);
      canvas.drawRoundRect(mImage, radius, radius, mPaint);
      canvas.drawRect(mLabel, mPaint);

      mPaint.setColor(mSubLabelColor);
      canvas.drawRect(mSubLabel, mPaint);
      canvas.translate(0, mImage.left + mImage.height());
    }

    canvas.restoreToCount(imagesCount);
  }

  @Override
  public void invalidate(float screenWidth, float screenHeight) {
    mActionBar.set(0, 0, screenWidth, screenHeight * ACTION_BAR_FACTOR);

    float actionBarHeight = mActionBar.height();
    float iconSize = actionBarHeight * ICON_FACTOR;
    float iconPadding = (actionBarHeight - iconSize) / 2;
    mIcon.set(iconPadding, iconPadding, iconPadding + iconSize, iconPadding + iconSize);

    float pathInitialY = mIcon.bottom - iconPadding;
    mTitlePath.moveTo(mIcon.right + iconPadding, pathInitialY);
    float writtingSize = iconSize / 3;
    float x = mIcon.right + iconPadding + writtingSize;
    for (int i = 0; i < 10; i++, x += writtingSize) {
      float newY = i % 2 == 0 ? pathInitialY - writtingSize : pathInitialY;
      mTitlePath.lineTo(x, newY);
    }

    float overflowSize = actionBarHeight / 8;
    float overflowLeft = screenWidth - iconSize / 2 - iconPadding - overflowSize / 2;
    mOverflow.set(overflowLeft, 0, overflowLeft + overflowSize, overflowSize);

    float imagePadding = iconPadding * 2;
    float imageSize = iconSize * 2;
    float imageTop = imagePadding + actionBarHeight;
    mImage.set(imagePadding, imageTop, imagePadding + imageSize, imageTop + imageSize);


    float labelLeft = mImage.right + imagePadding;
    float labelHeight = mImage.height() / 5;
    float labelTop = imageTop + labelHeight;
    mLabel.set(labelLeft, labelTop, screenWidth - imagePadding, labelTop + labelHeight);

    float sublabelTop = mLabel.bottom + labelHeight;
    mSubLabel.set(labelLeft, sublabelTop, labelLeft + mLabel.width() / 2, sublabelTop + labelHeight);
  }
}

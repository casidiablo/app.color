package app.color.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import app.color.R;
import app.color.utils.ColorHelper;

public class ColorProgressBar extends View {

  final Paint mPaint = new Paint();
  final Paint mTransparentPaint = new Paint();
  int mProgress;
  public int mBackColor = -1;

  @SuppressWarnings("UnusedDeclaration")
  public ColorProgressBar(Context context) {
    this(context, null);
  }

  public ColorProgressBar(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ColorProgressBar(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mTransparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorProgressBar);
    boolean attributeBooleanValue = typedArray.getBoolean(R.styleable.ColorProgressBar_fixed, false);
    if (attributeBooleanValue) {
      setProgress(100);
    }
    typedArray.recycle();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mBackColor != -1) {
      canvas.drawColor(mBackColor);
    } else {
      canvas.drawColor(0, PorterDuff.Mode.CLEAR);
    }

    final int width = canvas.getWidth();
    final int height = canvas.getHeight();
    final int colorWidth = width / ColorHelper.PALETTE_SIZE;

    final int until = (int) Math.ceil(mProgress * ColorHelper.PALETTE_SIZE / 100f);
    int right = 0;
    for (int i = 0; i < until; i++) {
      int flatColor = ColorHelper.getFlatColor(i);
      if (!isEnabled()) {
        float average = Color.red(flatColor) * .21f + Color.green(flatColor) * .71f + Color.blue(flatColor) * .07f;
        flatColor = Color.rgb((int) average, (int) average, (int) average);
      }
      mPaint.setColor(flatColor);
      final int startX = i * colorWidth;
      if (i == until - 1) {
        right = mProgress * width / 100;
      } else {
        right = startX + colorWidth;
      }
      canvas.drawRect(startX, 0, right, height, mPaint);
    }

    canvas.drawRect(right, 0, width, height, mTransparentPaint);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (!enabled) {
      setProgress(100);
    }
  }

  public void setProgress(int progress) {
    if (mProgress != progress) {
      mProgress = progress;
      invalidate();
    }
  }
}

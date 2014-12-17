package app.color.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import app.color.utils.ColorHelper;

import java.util.HashSet;
import java.util.Set;

public class ColorIndicatorView extends View {

  final Paint mPaint = new Paint();
  final Paint mTransparentPaint = new Paint();
  private final Set<Integer> mColors = new HashSet<Integer>();

  public ColorIndicatorView(Context context) {
    this(context, null);
  }

  public ColorIndicatorView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ColorIndicatorView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mTransparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mColors.isEmpty()) {
      return;
    }

    final int width = canvas.getWidth();
    final int height = canvas.getHeight();
    final int colorWidth = width / mColors.size();

    int right = 0;
    int i = 0;
    for (Integer color : mColors) {
      int flatColor = ColorHelper.getFlatColor(color);
      if (!isEnabled()) {
        float average = Color.red(flatColor) * .21f + Color.green(flatColor) * .71f + Color.blue(flatColor) * .07f;
        flatColor = Color.rgb((int) average, (int) average, (int) average);
      }
      mPaint.setColor(flatColor);
      final int startX = i * colorWidth;
      right = startX + colorWidth;
      canvas.drawRect(startX, 0, right, height, mPaint);
      i++;
    }

    canvas.drawRect(right, 0, width, height, mTransparentPaint);
  }

  void updateColors(Set<Integer> colors) {
    mColors.clear();
    mColors.addAll(colors);
    invalidate();
  }
}

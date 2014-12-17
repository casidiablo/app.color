package app.color.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import app.color.R;

public class MaxWidthLinearLayout extends FrameLayout {

  private final int mMaxWidth;

  public MaxWidthLinearLayout(Context paramContext) {
    this(paramContext, null);
  }

  public MaxWidthLinearLayout(Context paramContext, AttributeSet paramAttributeSet) {
    super(paramContext, paramAttributeSet);
    TypedArray a = paramContext.obtainStyledAttributes(paramAttributeSet, R.styleable.MaxWidthLinearLayout);
    mMaxWidth = a.getDimensionPixelSize(R.styleable.MaxWidthLinearLayout_maxWidthView, Integer.MIN_VALUE);
    a.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
    if (mMaxWidth > 0 && mMaxWidth < widthSize) {
      int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
      widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(mMaxWidth, widthMode);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}

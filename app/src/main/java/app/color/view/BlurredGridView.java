package app.color.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.GridView;

import app.color.R;
import app.color.utils.Blur;

public class BlurredGridView extends GridView {


  public static final Rect BLURRED_RECT = new Rect();
  public static final Rect NORMAL_RECT = new Rect();
  public static final Paint PAINT = new Paint();
  public static final Matrix HALF_MATRIX = new Matrix();

  static {
    PAINT.setColor(Color.MAGENTA);
    HALF_MATRIX.setScale(0.25f, 0.25f);
  }

  private Bitmap mBitmap;
  private Canvas mCanvas;
  private boolean mPortrait = true;

  public BlurredGridView(Context context) {
    this(context, null);
  }

  public BlurredGridView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BlurredGridView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mPortrait = getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
    BLURRED_RECT.left = 0;
    BLURRED_RECT.right = w;
    BLURRED_RECT.top = (int) (h - getResources().getDimension(R.dimen.filter_bar_height));
    BLURRED_RECT.bottom = h;

    NORMAL_RECT.left = 0;
    NORMAL_RECT.right = w;
    NORMAL_RECT.top = 0;
    NORMAL_RECT.bottom = BLURRED_RECT.top;

    if (mBitmap != null) {
      mBitmap.recycle();
    }
    mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    mCanvas = new Canvas(mBitmap);
  }

  @Override
  public void draw(Canvas canvas) {
    if (mPortrait) {
      mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
      super.draw(mCanvas);
      canvas.drawBitmap(mBitmap, NORMAL_RECT, NORMAL_RECT, null);

      Bitmap blurredBitmap = Bitmap.createBitmap(mBitmap, 0, BLURRED_RECT.top, BLURRED_RECT.width(), BLURRED_RECT.height(), HALF_MATRIX, false);
      Blur.blur(getContext(), blurredBitmap, 5);
      canvas.drawBitmap(blurredBitmap, null, BLURRED_RECT, null);
      blurredBitmap.recycle();
    } else {
      super.draw(canvas);
    }
  }

  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mPortrait = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE;
  }
}

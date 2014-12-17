package app.color.paint;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.color.R;
import app.color.utils.AppUtils;
import app.color.view.CellPhoneView;
import app.color.view.FilterView;

public class AppColorScreenLayer implements CellPhoneView.ScreenLayer {

  public static final float FILTER_SIZE_FACTOR = .15f;
  public static final float PADDING_FACTOR = .2f;
  public static final int COLUMNS = 4;
  final List<Pair<String, Bitmap>> mIconList = new ArrayList<Pair<String, Bitmap>>();
  final Comparator<? super Pair<String, Bitmap>> mIconSorter = new Comparator<Pair<String, Bitmap>>() {
    @Override
    public int compare(Pair<String, Bitmap> pair1, Pair<String, Bitmap> pair2) {
      return pair1.first.compareToIgnoreCase(pair2.first);
    }
  };

  final TextPaint mAppLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
  final Paint mFilterOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  final int mItemTextColor;
  final float mTextHeight;

  static final int[] FILTERS = new int[]{
      R.drawable.red,
      R.drawable.orange,
      R.drawable.yellow,
      R.drawable.green,
      R.drawable.blue,
      R.drawable.purple,
      R.drawable.white,
      R.drawable.black};
  final Bitmap[] mFiltersBitmaps = new Bitmap[FILTERS.length];
  private final Resources mRes;
  private final int mCheckMarkColor;
  private final int mCheckMarkColorWhite;
  private final Context mContext;
  private float mPadding;
  private float mInitialFilterX;
  private float mInitialFilterY;
  private float mFilterSize;
  private final List<Integer> mMarked = new ArrayList<Integer>();
  final Paint mCheckMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path mCheckMark = new Path();
  private List<String> mFilter;
  private float mIconWidth;
  private float mTopFilterOverlay;
  private float mFiltersPadding;

  public AppColorScreenLayer(Context context) {
    mContext = context;
    mRes = context.getResources();
    mItemTextColor = mRes.getColor(R.color.item_text_color);

    Typeface font = AppUtils.getFont(context);

    mAppLabelPaint.setColor(mItemTextColor);
    mAppLabelPaint.setTypeface(font);
    mAppLabelPaint.setTextSize(mRes.getDimension(R.dimen.cartoon_text_size));
    mTextHeight = mAppLabelPaint.measureText("yY");

    mCheckMarkColor = mRes.getColor(R.color.face_color);
    mCheckMarkColorWhite = mRes.getColor(R.color.black_face_color);
    mCheckMarkPaint.setColor(mCheckMarkColor);
    mCheckMarkPaint.setStrokeWidth(mRes.getDimension(R.dimen.check_mark_stroke) / 2);
    mCheckMarkPaint.setStyle(Paint.Style.STROKE);
    mCheckMarkPaint.setStrokeJoin(Paint.Join.ROUND);
    mCheckMarkPaint.setStrokeCap(Paint.Cap.ROUND);

    mFilterOverlayPaint.setColor(mRes.getColor(R.color.tutorial_filter_overlay));
  }

  @Override
  public void invalidate(float screenWidth, float screenHeight) {
    float columnWidth = screenWidth / COLUMNS;
    mPadding = columnWidth * PADDING_FACTOR;
    mIconWidth = (screenWidth - mPadding * (COLUMNS + 1)) / COLUMNS;

    if (mIconList.isEmpty()) {
      addAppsIcons();
    } else if (mIconList.get(0).second.getWidth() != mIconWidth) {
      mIconList.clear();
      addAppsIcons();
    }

    mFilterSize = screenWidth * FILTER_SIZE_FACTOR;
    for (int i = 0; i < FILTERS.length; i++) {
      if (mFiltersBitmaps[i] != null && mFiltersBitmaps[i].getWidth() == mFilterSize) {
        continue;
      }
      Bitmap bitmap = BitmapFactory.decodeResource(mRes, FILTERS[i]);
      if (bitmap.getWidth() != mFilterSize) {
        final Bitmap b = bitmap;
        bitmap = Bitmap.createScaledBitmap(b, (int) mFilterSize, (int) mFilterSize * b.getHeight() / b.getWidth(), true);
        b.recycle();
      }
      mFiltersBitmaps[i] = bitmap;
    }

    mFiltersPadding = mPadding / 2;
    float filtersWith = mFilterSize * (FILTERS.length / 2) + (mFiltersPadding * (FILTERS.length / 2 - 1));
    mInitialFilterX = screenWidth / 2 - filtersWith / 2;
    mInitialFilterY = screenHeight - (mFilterSize * 2) - mFiltersPadding * 2;
    mTopFilterOverlay = mInitialFilterY - mFiltersPadding;

    mCheckMark.reset();
    mCheckMark.moveTo(mFilterSize * FilterView.MARK_FACTOR_X1, mFilterSize * FilterView.MARK_FACTOR_Y1);
    mCheckMark.lineTo(mFilterSize * FilterView.MARK_FACTOR_X2, mFilterSize * FilterView.MARK_FACTOR_Y2);
    mCheckMark.lineTo(mFilterSize * FilterView.MARK_FACTOR_X3, mFilterSize * FilterView.MARK_FACTOR_Y3);
  }

  @Override
  public void draw(Canvas canvas, float width, float height) {
    float nextX = mPadding;
    float nextY = mPadding;
    float padding = mPadding;
    int iconListSize = mIconList.size();

    for (int i = 0, internalIndex = 0; i < iconListSize; i++) {
      Pair<String, Bitmap> app = mIconList.get(i);
      String label = app.first;
      if (mFilter != null && !mFilter.contains(label)) {
        continue;
      }

      Bitmap iconBitmap = app.second;
      int iconWidth = iconBitmap.getWidth();
      float iconHeight = iconBitmap.getHeight();

      canvas.drawBitmap(iconBitmap, nextX, nextY, null);

      nextX += padding + iconWidth;
      if ((internalIndex + 1) % COLUMNS == 0) {
        nextX = mPadding;
        nextY += padding + iconHeight;
      }
      internalIndex++;
    }

    canvas.drawRect(0, mTopFilterOverlay, width, height, mFilterOverlayPaint);

    float filterX = mInitialFilterX;
    float filterY = mInitialFilterY;
    for (int i = 0; i < FILTERS.length; i++) {
      canvas.drawBitmap(mFiltersBitmaps[i], filterX, filterY, null);

      if (mMarked.contains(i)) {
        if (i == FILTERS.length - 1) {
          mCheckMarkPaint.setColor(mCheckMarkColorWhite);
        } else {
          mCheckMarkPaint.setColor(mCheckMarkColor);
        }
        canvas.save();
        canvas.translate(filterX, filterY);
        canvas.drawPath(mCheckMark, mCheckMarkPaint);
        canvas.restore();
      }

      filterX += mFilterSize + mFiltersPadding;
      if ((i + 1) % (FILTERS.length / 2) == 0) {
        filterX = mInitialFilterX;
        filterY += mFilterSize + mFiltersPadding;
      }
    }
  }

  private void addAppsIcons() {
    AssetManager assets = mContext.getAssets();
    try {
      String[] apps = assets.list("apps");
      for (String app : apps) {
        InputStream is = assets.open("apps/" + app);
        Bitmap bitmap = BitmapFactory.decodeStream(is);

        if (bitmap.getWidth() != mIconWidth) {
          final Bitmap b = bitmap;
          bitmap = Bitmap.createScaledBitmap(b, (int) mIconWidth, (int) mIconWidth, true);
          b.recycle();
        }

        mIconList.add(new Pair<String, Bitmap>(app.replace(".png", ""), bitmap));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    Collections.sort(mIconList, mIconSorter);
  }

  public Pair<Float, Float> getCoordsForColor(int i, float offsetX, float offsetY) {
    int xIndex = i % (FILTERS.length / 2);
    float coordX = mInitialFilterX + xIndex * mFilterSize + xIndex * mFiltersPadding;
    float coordY = mInitialFilterY + (i >= FILTERS.length / 2 ? mFilterSize + mFiltersPadding : 0);

    return new Pair<Float, Float>(offsetX + coordX, offsetY + coordY);
  }

  public void addMarkAt(int marked) {
    mMarked.add(marked);
  }

  public void clear() {
    mMarked.clear();
    mFilter = null;
  }

  public void setFilter(List<String> filter) {
    mFilter = filter;
  }
}

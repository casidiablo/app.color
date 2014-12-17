package app.color.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.color.R;
import app.color.glue.ToggleFiltersState;
import app.color.model.UserPrefs;
import app.color.playground.ColorElement;
import app.color.playground.GraphicElement;
import app.color.utils.ColorHelper;
import app.color.utils.Consolator;
import app.color.utils.SingleTapDetector;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;

public class FilterView extends View implements ToggleFiltersState {

  static final RectF RECT = new RectF();
  static final int MAXIMUM_FILTER_PER_APP = 4;
  static final boolean DEBUG_FILTER = false;
  public static final float MARK_FACTOR_X1 = .35f;
  public static final float MARK_FACTOR_Y1 = .6f;
  public static final float MARK_FACTOR_X2 = .5f;
  public static final float MARK_FACTOR_Y2 = .68f;
  public static final float MARK_FACTOR_Y3 = 0.38f;
  public static final float MARK_FACTOR_X3 = 0.73f;

  public static final int FILTERS_ROWS = 2;

  final int mBlackFaceColor;
  final int mFaceColor;

  final Paint mDisabledPaint = new Paint(ANTI_ALIAS_FLAG);
  final Paint mSwitchPaint = new Paint(ANTI_ALIAS_FLAG);
  final Paint mCheckMarkPaint = new Paint(ANTI_ALIAS_FLAG);
  final Paint mGrayScalePaint = new Paint(ANTI_ALIAS_FLAG);
  final int mDisabledColor;
  final Path mCheckMark = new Path();

  final ViewConfiguration mViewConfiguration;

  float mFilterSize;
  int mFilterBorderRadius;

  final List<ColorElement> mColorElements = new ArrayList<ColorElement>();
  GraphicElement[] mElementsLayout;
  final GraphicElement[] mColorsLayout;
  final android.java.util.ArrayDeque<Integer> mFilters = new android.java.util.ArrayDeque<Integer>();
  final Set<Integer> mIgnoredColors = new HashSet<Integer>();
  final ToggleFilter mExactFilter = new ToggleFilter(R.drawable.ic_exact_on, R.drawable.ic_exact_off);
  final List<ToggleFilter> mToggleElements = new ArrayList<ToggleFilter>();

  final int mFilterWidth;

  ColorElement mLastTouchedColorElement;
  SingleTapDetector mSingleTapDetector;
  float mLastTouchX, mLastTouchY;
  float mLastTouchedX = -1, mLastTouchedY = -1;
  float mLastTouchedDownX = -1, mLastTouchedDownY = -1;
  int mLastSavedIndex = -1;
  float mLastSelectedX = -1, mLastSelectedY = -1;

  OnFilterListener mOnFilterListener;

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
  int mLastPlayedColor = -1;
  UserPrefs mUserPrefs;
  boolean mSelectOnTouch = true;
  final List<Point> mDebugPoints;
  private int mFilterMargin;
  private int mPreviousConfiguration = Configuration.ORIENTATION_UNDEFINED;
  private Bitmap mDisabledColorBitmap;

  @SuppressWarnings("UnusedDeclaration")
  public FilterView(Context context) {
    this(context, null);
  }

  public FilterView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FilterView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    mExactFilter.preconditionChecker = new PreconditionChecker() {
      @Override
      public boolean preconditionValid(boolean checked) {
        return checked || (!mFilters.isEmpty() && mFilters.size() + mIgnoredColors.size() < FILTERS.length);
      }
    };


    ColorMatrix cm = new ColorMatrix();
    cm.setSaturation(0);
    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
    mGrayScalePaint.setColorFilter(f);

    mToggleElements.add(mExactFilter);

    Resources res = getResources();
    //noinspection ConstantConditions
    mFilterWidth = (int) res.getDimension(R.dimen.filter_selector_width);
    mFilterBorderRadius = (int) res.getDimension(R.dimen.filter_border_radius);
    mFilterMargin = (int) res.getDimension(R.dimen.filter_margin);
    mFaceColor = res.getColor(R.color.face_color);
    mBlackFaceColor = res.getColor(R.color.black_face_color);

    mCheckMarkPaint.setColor(mFaceColor);
    mCheckMarkPaint.setStrokeWidth(res.getDimension(R.dimen.check_mark_stroke));
    mCheckMarkPaint.setStyle(Paint.Style.STROKE);
    mCheckMarkPaint.setStrokeJoin(Paint.Join.ROUND);
    mCheckMarkPaint.setStrokeCap(Paint.Cap.ROUND);

    mSwitchPaint.setColor(res.getColor(R.color.filter_bg));

    for (int i = 0; i < ColorHelper.PALETTE_SIZE; i++) {
      mColorElements.add(new ColorElement(i));
    }

    final ColorElement red = mColorElements.get(ColorHelper.RED_INDEX);
    final ColorElement orange = mColorElements.get(ColorHelper.ORANGE_INDEX);
    final ColorElement yellow = mColorElements.get(ColorHelper.YELLOW_INDEX);
    final ColorElement green = mColorElements.get(ColorHelper.GREEN_INDEX);
    final ColorElement blue = mColorElements.get(ColorHelper.BLUE_INDEX);
    final ColorElement purple = mColorElements.get(ColorHelper.PURPLE_INDEX);
    final ColorElement white = mColorElements.get(ColorHelper.WHITE_INDEX);
    final ColorElement black = mColorElements.get(ColorHelper.BLACK_INDEX);

    mColorsLayout = new GraphicElement[]{
        red, white, black, orange,
        yellow, blue, green, purple,};

    mElementsLayout = mColorsLayout;
    enableElements(mElementsLayout, true);

    red.payload = ColorHelper.RED_INDEX;
    orange.payload = ColorHelper.ORANGE_INDEX;
    yellow.payload = ColorHelper.YELLOW_INDEX;
    green.payload = ColorHelper.GREEN_INDEX;
    blue.payload = ColorHelper.BLUE_INDEX;
    purple.payload = ColorHelper.PURPLE_INDEX;
    white.payload = ColorHelper.WHITE_INDEX;
    black.payload = ColorHelper.BLACK_INDEX;

    mDisabledColor = res.getColor(R.color.filter_rect);
    mDisabledPaint.setColor(mDisabledColor);

    mSingleTapDetector = new SingleTapDetector(context, mGestureListener);
    mViewConfiguration = ViewConfiguration.get(context);

    if (DEBUG_FILTER) {
      mDebugPoints = new ArrayList<Point>();
    } else {
      mDebugPoints = null;
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    measureElements(w, h, false);
  }

  private void measureElements(int w, int h, boolean force) {
    Resources res = getResources();
    int newFilterMargin = mFilterMargin;

    int rowSize = mElementsLayout.length / FILTERS_ROWS;
    //noinspection ConstantConditions
    boolean isPortrait = res.getConfiguration().orientation != ORIENTATION_LANDSCAPE;
    int newConfiguration = isPortrait ? ORIENTATION_PORTRAIT : ORIENTATION_LANDSCAPE;

    int containerWidth = isPortrait ? w : h;
    int containerHeight = isPortrait ? h : w;

    int maxContainerWidth = containerWidth - 2 * mFilterMargin;
    int maxContainerHeight = containerHeight - 2 * mFilterMargin;
    float maxContainerRatio = (float) maxContainerWidth / (float) maxContainerHeight;
    float filtersRatio = rowSize / FILTERS_ROWS;

    int newFilterSize;
    int initialCoordX = mFilterMargin;
    int initialCoordY = mFilterMargin;
    if (filtersRatio < maxContainerRatio) {
      newFilterSize = (maxContainerHeight - mFilterMargin * (FILTERS_ROWS - 1)) / FILTERS_ROWS;
      int deltaX = containerWidth / 2 - (newFilterSize * rowSize + mFilterMargin * (rowSize - 1)) / 2;
      if (isPortrait) {
        initialCoordX = deltaX;
      } else {
        //noinspection SuspiciousNameCombination
        initialCoordY = deltaX;
      }
    } else {
      newFilterSize = (maxContainerWidth - mFilterMargin * (rowSize - 1)) / rowSize;
      int deltaY = containerHeight / 2 - (newFilterSize * rowSize + mFilterMargin * (rowSize - 1)) / 2;
      if (isPortrait) {
        initialCoordY = deltaY;
      } else {
        //noinspection SuspiciousNameCombination
        initialCoordX = deltaY;
      }
    }

    if (!force && mFilterSize == newFilterSize && mPreviousConfiguration == newConfiguration) {
      return;
    }

    mFilterSize = newFilterSize;
    ensureDisabledColor();
    mPreviousConfiguration = newConfiguration;

    float step = newFilterMargin + mFilterSize;

    // create path
    mCheckMark.reset();
    mCheckMark.moveTo(mFilterSize * MARK_FACTOR_X1, mFilterSize * MARK_FACTOR_Y1);
    mCheckMark.lineTo(mFilterSize * MARK_FACTOR_X2, mFilterSize * MARK_FACTOR_Y2);
    mCheckMark.lineTo(mFilterSize * MARK_FACTOR_X3, mFilterSize * MARK_FACTOR_Y3);

    int coordX = initialCoordX;
    int coordY = initialCoordY;
    for (int i = 0; i < mElementsLayout.length; i++) {
      GraphicElement graphicElement = mElementsLayout[i];
      if (graphicElement != null) {
        if (i > 0 && graphicElement == mElementsLayout[i - 1]) {
          continue;
        }

        if (i + 1 < mElementsLayout.length && graphicElement == mElementsLayout[i + 1]) {
          // double size
          graphicElement.width = mFilterSize * 2;
          graphicElement.height = mFilterSize;

          graphicElement.x = coordX;
          graphicElement.y = coordY;
        } else {
          graphicElement.width = graphicElement.height = mFilterSize;
          graphicElement.x = coordX;
          graphicElement.y = coordY;
        }

        if (graphicElement.payload != Integer.MIN_VALUE) {
          final int colorIndex = graphicElement.payload;
          if (mFiltersBitmaps[colorIndex] != null) {
            mFiltersBitmaps[colorIndex].recycle();
            mFiltersBitmaps[colorIndex] = null;
          }

          Bitmap bitmap = createFilterBitmap(res, FILTERS[colorIndex]);
          mFiltersBitmaps[colorIndex] = bitmap;
        }

        if (graphicElement instanceof ToggleFilter) {
          configureToggle((ToggleFilter) graphicElement);
        }
      }

      if (isPortrait) {
        coordX += step;
        if ((mElementsLayout.length / FILTERS_ROWS) - 1 == i) {
          coordX = initialCoordX;
          coordY += mFilterMargin + mFilterSize;
        }
      } else {
        coordY += step;
        if ((mElementsLayout.length / FILTERS_ROWS) - 1 == i) {
          coordY = initialCoordY;
          coordX += mFilterMargin + mFilterSize;
        }
      }
    }

    configureToggle(mExactFilter);
    if (isPortrait) {
      mExactFilter.x = mFilterMargin;
      mExactFilter.y = h / 2 - mExactFilter.height / 2;
    } else {
      mExactFilter.x = w / 2 - mExactFilter.width / 2;
      mExactFilter.y = mFilterMargin;
    }

    invalidate();
  }

  private void configureToggle(ToggleFilter filter) {
    if (filter.mOnRes != 0) {
      filter.bitmapChecked = createFilterBitmap(getResources(), filter.mOnRes);
      filter.bitmapUnchecked = createFilterBitmap(getResources(), filter.mOffRes);
    } else {
      filter.bitmapChecked = createFilterBitmap(getResources(), filter.mOffRes);
      filter.bitmapUnchecked = toGrayScale(filter.bitmapChecked);
    }
    filter.width = filter.height = mFilterSize;
  }

  public Bitmap toGrayScale(Bitmap bmpOriginal) {
    int width = bmpOriginal.getWidth();
    int height = bmpOriginal.getHeight();

    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bmpGrayscale);
    c.drawBitmap(bmpOriginal, 0, 0, mGrayScalePaint);
    return bmpGrayscale;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    // draw colors
    if (mElementsLayout == mColorsLayout) {
      for (ColorElement colorElement : mColorElements) {
        drawColorItem(canvas, colorElement);
      }
    }

    mExactFilter.enabled = !mFilters.isEmpty() && mFilters.size() < MAXIMUM_FILTER_PER_APP;
    drawToggleItem(canvas, mExactFilter);

    if (DEBUG_FILTER) {
      for (Point point : mDebugPoints) {
        canvas.drawPoint(point.x, point.y, sDebugPaint);
      }
    }
  }

  private void drawColorItem(Canvas canvas, final ColorElement colorElement) {
    if (!colorElement.enabled) {
      return;
    }
    RECT.left = colorElement.x;
    RECT.right = RECT.left + colorElement.width;
    RECT.top = colorElement.y;
    RECT.bottom = RECT.top + colorElement.height;

    final int colorIndex = colorElement.colorIndex;
    final boolean colorIgnored = mIgnoredColors.contains(colorIndex);

    if (colorIgnored) {
      canvas.drawBitmap(mDisabledColorBitmap, RECT.left, RECT.top, null);
    } else {
      canvas.drawBitmap(mFiltersBitmaps[colorIndex], RECT.left, RECT.top, null);
    }

    if (colorIndex == ColorHelper.BLACK_INDEX) {
      mCheckMarkPaint.setColor(mBlackFaceColor);
    }

    // draw checkmark
    if (colorElement.selected) {
      canvas.save();
      canvas.translate(RECT.left, RECT.top);
      canvas.drawPath(mCheckMark, mCheckMarkPaint);
      canvas.restore();
    }

    mCheckMarkPaint.setColor(mFaceColor);
  }

  private void ensureDisabledColor() {
    if (mDisabledColorBitmap == null) {
      mDisabledColorBitmap = Bitmap.createBitmap((int) mFilterSize, (int) mFilterSize, ARGB_8888);
      final Canvas disabledColorCanvas = new Canvas(mDisabledColorBitmap);
      RECT.left = RECT.top = 0;
      RECT.right = RECT.bottom = mFilterSize;
      disabledColorCanvas.drawRoundRect(RECT, mFilterBorderRadius, mFilterBorderRadius, mDisabledPaint);
    }
  }

  private void drawToggleItem(Canvas canvas, ToggleFilter toggleFilter) {
    if (!toggleFilter.enabled || toggleFilter.bitmapUnchecked == null) {
      return;
    }
    RECT.left = toggleFilter.x;
    RECT.right = RECT.left + toggleFilter.width;
    RECT.top = toggleFilter.y;
    RECT.bottom = RECT.top + toggleFilter.height;
    if (toggleFilter.checked) {
      canvas.drawBitmap(toggleFilter.bitmapChecked, RECT.left, RECT.top, null);
    } else {
      canvas.drawBitmap(toggleFilter.bitmapUnchecked, RECT.left, RECT.top, null);
    }
  }

  @Override
  public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {
    if (DEBUG_FILTER) {
      mDebugPoints.add(new Point((int) event.getX(), (int) event.getY()));
    }
    if (mOnFilterListener == null) {
      return super.onTouchEvent(event);
    }

    if (!mSelectOnTouch) {
      mSingleTapDetector.onTouchEvent(event);
    }

    mLastTouchedX = event.getX();
    mLastTouchedY = event.getY();

    // TODO call invalidate only when necessary
    final int action = event.getAction();

    mLastTouchX = event.getX();
    mLastTouchY = event.getY();

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        ColorElement colorElement = touchedColor(event);
        if (colorElement != null) {
          onColorTest(colorElement);
          feedback(colorElement.colorIndex);

          if (mSelectOnTouch) {
            toggleColorSelection();
          }
          invalidate();
        } else if (mSelectOnTouch) {
          ToggleFilter toggleFilter = toggleElementTouched(event, false);
          if (toggleFilter != null) {
            toggleFilter.alreadyTouched = true;
            toggleFilter.toggle();
            invalidate();
          }
        }
        return true;
      case MotionEvent.ACTION_MOVE:
        mOnFilterListener.onTouchReleased();

        final ColorElement touchedColor = touchedColor(event);
        final boolean colorWasTouched = touchedColor != null;

        if (mSelectOnTouch && mFilters.size() <= 1 && colorWasTouched && touchedColor.colorIndex != mLastSavedIndex && mLastSavedIndex != -1) {
          int diff = Math.abs(mLastSavedIndex - touchedColor.colorIndex);
          if (diff > 1 && mLastSelectedX != -1 && Math.abs(mLastSelectedX - mLastTouchedX) < mFilterSize) {
            boolean topToBottom = mLastSelectedY < mLastTouchedY;
            if (topToBottom) {
              for (int i = mLastSavedIndex + 1; i < mLastSavedIndex + Math.min(MAXIMUM_FILTER_PER_APP - 1, diff); i++) {
                toggleColor(findColorByIndex(i));
              }
            } else {
              for (int i = mLastSavedIndex - 1; i > mLastSavedIndex - Math.min(MAXIMUM_FILTER_PER_APP - 1, diff); i--) {
                toggleColor(findColorByIndex(i));
              }
            }
          }
        }

        if (mSelectOnTouch && colorWasTouched) {
          mLastSavedIndex = touchedColor.colorIndex;
        }

        boolean selectedLessThanMaximum = mFilters.size() < MAXIMUM_FILTER_PER_APP;
        if (selectedLessThanMaximum) {
          onColorTest(touchedColor);
          if (colorWasTouched && touchedColor.colorIndex != mLastPlayedColor
              && !mIgnoredColors.contains(touchedColor.colorIndex)) {
            if (mSelectOnTouch) {
              toggleColorSelection();
              mLastSelectedX = mLastTouchedX;
              mLastSelectedY = mLastTouchedY;
            }
            feedback(touchedColor.colorIndex);
          }
        }

        if (!colorWasTouched) {
          ToggleFilter toggleFilter = toggleElementTouched(event, true);
          if (toggleFilter != null) {
            toggleFilter.alreadyTouched = true;
            toggleFilter.toggle();
          }
        }

        invalidate();
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        clearToggles(true, false);
        mOnFilterListener.onTouchReleased();
        mLastTouchedColorElement = null;
        mLastPlayedColor = -1;
        mLastSavedIndex = -1;
        mLastSelectedX = -1;
        mLastSelectedY = -1;

        mFilters.removeAll(getUnselectedColors());
        onFilterUpdated(true);
        invalidate();
        return true;
    }
    return super.onTouchEvent(event);
  }

  private void enableElements(GraphicElement[] elements, boolean enable) {
    for (GraphicElement element : elements) {
      if (element != null) {
        element.enabled = enable;
      }
    }
  }

  private void clearToggles(boolean clearAlreadyTouched, boolean clearCheckedState) {
    for (ToggleFilter mToggleElement : mToggleElements) {
      clearToggle(mToggleElement, clearAlreadyTouched, clearCheckedState);
    }
  }

  private void clearToggle(ToggleFilter toggleFilter, boolean clearAlreadyTouched, boolean clearCheckedState) {
    if (clearAlreadyTouched) {
      toggleFilter.alreadyTouched = false;
    }
    if (clearCheckedState) {
      toggleFilter.checked = false;
    }
  }

  void feedback(int colorIndex) {
    mLastPlayedColor = colorIndex;
    vibrate();
  }

  void vibrate() {
    if (mUserPrefs != null && mUserPrefs.hapticFeedback) {
      Consolator.get(getContext()).vibrate();
    }
  }

  void selectColor(ColorElement lastTouchedColorElement) {
    boolean notNull = lastTouchedColorElement != null;
    if (notNull && !lastTouchedColorElement.selected && !mIgnoredColors.contains(lastTouchedColorElement.colorIndex)) {
      lastTouchedColorElement.selected = true;
      onFilterUpdated(true);
      if (!mFilters.contains(lastTouchedColorElement.colorIndex)) {
        mFilters.push(lastTouchedColorElement.colorIndex);
      }
    }
  }

  void onFilterUpdated(boolean ignore) {
    Set<Integer> toIgnore = mOnFilterListener.onFilterUpdated();
    if (ignore) {
      synchronized (mIgnoredColors) {
        mIgnoredColors.clear();
        mIgnoredColors.addAll(toIgnore);
      }
    }
  }

  void onColorTest(ColorElement colorElement) {
    if (colorElement != null) {
      mLastTouchedColorElement = colorElement;

      final int currentColor = colorElement.colorIndex;
      if (!mFilters.contains(currentColor) && !mIgnoredColors.contains(currentColor)) {
        mFilters.removeAll(getUnselectedColors());
        mFilters.push(currentColor);
        onFilterUpdated(false);
      }
    }
  }

  private ColorElement touchedColor(MotionEvent event) {
    for (ColorElement colorElement : mColorElements) {
      if (colorElement.enabled && isColorTouched(colorElement, event.getX(), event.getY())) {
        return colorElement;
      }
    }
    return null;
  }

  public void setOnFilterListener(OnFilterListener onFilterListener) {
    mOnFilterListener = onFilterListener;
  }

  @Override
  public boolean dispatchKeyEvent(@SuppressWarnings("NullableProblems") KeyEvent event) {
    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      if (event.getAction() == KeyEvent.ACTION_UP) {
        mLastTouchedColorElement = null;
        boolean allTogglesAreUnchecked = allTogglesAreUnchecked();
        if (allTogglesAreUnchecked && mFilters.isEmpty()) {
          mOnFilterListener.onExitFilter();
          clear();
        } else if (!allTogglesAreUnchecked) {
          uncheckFirstCheckedToggle();
          onFilterUpdated(true);
          invalidate();
        } else {
          final int removedColor = mFilters.pop();
          unselectColor(removedColor);
          invalidate();
        }
      }
      return true;
    }
    return super.dispatchKeyEvent(event);
  }

  private void uncheckFirstCheckedToggle() {
    for (ToggleFilter mToggleElement : mToggleElements) {
      if (mToggleElement.checked) {
        mToggleElement.checked = false;
        return;
      }
    }
  }

  private boolean allTogglesAreUnchecked() {
    for (ToggleFilter mToggleElement : mToggleElements) {
      if (mToggleElement.checked) {
        return false;
      }
    }
    return true;
  }

  void clear() {
    boolean hadFilters = !mFilters.isEmpty();

    clearToggles(true, true);
    mFilters.clear();
    mIgnoredColors.clear();
    for (ColorElement mColorElement : mColorElements) {
      mColorElement.selected = false;
    }
    mLastSavedIndex = -1;
    mLastSelectedX = -1;
    mLastSelectedY = -1;
    if (DEBUG_FILTER) {
      mDebugPoints.clear();
    }
    if (hadFilters) {
      mOnFilterListener.onFilterUpdated();
    }
  }

  void clearExactFilter() {
    clearFilter(mExactFilter);
  }

  private void clearFilter(ToggleFilter filter) {
    filter.checked = false;
    filter.alreadyTouched = false;
    onFilterUpdated(true);
    invalidate();
  }

  void unselectColor(ColorElement colorElement) {
    if (colorElement != null && colorElement.selected) {
      colorElement.selected = false;
      onFilterUpdated(true);
      mFilters.remove(colorElement.colorIndex);

      if (mFilters.isEmpty()) {
        clearToggles(true, true);
      }
    }
  }

  private void unselectColor(int colorToUnselect) {
    ColorElement colorElement = findColorByIndex(colorToUnselect);
    if (colorElement != null) {
      unselectColor(colorElement);
    }
  }

  private ColorElement findColorByIndex(int colorIndex) {
    for (ColorElement colorElement : mColorElements) {
      if (colorElement.colorIndex == colorIndex) {
        return colorElement;
      }
    }
    return null;
  }

  public android.java.util.ArrayDeque<Integer> getFiltersRef() {
    return mFilters;
  }

  @Override
  public boolean exactFilterEnabled() {
    return mExactFilter.checked;
  }

  public Collection<Integer> getUnselectedColors() {
    ArrayList<Integer> unselected = new ArrayList<Integer>();
    for (ColorElement colorElement : mColorElements) {
      if (!colorElement.selected) {
        unselected.add(colorElement.colorIndex);
      }
    }
    return unselected;
  }

  public interface OnFilterListener {

    /**
     * returns color indexes to ignore *
     */
    Set<Integer> onFilterUpdated();

    void onExitFilter();

    void onTouchReleased();
  }

  public ToggleFilter toggleElementTouched(MotionEvent event, boolean ignoreIfAlreadyTouched) {
    for (ToggleFilter toggleElement : mToggleElements) {
      if ((ignoreIfAlreadyTouched && toggleElement.alreadyTouched) || !toggleElement.enabled) {
        continue;
      }
      if (elementTouched(event, toggleElement)) {
        return toggleElement;
      }
    }
    return null;
  }

  public static boolean isColorTouched(GraphicElement graphicElement, float touchX, float touchY) {
    float left = graphicElement.x;
    float right = left + graphicElement.width;
    float top = graphicElement.y;
    float bottom = graphicElement.y + graphicElement.height;

    boolean insideSquare = touchX >= left && touchX <= right && touchY >= top && touchY <= bottom;
    if (!insideSquare) {
      return false;
    }

    float middleX = right - graphicElement.width / 2;
    float middleY = top - graphicElement.width / 2;
    float tolerance = graphicElement.width / 6;

    if (touchX < middleX && touchY < middleY) {
      boolean outOfLeftTopCorner = distance(left, top, touchX, touchY) < tolerance;
      if (outOfLeftTopCorner) {
        return false;
      }
    }

    if (touchX > middleX && touchY < middleY) {
      boolean outOfRightTopCorner = distance(right, top, touchX, touchY) < tolerance;
      if (outOfRightTopCorner) {
        return false;
      }
    }

    if (touchX > middleX && touchY > middleY) {
      boolean outOfRightBottomCorner = distance(right, bottom, touchX, touchY) < tolerance;
      if (outOfRightBottomCorner) {
        return false;
      }
    }

    if (touchX < middleX && touchY > middleY) {
      boolean outOfLeftBottomCorner = distance(left, bottom, touchX, touchY) < tolerance;
      if (outOfLeftBottomCorner) {
        return false;
      }
    }

    return true;
  }

  private static float distance(float x1, float y1, float x2, float y2) {
    return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
  }

  public static boolean elementTouched(MotionEvent ev, GraphicElement graphicElement) {
    final float x = ev.getX();
    final float y = ev.getY();
    return x >= graphicElement.x && x <= graphicElement.x + graphicElement.width && y >= graphicElement.y && y <= graphicElement.y + graphicElement.height;
  }

  final SingleTapDetector.OnSingleTapListener mGestureListener = new SingleTapDetector.OnSingleTapListener() {
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      toggleColorSelection();
      return true;
    }
  };

  private void toggleColorSelection() {
    ColorElement lastTouchedColorElement = mLastTouchedColorElement;
    toggleColor(lastTouchedColorElement);
  }

  private void toggleColor(ColorElement lastTouchedColorElement) {
    if (lastTouchedColorElement != null) {
      mLastTouchedColorElement = null;

      if (lastTouchedColorElement.selected) {
        unselectColor(lastTouchedColorElement);
      } else {
        selectColor(lastTouchedColorElement);
      }
      invalidate();
    }
  }

  void updatePrefs(UserPrefs userPrefs) {
    mUserPrefs = userPrefs;
    if (mUserPrefs != null) {
      mSelectOnTouch = !mUserPrefs.tapToSelectColor;
    }
  }

  static Paint sDebugPaint;

  static {
    if (DEBUG_FILTER) {
      sDebugPaint = new Paint();
      sDebugPaint.setStyle(Paint.Style.STROKE);
      sDebugPaint.setStrokeWidth(10);
      sDebugPaint.setColor(Color.CYAN);
    }
  }

  private Bitmap createFilterBitmap(Resources res, int resId) {
    Bitmap bitmap = BitmapFactory.decodeResource(res, resId);
    if (bitmap.getWidth() != mFilterSize) {
      final Bitmap b = bitmap;
      bitmap = Bitmap.createScaledBitmap(b, (int) mFilterSize, (int) mFilterSize * b.getHeight() / b.getWidth(), true);
      b.recycle();
    }
    return bitmap;
  }

  public class ToggleFilter extends GraphicElement {
    final int mOnRes;
    final int mOffRes;
    boolean checked;
    boolean alreadyTouched;
    Bitmap bitmapChecked;
    Bitmap bitmapUnchecked;
    PreconditionChecker preconditionChecker;
    long lastTimeActivated;
    long lastTimeSubLaunched;

    public ToggleFilter(int onRes, int offRes) {
      mOnRes = onRes;
      mOffRes = offRes;
    }

    private void toggle() {
      boolean preconditionValid = preconditionChecker == null || preconditionChecker.preconditionValid(checked);
      if (preconditionValid && mSelectOnTouch) {
        checked = !checked;
        onFilterUpdated(true);
      }
      vibrate();
    }
  }

  private interface PreconditionChecker {
    boolean preconditionValid(boolean currentState);
  }
}

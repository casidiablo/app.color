package app.color.view;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.java.util.ArrayDeque;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.SqlAdapter;

import java.io.File;
import java.util.List;
import java.util.Set;

import app.color.R;
import app.color.activity.GesturesListActivity;
import app.color.adapter.AppsAdapter;
import app.color.model.AppEntry;
import app.color.model.UserPrefs;
import app.color.service.IndexApps;
import app.color.utils.AppUtils;
import app.color.utils.Consolator;
import app.color.utils.GestureManager;
import app.color.utils.Utilities;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;

import static android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM;
import static app.color.utils.ViewUtils.enable;

@SuppressWarnings("ConstantConditions")
public class PlayGroundView extends RelativeLayout {

  static final FrameLayout.LayoutParams FRAME_MATCH_LP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
  static final int LAUNCH_APP = 0;
  static final int UPDATE_APPS = 1;
  public static final int NUM_COLUMNS_LANDSCAPE = 5;
  public static final int NUM_COLUMNS_PORTRAIT = 4;
  private static final int CLEAR_STACK_BACKGROUND_COLOR = 0;
  private static final int POP_FROM_OVERLAY_STACK = 1;
  private static final int CLEAR_EVERYTHING = 2;

  @InjectView(R.id.radio_group) View mActionBarOptions;
  @InjectView(R.id.action_bar) View mActionBar;
  @InjectView(R.id.action_bar_icon) ImageView mActionBarIcon;
  @InjectView(R.id.apps_grid) GridView mAppGrid;
  @InjectView(R.id.filter_view) FilterView mFilterView;
  @InjectView(R.id.sort_by_name) RadioButton mSortByName;
  @InjectView(R.id.sort_by_usage) RadioButton mSortByUsage;
  @InjectView(R.id.search_box) EditText mSearchBox;
  @InjectView(R.id.search_btn) View mSearchBtn;
  @InjectView(R.id.empty_view) TextView mEmptyView;

  final AppsAdapter mAdapter;
  final OnExitListener mExitListener;
  final int mFilterBarHeight;
  final ArrayDeque<Integer> mFilters;
  final int mOriginalBackgroundColor;
  final FrameLayout mOverlayStack;

  long mLastTimeVisible;
  private UserPrefs mUserPrefs;

  public PlayGroundView(final Context context, OnExitListener exitListener, List<AppEntry> apps) {
    super(context);
    mExitListener = exitListener;

    LayoutInflater.from(context).inflate(R.layout.overlay_view, this, true);
    ButterKnife.inject(this);

    Resources res = getResources();
    //noinspection ConstantConditions
    mOriginalBackgroundColor = res.getColor(R.color.overlay_view_bg);
    mFilterBarHeight = (int) res.getDimension(R.dimen.filter_bar_height);

    updateBackground();
    mActionBarOptions.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // no-op
      }
    });

    enable(mSortByUsage, true);

    final CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
          switch (buttonView.getId()) {
            case R.id.sort_by_name:
              mAdapter.sortByAlpha();
              break;
            case R.id.sort_by_usage:
              mAdapter.sortByUsage();
              break;
          }
        }
      }
    };
    mSortByName.setOnCheckedChangeListener(listener);
    mSortByUsage.setOnCheckedChangeListener(listener);

    mFilters = mFilterView.getFiltersRef();
    mFilterView.setOnFilterListener(mOnFilterListener);

    mUserPrefs = Persistence.quickPref(getContext(), UserPrefs.class);
    mAdapter = new AppsAdapter(context, mFilters, apps, mTextFilter, mFilterView, mUserPrefs,
        mEmptyListener);

    setupAppList();

    OnKeyListener searchBoxOnKeyListener = new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        boolean pressedBack = keyCode == KeyEvent.KEYCODE_BACK;
        if (pressedBack && event.getAction() == KeyEvent.ACTION_UP) {
          restoreSearchBox();
        }
        return keyCode == KeyEvent.KEYCODE_BACK;
      }
    };
    mSearchBox.setOnKeyListener(searchBoxOnKeyListener);
    mSearchBox.addTextChangedListener(mSearchWatcher);

    LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
    mOverlayStack = new FrameLayout(context);
    addView(mOverlayStack, lp);
  }

  @OnClick(R.id.search_btn) void onSearchClicked() {
    mSearchBtn.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    mActionBarOptions.setVisibility(View.GONE);
    mSearchBox.setVisibility(View.VISIBLE);
    mSearchBox.requestFocus();
    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(mSearchBox, 0);
  }

  @OnLongClick(R.id.action_bar_icon) boolean onActionBarIconLongPress() {
    Object tag = mActionBarIcon.getTag();
    if (tag instanceof String) {
      String packageName = (String) tag;
      AppEntry appEntry = mAdapter.findByPackage(packageName);
      longClickedApp(appEntry);
    }
    return true;
  }

  @OnClick(R.id.action_bar_icon) void onActionBarIcon() {
    mExitListener.onExit();
  }

  @OnClick(R.id.gestures) void onGestures() {
    if (GestureManager.getInstance(getContext()).isEmpty()) {
      Toast.makeText(getContext(), R.string.press_and_hold_gestures, Toast.LENGTH_LONG).show();
    } else {
      addOverlayView(new SmartGestureOverlay(getContext(), mOnGestureDetected));
    }
  }

  @OnClick(R.id.overflow) void onOverflow() {
    addOverlayView(new OverflowOptions(getContext(), mOnOptionFired));
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);
    if (View.VISIBLE == visibility) {
      mLastTimeVisible = System.currentTimeMillis();
      mFilterView.requestFocus();
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    postDelayed(new Runnable() {
      @Override
      public void run() {
        final Resources res = getResources();
        if (res != null) {
          layoutFromConfig(res.getConfiguration());
        }
      }
    }, 50);
  }

  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    layoutFromConfig(newConfig);
  }

  private void layoutFromConfig(Configuration newConfig) {
    LayoutParams gridViewLP = (LayoutParams) mAppGrid.getLayoutParams();
    LayoutParams filterViewLP = (LayoutParams) mFilterView.getLayoutParams();
    LayoutParams actionBarLP = (LayoutParams) mActionBar.getLayoutParams();

    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      mAdapter.setNumColumns(NUM_COLUMNS_LANDSCAPE);
      mAppGrid.setNumColumns(NUM_COLUMNS_LANDSCAPE);

      // action bar
      actionBarLP.addRule(LEFT_OF, R.id.filter_view);

      // gridview
      gridViewLP.addRule(LEFT_OF, R.id.filter_view);

      // filterview
      filterViewLP.addRule(ALIGN_PARENT_BOTTOM, 0);
      filterViewLP.addRule(ALIGN_PARENT_RIGHT, TRUE);
      //noinspection SuspiciousNameCombination
      filterViewLP.width = mFilterBarHeight;
      filterViewLP.height = ViewGroup.LayoutParams.MATCH_PARENT;
    } else {
      mAdapter.setNumColumns(NUM_COLUMNS_PORTRAIT);
      mAppGrid.setNumColumns(NUM_COLUMNS_PORTRAIT);

      // action bar
      actionBarLP.addRule(LEFT_OF, 0);

      // gridview
      gridViewLP.addRule(LEFT_OF, 0);

      // filterview
      filterViewLP.addRule(ALIGN_PARENT_BOTTOM, TRUE);
      filterViewLP.addRule(ALIGN_PARENT_RIGHT, 0);
      filterViewLP.width = ViewGroup.LayoutParams.MATCH_PARENT;
      filterViewLP.height = mFilterBarHeight;
    }

    requestLayout();
  }

  @SuppressWarnings("FieldCanBeLocal")
  private final AppsAdapter.EmptyListener mEmptyListener = new AppsAdapter.EmptyListener() {
    @Override
    public void adapterIsEmpty(String state, final String action) {
      mEmptyView.setVisibility(View.VISIBLE);
      mEmptyView.setText(state);

      mEmptyView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          if (AppsAdapter.EmptyListener.CLEAR_FILTER.equals(action)) {
            mFilterView.clear();
          } else if (AppsAdapter.EmptyListener.CLEAR_EXACT.equals(action)) {
            mFilterView.clearExactFilter();
          } else if (action != null) {
            AppUtils.searchPlayStore(getContext(), action);
            mFilterView.clear();
          }
        }
      });
    }

    @Override
    public void adapterIsNotEmpty() {
      mEmptyView.setText("");
      mEmptyView.setVisibility(View.GONE);
      mEmptyView.setOnClickListener(null);
    }
  };

  private final AdapterView.OnItemLongClickListener mItemLongClickListener = new AdapterView.OnItemLongClickListener() {
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
      AppsAdapter adapter = (AppsAdapter) parent.getAdapter();
      AppEntry item = adapter.getItem(position);
      return longClickedApp(item);
    }
  };

  private boolean longClickedApp(AppEntry item) {
    if (item == null) {
      return true;
    }

    final OptionsView child = new AppOptionsView(getContext(), mOnOptionFired, item);
    addOverlayView(child);
    vibrate();
    return true;
  }

  private final OnKeyListener mOnKeyListener = new OnKeyListener() {
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      return false;
    }
  };

  private final SmartGestureOverlay.OnGestureDetected mOnGestureDetected = new SmartGestureOverlay.OnGestureDetected() {
    @Override
    public void onNothingDetected() {
      Toast.makeText(getContext(), R.string.cant_recognize_gesture, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAppDetected(String id) {
      SqlAdapter adapter = Persistence.getAdapter(getContext());
      AppEntry found = adapter.findFirst(AppEntry.class, "_id = ?", new String[]{id});
      if (found == null) {
        Toast.makeText(getContext(), R.string.recognized_gesture_fail, Toast.LENGTH_LONG).show();
        return;
      }

      launchApp(found);
    }

    @Override
    public void onConfigGestures() {
      AppUtils.launch(getContext(), GesturesListActivity.class);
      clearOverlay();
      mExitListener.onExit();
    }
  };

  private void setupAppList() {
    mAppGrid.setOnItemLongClickListener(mItemLongClickListener);
    mAppGrid.setFocusable(false);
    mAppGrid.setOnKeyListener(mOnKeyListener);
    mAppGrid.setOnItemClickListener(mOnItemClickListener);
    mAppGrid.setAdapter(mAdapter);
  }


  private final AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      final AppEntry item = mAdapter.getItem(position);
      if (item != null) {
        launchApp(item);
      }
    }
  };

  private void addOverlayView(View child) {
    mOverlayStack.setBackgroundColor(getResources().getColor(R.color.meta_overlay_view_bg));
    mOverlayStack.addView(child, FRAME_MATCH_LP);
  }

  private void restoreSearchBox() {
    mSearchBox.setVisibility(View.GONE);
    mActionBarOptions.setVisibility(View.VISIBLE);
    mSearchBox.setText("");
    mFilterView.requestFocus();
  }

  final FilterView.OnFilterListener mOnFilterListener = new FilterView.OnFilterListener() {

    @Override
    public Set<Integer> onFilterUpdated() {
      mAdapter.updateFilteredApps();
      mAdapter.notifyDataSetChanged();
      mAppGrid.post(new Runnable() {
        @Override
        public void run() {
          mAppGrid.setSelection(0);
        }
      });
      return mAdapter.getColorsToIgnore();
    }

    @Override
    public void onExitFilter() {
      mExitListener.onExit();
      restoreSearchBox();
    }

    @Override
    public void onTouchReleased() {
      mHandler.removeMessages(LAUNCH_APP);
    }
  };

  void resetTransition(View previousView) {
    if (previousView != null) {
      Drawable background = previousView.getBackground();
      if (background instanceof TransitionDrawable) {
        TransitionDrawable td = (TransitionDrawable) background;
        td.resetTransition();
      }
    }
  }

  void launchApp(AppEntry app) {
    vibrate();
    clearEverything();
    AppUtils.launchApp(getContext(), app, mFilters, System.currentTimeMillis() - mLastTimeVisible);
  }

  private void clearEverything() {
    clearOverlay();
    mExitListener.onExit();
    mFilterView.clear();
    restoreSearchBox();
  }

  private void clearOverlay() {
    mOverlayStack.removeAllViews();
    mOverlayStack.setBackgroundColor(0);
  }

  void vibrate() {
    if (mUserPrefs != null && mUserPrefs.hapticFeedback) {
      Consolator.get(getContext()).vibrateShort();
    }
  }

  final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      switch (msg.what) {
        case LAUNCH_APP:
          launchApp((AppEntry) msg.obj);

          try {
            View view = mAppGrid.getChildAt(msg.arg1);
            resetTransition(view);
          } catch (Exception e) {
            Utilities.report(e);
          }
          break;
        case UPDATE_APPS:
          //noinspection unchecked
          mAdapter.updateApps((List<AppEntry>) msg.obj);
          break;
      }
    }
  };

  final OptionsView.OnOptionFired mOnOptionFired = new OptionsView.OnOptionFired() {
    @Override
    public void onAppLaunched(AppEntry app) {
      launchApp(app);
    }

    @Override
    public void onCreateGesture(AppEntry appEntry) {
      Bitmap icon = mAdapter.getIconFor(appEntry);
      CreateGestureView createGestureView = new CreateGestureView(getContext(), appEntry, icon);
      createGestureView.setOnGestureFinishedListener(mOnGestureFinishedListener);
      addOverlayView(createGestureView);
    }

    @Override
    public void optionFired(OptionsView view, int whatsNext) {
      removeFromOverlayStack(view);
      switch (whatsNext) {
        case OptionsView.EXIT:
          mExitListener.onExit();
          return;
        case OptionsView.UPDATE_ADAPTER:
          List<AppEntry> apps = Persistence.getAdapter(getContext()).findAll(AppEntry.class);
          mAdapter.updateApps(apps);
          return;
        case OptionsView.DO_NOTHING:
        default:
      }
    }
  };

  private void removeFromOverlayStack(OptionsView view) {
    mOverlayStack.removeView(view);
    if (mOverlayStack.getChildCount() == 0) {
      mOverlayStack.setBackgroundColor(0);
    }
  }

  private boolean popOverlayStack() {
    int childCount = mOverlayStack.getChildCount();
    if (childCount == 0) {
      mStackHandler.sendEmptyMessage(CLEAR_STACK_BACKGROUND_COLOR);
      return false;
    }
    mStackHandler.sendEmptyMessage(POP_FROM_OVERLAY_STACK);
    return true;
  }

  public void clearPlayGround() {
    mStackHandler.sendEmptyMessage(CLEAR_EVERYTHING);
  }

  private final Handler mStackHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      switch (msg.what) {
        case CLEAR_STACK_BACKGROUND_COLOR:
          mOverlayStack.setBackgroundColor(0);
          break;
        case POP_FROM_OVERLAY_STACK:
          final int childCount = mOverlayStack.getChildCount();
          if (childCount <= 0) {
            return;
          }
          mOverlayStack.removeViewAt(childCount - 1);
          if (mOverlayStack.getChildCount() == 0) {
            mOverlayStack.setBackgroundColor(0);
          }
          break;
        case CLEAR_EVERYTHING:
          clearEverything();
          break;
      }
    }
  };

  final CreateGestureView.OnGestureFinishedListener mOnGestureFinishedListener = new CreateGestureView.OnGestureFinishedListener() {
    @Override
    public void onGestureFinished(CreateGestureView view) {
      popOverlayStack();
    }
  };

  final TextWatcher mSearchWatcher = new TextWatcher() {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
      mAdapter.updateFilteredApps();
      mAdapter.notifyDataSetChanged();
      mFilterView.onFilterUpdated(true);
      mFilterView.invalidate();
    }
  };

  final TextFilter mTextFilter = new TextFilter() {
    @Override
    public String getCurrentFilter() {
      if (mSearchBox == null) {
        return null;
      }
      Editable text = mSearchBox.getText();
      if (text != null && text.length() > 0) {
        String cleaned = text.toString().trim();
        if (!TextUtils.isEmpty(cleaned)) {
          return cleaned;
        }
      }
      return null;
    }
  };

  public void updateApps(List<AppEntry> apps) {
    Message message = Message.obtain(mHandler, UPDATE_APPS, apps);
    mHandler.sendMessage(message);
  }

  public void updateUserPrefs(UserPrefs userPrefs) {
    mUserPrefs = userPrefs;
    mAdapter.updateUserPrefs(mUserPrefs);
    if (mFilterView != null) {
      mFilterView.updatePrefs(userPrefs);
    }
  }

  public boolean handleRestore() {
    return popOverlayStack();
  }

  public void updateBackground() {
    Context context = getContext();
    Resources res = context.getResources();
    WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
    Drawable wallpaperDrawable = wallpaperManager.getFastDrawable();
    int[] colors = {res.getColor(R.color.wallpaper_overlay), res.getColor(R.color.wallpaper_overlay_edges)};
    GradientDrawable gradientDrawable = new GradientDrawable(TOP_BOTTOM, colors);
    setBackground(new LayerDrawable(new Drawable[]{wallpaperDrawable, gradientDrawable}));
  }

  public void updateTopIcon(PackageManager pm, ComponentName topActivity) {
    if (pm == null || topActivity == null) {
      return;
    }
    Intent launchIntent = pm.getLaunchIntentForPackage(topActivity.getPackageName());
    if (launchIntent == null) {
      return;
    }

    ResolveInfo info = pm.resolveActivity(launchIntent, PackageManager.MATCH_DEFAULT_ONLY);
    if (info == null) {
      return;
    }

    final File apkFile = new File(info.activityInfo.applicationInfo.sourceDir);
    final Bitmap icon = IndexApps.getIcon(pm, info, apkFile);

    if (icon == null) {
      mActionBarIcon.setImageResource(R.drawable.ic_launcher);
      mActionBarIcon.setTag(null);
    } else {
      mActionBarIcon.setImageBitmap(icon);
      mActionBarIcon.setTag(topActivity.getPackageName());
    }
  }

  public interface OnExitListener {
    void onExit();
  }

  public interface TextFilter {
    String getCurrentFilter();
  }
}

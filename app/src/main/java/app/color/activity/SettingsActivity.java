package app.color.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.widget.SeekBar;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.PreferencesAdapter;
import com.telly.groundy.Groundy;
import com.telly.groundy.annotations.OnProgress;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;

import java.util.Arrays;
import java.util.List;

import app.color.R;
import app.color.core.AppColor;
import app.color.glue.SeekBarListenerAdapter;
import app.color.madness.Mad;
import app.color.model.UserPrefs;
import app.color.service.IndexApps;
import app.color.service.OverlayService;
import app.color.utils.Consolator;
import app.color.utils.Toaster;
import app.color.view.CellPhoneView;
import app.color.view.ColorProgressBar;
import app.color.view.HelpDialog;
import app.color.view.PrefCheckBox;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static app.color.utils.AppUtils.launchTutorial;

public class SettingsActivity extends FontActivity {

  static final int SENSITIVITY_RANGE = Mad.MAX_SENSITIVITY - Mad.MIN_SENSITIVITY;
  public static final int TUTORIAL_DELAY = 5000;
  PreferencesAdapter mPrefsAdapter;
  UserPrefs mUserPrefs;
  @InjectView(R.id.progress_bar) ColorProgressBar mColorProgressBar;
  @InjectView(R.id.cellphone) CellPhoneView mPhone;
  @InjectView(R.id.sensitivity) SeekBar mSensitivity;
  @InjectView(R.id.active_area_height) SeekBar mActiveAreaHeight;
  @InjectView(R.id.show_notification_icon) PrefCheckBox mNotificationIcon;
  @InjectView(R.id.haptic_feedback) PrefCheckBox mHapticFeedback;
  @InjectView(R.id.tap_to_select_color) PrefCheckBox mTapToSelectColor;
  @InjectView(R.id.show_hidden_apps) PrefCheckBox mShowHiddenApps;
  @InjectView(R.id.show_active_area) PrefCheckBox mShowActiveArea;
  @InjectView(R.id.active_area_position) PrefCheckBox mActiveAreaPosition;
  @InjectView(R.id.enable_app_color) PrefCheckBox mEnabled;


  OverlayService.OverlayBinder mServiceBinder;
  CellPhoneView.ScreenLayer mScreenLayer;
  private boolean mUserWasVirgin;
  private boolean mLaunchedTutorial;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);
    ButterKnife.inject(this);

    setupFont(R.id.settings_root);
    mColorProgressBar.setProgress(100);
    mColorProgressBar.mBackColor = getResources().getColor(R.color.flat_bg);

    mPrefsAdapter = Persistence.getPreferenceAdapter(SettingsActivity.this, AppColor.PREFS);
    mUserPrefs = mPrefsAdapter.retrieve(UserPrefs.class);

    mSensitivity.setMax(SENSITIVITY_RANGE);
    mSensitivity.setProgress(mUserPrefs.sensitivity + Mad.MIN_SENSITIVITY);
    mSensitivity.setOnSeekBarChangeListener(mSensitivityListener);

    mActiveAreaHeight.setMax(100);
    mActiveAreaHeight.setProgress(mUserPrefs.activeAreaHeight);
    mActiveAreaHeight.setOnSeekBarChangeListener(mActiveAreaHeightListener);

    mNotificationIcon.setChecked(mUserPrefs.showNotification);
    mNotificationIcon.mCheckListener = mPrefChangedListener;

    mHapticFeedback.setChecked(mUserPrefs.hapticFeedback);
    mHapticFeedback.mCheckListener = mPrefChangedListener;

    mTapToSelectColor.setChecked(mUserPrefs.tapToSelectColor);
    mTapToSelectColor.mCheckListener = mPrefChangedListener;

    mShowHiddenApps.setChecked(mUserPrefs.showHiddenApps);
    mShowHiddenApps.mCheckListener = mPrefChangedListener;

    mShowActiveArea.setChecked(mUserPrefs.showActiveArea);
    mShowActiveArea.mCheckListener = mPrefChangedListener;

    mActiveAreaPosition.setChecked(mUserPrefs.activeAreaRight);
    mActiveAreaPosition.mCheckListener = mPrefChangedListener;

    mEnabled.setChecked(mUserPrefs.enabled);
    if (!mUserPrefs.enabled) {
      enableControls(false);
    }
    mColorProgressBar.setEnabled(mUserPrefs.enabled);
    mEnabled.mCheckListener = mPrefChangedListener;

    if (mUserPrefs.enabled) {
      startOverlayService();
    }

    setupLongClickListener();

    mScreenLayer = new ActiveAreaScreenLayer();
    mPhone.addScreenLayer(mScreenLayer);

    if (mUserPrefs.virgin) {
      mUserWasVirgin = true;
      indexApps();
      Toaster.showLong(this, R.string.updating_apps_database);
      mUserPrefs.virgin = false;
      updatePrefs();
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          launchTutorialIfNecessary();
        }
      }, TUTORIAL_DELAY);
    }
  }

  @OnClick(R.id.refresh) void onRefreshClicked() {
    indexApps();
  }

  @OnClick(R.id.tutorial) void onTutorialClicked() {
    launchTutorial(SettingsActivity.this);
  }

  @OnClick(R.id.active_area_gravity) void onActiveAreaGravity() {
    switch (mUserPrefs.handleGravity) {
      case Gravity.CENTER_VERTICAL:
        mUserPrefs.handleGravity = Gravity.BOTTOM;
        break;
      case Gravity.TOP:
        mUserPrefs.handleGravity = Gravity.CENTER_VERTICAL;
        break;
      default:
        mUserPrefs.handleGravity = Gravity.TOP;
        break;
    }

    if (mServiceBinder != null) {
      mServiceBinder.updateGravity(mUserPrefs.handleGravity);
    }
    updatePrefs();
    mPhone.invalidateScreenLayers();
  }

  private void launchTutorialIfNecessary() {
    if (!mLaunchedTutorial) {
      mLaunchedTutorial = true;
      launchTutorial(this);
    }
  }

  void startOverlayService() {
    OverlayService.start(this, false);
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        bindOverlayService();
      }
    }, 500);
  }

  private void indexApps() {
    Groundy.create(IndexApps.class)
           .callback(SettingsActivity.this)
           .queueUsing(SettingsActivity.this);
  }

  private void bindOverlayService() {
    bindService(new Intent(this, OverlayService.class), mServiceConnection, BIND_AUTO_CREATE);
  }

  final ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
      mServiceBinder = (OverlayService.OverlayBinder) iBinder;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      mServiceBinder = null;
    }
  };

  @Override
  protected void onResume() {
    super.onResume();
    if (mUserPrefs.enabled) {
      startOverlayService();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    try {
      unbindService(mServiceConnection);
    } catch (Exception ignored) {
    }
  }

  void setupLongClickListener() {
    for (int i = 0; i < HELP_MAP.size(); i++) {
      findViewById(HELP_MAP.keyAt(i)).setOnLongClickListener(mOnLongClickListener);
    }
  }

  void enableControls(boolean enabled) {
    List<Integer> ids = Arrays.asList(R.id.haptic_feedback, R.id.show_notification_icon, R.id.refresh,
        R.id.show_active_area, R.id.tap_to_select_color, R.id.active_area_height_label, R.id.active_area_height,
        R.id.sensitivity, R.id.sensitivity_label, R.id.active_area_gravity,
        R.id.active_area_position, R.id.show_hidden_apps);
    for (Integer id : ids) {
      findViewById(id).setEnabled(enabled);
    }
  }

  @OnProgress(IndexApps.class)
  public void onIndexProgress(@Param(Groundy.PROGRESS) int progress) {
    mColorProgressBar.setProgress(progress);
  }

  @OnSuccess(IndexApps.class)
  public void onAppsIndexed() {
    if (mUserWasVirgin) {
      launchTutorialIfNecessary();
    }
  }

  void updatePrefs() {
    mPrefsAdapter.store(mUserPrefs);
  }

  final SeekBar.OnSeekBarChangeListener mSensitivityListener = new SeekBarListenerAdapter() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      mUserPrefs.sensitivity = Mad.MIN_SENSITIVITY + progress;
      updatePrefs();
    }
  };

  final SeekBar.OnSeekBarChangeListener mActiveAreaHeightListener = new SeekBarListenerAdapter() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (mServiceBinder != null) {
        mServiceBinder.updateActiveAreaHeight(progress);
      }
      mUserPrefs.activeAreaHeight = Math.max(10, progress);
      updatePrefs();
      mPhone.invalidateScreenLayers();
    }
  };

  final View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
    @Override
    public boolean onLongClick(View v) {
      int id = HELP_MAP.get(v.getId(), -1);
      boolean valid = id != -1;
      if (valid) {
        HelpDialog.show(getFragmentManager(), id);
      }
      return valid;
    }
  };

  final PrefCheckBox.OnCheckedChangeListener mPrefChangedListener = new PrefCheckBox.OnCheckedChangeListener() {
    @Override
    public void onCheckedChanged(int id, boolean checked) {
      switch (id) {
        case R.id.active_area_position:
          mUserPrefs.activeAreaRight = checked;
          if (mServiceBinder != null) {
            mServiceBinder.updatePosition(mUserPrefs.activeAreaRight);
          }
          mPhone.invalidateScreenLayers();
          break;
        case R.id.haptic_feedback:
          mUserPrefs.hapticFeedback = checked;
          if (mUserPrefs.hapticFeedback) {
            Consolator.get(SettingsActivity.this).vibrateShort();
          }
          break;
        case R.id.tap_to_select_color:
          mUserPrefs.tapToSelectColor = checked;
          break;
        case R.id.show_hidden_apps:
          mUserPrefs.showHiddenApps = checked;
          if (mUserPrefs.enabled) {
            OverlayService.startAndUpdate(SettingsActivity.this);
          }
          break;
        case R.id.show_active_area:
          mUserPrefs.showActiveArea = checked;
          break;
        case R.id.show_notification_icon:
          mUserPrefs.showNotification = checked;
          break;
        case R.id.enable_app_color:
          mUserPrefs.enabled = checked;
          if (mUserPrefs.enabled) {
            startOverlayService();
            mColorProgressBar.setProgress(100);
            enableControls(true);
          } else {
            unbindService(mServiceConnection);
            OverlayService.stop(SettingsActivity.this);
            enableControls(false);
          }
          mColorProgressBar.setEnabled(mUserPrefs.enabled);
          break;
      }
      updatePrefs();
    }
  };


  class ActiveAreaScreenLayer implements CellPhoneView.ScreenLayer {
    final Paint mPaint = new Paint();
    final int mActiveAreaColor;
    final RectF mActiveArea = new RectF();

    ActiveAreaScreenLayer() {
      mActiveAreaColor = getResources().getColor(R.color.settings_mode);
    }

    @Override
    public void draw(Canvas canvas, float width, float height) {
      mPaint.setColor(mActiveAreaColor);
      canvas.drawRect(mActiveArea, mPaint);
    }

    @Override
    public void invalidate(float screenWidth, float screenHeight) {
      float activeAreaHeight = mUserPrefs.activeAreaHeight / 100f;
      float realHeight = Math.max(screenHeight * activeAreaHeight, screenHeight / 12);
      float top;

      switch (mUserPrefs.handleGravity) {
        case Gravity.TOP:
          top = 0;
          break;
        case Gravity.BOTTOM:
          top = screenHeight - realHeight;
          break;
        case Gravity.CENTER_VERTICAL:
        default:
          top = screenHeight / 2 - realHeight / 2;
          break;
      }

      float activeAreaWidth = screenWidth / 10;
      float left;
      if (mUserPrefs.activeAreaRight) {
        left = screenWidth - activeAreaWidth;
      } else {
        left = 0;
      }
      mActiveArea.set(left, top, left + activeAreaWidth, top + realHeight);
    }
  }

  static final SparseIntArray HELP_MAP = new SparseIntArray();

  static {
    HELP_MAP.put(R.id.enable_app_color, R.array.enable_app_color);
    HELP_MAP.put(R.id.show_notification_icon, R.array.show_notification_icon);
    HELP_MAP.put(R.id.active_area_position, R.array.active_area_position);
    HELP_MAP.put(R.id.active_area_gravity, R.array.active_area_gravity);
    HELP_MAP.put(R.id.refresh, R.array.refresh);
    HELP_MAP.put(R.id.show_hidden_apps, R.array.show_hidden_apps);
    HELP_MAP.put(R.id.active_area_height_label, R.array.active_area_height_label);
    HELP_MAP.put(R.id.show_active_area, R.array.show_active_area);
    HELP_MAP.put(R.id.haptic_feedback, R.array.haptic_feedback);
    HELP_MAP.put(R.id.tap_to_select_color, R.array.tap_to_select_color);
    HELP_MAP.put(R.id.sensitivity_label, R.array.sensitivity_label);
    HELP_MAP.put(R.id.tutorial, R.array.tutorial);
  }
}

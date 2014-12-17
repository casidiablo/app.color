package app.color.service;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.PreferencesAdapter;
import com.codeslap.persistence.SqlAdapter;
import com.telly.groundy.Groundy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.color.R;
import app.color.activity.SettingsActivity;
import app.color.core.AppColor;
import app.color.model.AppEntry;
import app.color.model.GlobalAppLog;
import app.color.model.UserPrefs;
import app.color.utils.AppUtils;
import app.color.utils.PackageHelper;
import app.color.utils.Toaster;
import app.color.utils.Utilities;
import app.color.view.BubbleView;
import app.color.view.PlayGroundView;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.Gravity.LEFT;
import static android.view.Gravity.RIGHT;
import static android.view.Gravity.TOP;
import static android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.TYPE_PHONE;
import static app.color.utils.Utilities.report;

public class OverlayService extends Service {

  public static final String SHOW_OVERLAY = "app.color.SHOW_OVERLAY";
  public static final String FROM_WIDGET = "app.color.FROM_WIDGET";
  static final String UPDATE_APPS = "app.color.UPDATE_APPS";
  static final int NOTIF_ID = 8423;
  static final int FADE_DURATION = 400;
  static final int USAGE_MONITOR_INTERVAL = 500;
  public static final float MIN_HANDLE_HEIGHT_FACTOR = 0.1f;

  WindowManager mWindowManager;

  PlayGroundView mPlayGroundView;
  FrameLayout mMainContainer;
  BubbleView mBubbleView;

  WindowManager.LayoutParams mMainContainerCollapsedParams;
  WindowManager.LayoutParams mMainContainerExpandedParams;

  PackageHelper.PackageIntentReceiver mPackageObserver;
  final PackageHelper.InterestingConfigChanges mLastConfig = new PackageHelper.InterestingConfigChanges();

  ActivityManager mActivityManager;
  UsageHandler mUsageHandler;
  SqlAdapter mAdapter;
  KeyguardManager mKeyguardManager;
  SharedPreferences mSharedPreferences;
  PreferencesAdapter mPrefsAdapter;
  UserPrefs mUserPrefs;
  final OverlayBinder mBinder = new OverlayBinder();
  private final Map<String, String> mLaunchers = new HashMap<String, String>();
  private TelephonyManager mTelephonyManager;
  private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
        mUsageHandler.sendEmptyMessageDelayed(0, USAGE_MONITOR_INTERVAL);
      } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
        mUsageHandler.removeMessages(0);
      }
    }
  };
  private final BroadcastReceiver mWallpaperChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      //noinspection deprecation
      if (Intent.ACTION_WALLPAPER_CHANGED.equals(intent.getAction()) && mPlayGroundView != null) {
        mPlayGroundView.updateBackground();
      }
    }
  };
  private boolean mOverlayJustShowed;
  private PackageManager mPackageManager;

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

    mPrefsAdapter = Persistence.getPreferenceAdapter(this, AppColor.PREFS);
    mUserPrefs = mPrefsAdapter.retrieve(UserPrefs.class);

    mSharedPreferences = getSharedPreferences(AppColor.PREFS, MODE_PRIVATE);
    mSharedPreferences.registerOnSharedPreferenceChangeListener(mPrefsListener);

    mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

    // configure params for main container when it's collapsed
    recreateCollapsedParams(mUserPrefs.activeAreaHeight, mUserPrefs.activeAreaRight, mUserPrefs.handleGravity);

    // configure params for main container when it's expanded
    int flags = FLAG_HARDWARE_ACCELERATED;
    mMainContainerExpandedParams = new WindowManager.LayoutParams(
        MATCH_PARENT, MATCH_PARENT,
        0, 0,
        TYPE_PHONE,
        flags,
        PixelFormat.TRANSLUCENT);
    mMainContainerExpandedParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

    mAdapter = Persistence.getAdapter(this);
    setupMainContainer();

    startForeground(NOTIF_ID, getNotification());


    // Start watching for changes in the app data.
    if (mPackageObserver == null) {
      mPackageObserver = new PackageHelper.PackageIntentReceiver(this);
    }

    mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

    new Thread(new Runnable() {
      @Override
      public void run() {
        Looper.prepare();
        mUsageHandler = new UsageHandler();
        mUsageHandler.sendEmptyMessageDelayed(0, USAGE_MONITOR_INTERVAL);
        Looper.loop();
      }
    }).start();

    IntentFilter screenFilter = new IntentFilter();
    screenFilter.addAction(Intent.ACTION_SCREEN_ON);
    screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
    registerReceiver(mScreenReceiver, screenFilter);

    IntentFilter filterWallpaper = new IntentFilter();
    //noinspection deprecation
    filterWallpaper.addAction(Intent.ACTION_WALLPAPER_CHANGED);
    registerReceiver(mWallpaperChangedReceiver, filterWallpaper);

    mPackageManager = getPackageManager();
    getLaunchers();
  }

  void recreateCollapsedParams(int heightPercentage, boolean activeAreaRight, int newGravity) {
    Resources resources = getResources();
    DisplayMetrics metrics = resources.getDisplayMetrics();
    float heightFactor = Math.max(MIN_HANDLE_HEIGHT_FACTOR, heightPercentage / 100f);
    int handleHeight = (int) (metrics.heightPixels * heightFactor);
    int handleWidth = (int) resources.getDimension(R.dimen.handle_width);

    mMainContainerCollapsedParams = new WindowManager.LayoutParams(
        handleWidth, handleHeight,
        0, 0,
        TYPE_PHONE,
        FLAG_NOT_FOCUSABLE | FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT);
    int gravity = activeAreaRight ? RIGHT : LEFT;
    if (newGravity != CENTER_VERTICAL && newGravity != BOTTOM && newGravity != TOP) {
      newGravity = CENTER_VERTICAL;
    }
    mMainContainerCollapsedParams.gravity = newGravity | gravity;
  }

  Notification getNotification() {
    Intent intent = new Intent(this, SettingsActivity.class);
    PendingIntent activityIntent = PendingIntent.getActivity(this, 0, intent, 0);

    Notification.Builder builder = new Notification.Builder(this)
        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_stat_app))
        .setSmallIcon(R.drawable.ic_stat_app)
        .setContentTitle(getString(R.string.app_color_running))
        .setContentText(getString(R.string.tap_to_settings))
        .setContentIntent(activityIntent)
        .setOngoing(true);
    if (!mUserPrefs.showNotification) {
      builder.setPriority(-Integer.MAX_VALUE);
    }
    return builder.build();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    boolean showOverlay = intent != null && intent.getBooleanExtra(SHOW_OVERLAY, false);
    if (showOverlay) {
      updateExpandedView();
      showPlayGround();
    }

    if (intent != null && mPlayGroundView != null && UPDATE_APPS.equals(intent.getAction())) {
      mPlayGroundView.updateApps(getAppEntries());
    }

    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    removeMainContainer();
    stopForeground(false);
    unregisterReceiver(mScreenReceiver);
    unregisterReceiver(mWallpaperChangedReceiver);

    // Stop monitoring for changes.
    if (mPackageObserver != null) {
      unregisterReceiver(mPackageObserver);
      mPackageObserver = null;
    }

    mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    mTelephonyManager.listen(mPhoneStateListener, 0);
  }

  private void removeMainContainer() {
    if (mMainContainer != null) {
      try {
        mWindowManager.removeView(mMainContainer);
      } catch (Exception e) {
        report(e);
      }
    }
  }

  private void setupMainContainer() {
    mMainContainer = new FrameLayout(this);
    mMainContainer.setClickable(true);
    mMainContainer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        removeOverlay(false);
      }
    });
    int gravity = mUserPrefs.activeAreaRight ? RIGHT : LEFT;
    mBubbleView = new BubbleView(this, gravity);
    mBubbleView.setOnBubbleChange(new BubbleView.OnBubbleChangeListener() {
      @Override
      public void onBubbleChange(int newTransparency) {
        mMainContainer.setBackgroundColor(newTransparency << 24);
      }

      @Override
      public void onBubbleComplete() {
        showPlayGround();
      }

      @Override
      public void onStartTouching() {
        updateExpandedView();
      }

      @Override
      public void onCancel() {
        removeOverlay(false);
      }
    });

    mMainContainer.addView(mBubbleView);
    addPlayGround();

    addCollapsedView();
  }

  private void updateExpandedView() {
    updateParams(mMainContainer, mMainContainerExpandedParams);
  }

  private void addCollapsedView() {
    try {
      mBubbleView.enable();
      mWindowManager.addView(mMainContainer, mMainContainerCollapsedParams);
    } catch (Exception e) {
      e.printStackTrace();
      report(e);
    }
  }

  private void collapseMainView() {
    updateParams(mMainContainer, mMainContainerCollapsedParams);
  }

  void showPlayGround() {
    mOverlayJustShowed = true;

    // get latest running app...
    ComponentName topActivity = getTopActivity();
    mPlayGroundView.updateTopIcon(mPackageManager, topActivity);

    mMainContainer.setBackgroundColor(0);
    mBubbleView.setVisibility(View.GONE);

    Animation animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
    if (animation != null) {
      animation.setDuration(FADE_DURATION);
      mPlayGroundView.startAnimation(animation);
    }

    mPlayGroundView.setVisibility(View.VISIBLE);
    mPlayGroundView.setFocusable(true);
    mPlayGroundView.requestFocus();
  }

  void updateParams(FrameLayout view, WindowManager.LayoutParams params) {
    try {
      mWindowManager.updateViewLayout(view, params);
    } catch (Exception e) {
      report(e);
    }
  }

  void addPlayGround() {
    if (mPlayGroundView == null) {
      mLastConfig.applyNewConfig(getResources());
      final List<AppEntry> apps = getAppEntries();

      mPlayGroundView = new PlayGroundView(OverlayService.this, new PlayGroundView.OnExitListener() {
        @Override
        public void onExit() {
          removeOverlay(false);
        }
      }, apps);
      updateUserPrefs();

      mPlayGroundView.setVisibility(View.GONE);
    }

    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    mMainContainer.addView(mPlayGroundView, lp);

    // Has something interesting in the configuration changed since we last built the app list?
    boolean configChange = mLastConfig.applyNewConfig(getResources());
    if (configChange) {
      Groundy.create(IndexApps.class).queueUsing(this);
    }
  }

  private void updateUserPrefs() {
    mPlayGroundView.updateUserPrefs(mUserPrefs);
    mBubbleView.updateUserPrefs(mUserPrefs);
  }

  List<AppEntry> getAppEntries() {
    return mAdapter.findAll(AppEntry.class);
  }

  void removeOverlay(boolean force) {
    if (force || !mPlayGroundView.handleRestore()) {
      mOverlayJustShowed = false;
      mRestoreHandler.sendEmptyMessageDelayed(0, 50);
    }

    if (force) {
      mPlayGroundView.clearPlayGround();
    }
  }

  public static void start(Context context, boolean showOverlay) {
    Intent service = new Intent(context, OverlayService.class);
    service.putExtra(OverlayService.SHOW_OVERLAY, showOverlay);
    context.startService(service);
  }

  public static void startAndUpdate(Context context) {
    context.startService(new Intent(context, OverlayService.class)
        .setAction(OverlayService.UPDATE_APPS));
  }

  public static void stop(Context context) {
    context.stopService(new Intent(context, OverlayService.class));
  }

  final Handler mRestoreHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      removeMainContainer();
      mPlayGroundView.setVisibility(View.GONE);
      mBubbleView.setVisibility(View.VISIBLE);
      mBubbleView.enable();
      mMainContainer.setBackgroundColor(0);
      addCollapsedView();
    }
  };

  private void getLaunchers() {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_DEFAULT);
    intent.addCategory(Intent.CATEGORY_HOME);
    List<ResolveInfo> launchers = mPackageManager.queryIntentActivities(intent, 0);
    int launchersSize = launchers.size();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < launchersSize; i++) {
      ResolveInfo launcher = launchers.get(i);
      if (launcher != null && launcher.activityInfo != null &&
          launcher.activityInfo.applicationInfo != null) {
        mLaunchers.put(launcher.activityInfo.applicationInfo.packageName, launcher.activityInfo.name);
      }
    }
  }

  ComponentName getTopActivity() {
    if (mActivityManager == null) {
      return null;
    }

    List<ActivityManager.RunningTaskInfo> runningTasks;
    try {
      runningTasks = mActivityManager.getRunningTasks(1);
    } catch (Exception e) {
      Utilities.report(e);
      mUsageHandler.sendEmptyMessageDelayed(0, USAGE_MONITOR_INTERVAL);
      return null;
    }
    if (runningTasks != null && !runningTasks.isEmpty()) {
      ActivityManager.RunningTaskInfo taskInfo = runningTasks.get(0);
      if (taskInfo != null) {
        return taskInfo.topActivity;
      }
    }
    return null;
  }

  final class UsageHandler extends Handler {

    private String mPreviouslyLaunchedApp;

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      if (mKeyguardManager == null || mKeyguardManager.inKeyguardRestrictedInputMode()) {
        sendEmptyMessageDelayed(0, USAGE_MONITOR_INTERVAL);
        return;
      }

      ComponentName topActivity = getTopActivity();
      if (topActivity != null) {
        String packageName = topActivity.getPackageName();
        if (mOverlayJustShowed) {
          mPreviouslyLaunchedApp = packageName;
          mOverlayJustShowed = false;
        } else if (mPreviouslyLaunchedApp != null && !mPreviouslyLaunchedApp.equals(packageName)) {
          removeOverlay(true);
          mPreviouslyLaunchedApp = null;
        }

        if (mLaunchers.containsKey(packageName) && mLaunchers.get(packageName).equals(topActivity.getClassName())) {
          sendEmptyMessageDelayed(0, USAGE_MONITOR_INTERVAL);
          return;
        }

        long now = System.currentTimeMillis();
        //noinspection MagicConstant
        long today = now - ((now % 86400000) + 60000);
        String where = "package_name = ? AND date = ?";
        String[] whereArgs = {packageName, String.valueOf(today)};
        GlobalAppLog appRecord = mAdapter.findFirst(GlobalAppLog.class, where, whereArgs);
        if (appRecord == null) {
          appRecord = new GlobalAppLog();
          appRecord.packageName = packageName;
          appRecord.date = today;
          appRecord.id = (Long) mAdapter.store(appRecord);
        }

        appRecord.count++;
        mAdapter.update(appRecord, where, whereArgs);
      }

      sendEmptyMessageDelayed(0, USAGE_MONITOR_INTERVAL);
    }
  }

  final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
      UserPrefs retrieve = mPrefsAdapter.retrieve(UserPrefs.class);
      boolean shouldRestartService = retrieve.showNotification != mUserPrefs.showNotification
          || retrieve.enabled != mUserPrefs.enabled;
      mUserPrefs = retrieve;
      updateFromPreferences(shouldRestartService);
    }
  };

  void updateFromPreferences(boolean shouldRestartService) {
    if (shouldRestartService) {
      stopForeground(true);
      startForeground(NOTIF_ID, getNotification());
    }

    if (mPlayGroundView != null) {
      updateUserPrefs();
    }
  }

  public class OverlayBinder extends Binder {
    public void updateActiveAreaHeight(int newHeight) {
      recreateCollapsedParams(newHeight, mUserPrefs.activeAreaRight, mUserPrefs.handleGravity);
      collapseMainView();
    }

    public void updatePosition(boolean right) {
      recreateCollapsedParams(mUserPrefs.activeAreaHeight, right, mUserPrefs.handleGravity);
      if (mBubbleView != null) {
        mBubbleView.updateGravity(right ? RIGHT : LEFT);
      }
      collapseMainView();
    }

    public void updateGravity(int newGravity) {
      recreateCollapsedParams(mUserPrefs.activeAreaHeight, mUserPrefs.activeAreaRight, newGravity);
      collapseMainView();
    }
  }

  private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
      super.onCallStateChanged(state, incomingNumber);
      if (state == TelephonyManager.CALL_STATE_RINGING) {
        removeOverlay(true);
      }
    }
  };
}

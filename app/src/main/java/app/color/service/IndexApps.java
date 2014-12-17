package app.color.service;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.SqlAdapter;
import com.telly.groundy.Groundy;
import com.telly.groundy.GroundyTask;
import com.telly.groundy.TaskResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import app.color.core.AppColor;
import app.color.madness.Mad;
import app.color.model.AppEntry;
import app.color.model.UserPrefs;
import app.color.utils.Utilities;

public class IndexApps extends GroundyTask {

  /**
   * Getting palette distribution is 95% of the work
   */
  static final int IMAGE_PROCESSING_PERCENTAGE = 95;
  static final float PERSISTENCE_PERCENTAGE_RATIO = (int) (5.f / 100f);
  public static final String ARG_PACKAGE_NAME = "app.color.ARG_PACKAGE_NAME";
  public static final String ARG_REMOVE_ENTRY = "app.color.SHOULD_REMOVE_ENTRY";
  private SqlAdapter mAdapter;
  private PackageManager mPackageManager;
  private UserPrefs mUserPrefs;
  private int mSize;
  private AtomicInteger mFinishedTasksCount = new AtomicInteger(0);
  private List<AppEntry> mAppEntries = Collections.synchronizedList(new ArrayList<AppEntry>());

  protected TaskResult doInBackground() {
    mAdapter = Persistence.getAdapter(getContext());

    boolean removeEntry = getBooleanArg(ARG_REMOVE_ENTRY);
    String pkgName = getStringArg(ARG_PACKAGE_NAME, null);
    if (removeEntry) {
      removeEntry(pkgName);
      return succeeded();
    }
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    mPackageManager = getContext().getPackageManager();

    mUserPrefs = Persistence.quickPref(getContext(), AppColor.PREFS, UserPrefs.class);

    List<ResolveInfo> apps;
    if (pkgName == null) {
      Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
      mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
      apps = mPackageManager.queryIntentActivities(mainIntent, 0);
    } else {
      Intent launchIntent = mPackageManager.getLaunchIntentForPackage(pkgName);
      ResolveInfo resolveInfo = mPackageManager.resolveActivity(launchIntent, PackageManager.MATCH_DEFAULT_ONLY);
      apps = Arrays.asList(resolveInfo);
    }

    if (apps == null) {
      apps = new ArrayList<ResolveInfo>();
    }
    // Create corresponding array of entries and load their labels.
    mSize = apps.size();

    ArrayList<Callable<Object>> callables = new ArrayList<Callable<Object>>();
    for (int i = 0; i < mSize; i++) {
      final ResolveInfo info = apps.get(i);
      callables.add(Executors.callable(new Runnable() {
        @Override
        public void run() {
          getPalette(info);
        }
      }));
    }

    try {
      executorService.invokeAll(callables);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    updateProgress(IMAGE_PROCESSING_PERCENTAGE);

    if (pkgName == null) {
      mAdapter.storeUniqueCollection(mAppEntries, new SqlAdapter.ProgressListener() {
        int lastPercentage = -1;

        @Override
        public void onProgressChange(int percentage) {
          if (lastPercentage == percentage) {
            return;
          }
          updateProgress((int) (IMAGE_PROCESSING_PERCENTAGE + percentage * PERSISTENCE_PERCENTAGE_RATIO));
          lastPercentage = percentage;
        }
      });
    } else {
      mAdapter.store(mAppEntries.get(0));
    }

    updateProgress(100);

    OverlayService.startAndUpdate(getContext());

    return succeeded();
  }

  private void getPalette(ResolveInfo info) {
    String className = info.activityInfo.name;

    final File apkFile = new File(info.activityInfo.applicationInfo.sourceDir);
    final Bitmap icon = getIcon(mPackageManager, info, apkFile);
    String packageName = info.activityInfo.applicationInfo.packageName;
    if (icon == null || className == null || packageName == null || getContext().getPackageName().equals(packageName)) {
      return;
    }
    final int paletteDistribution = Mad.safePalette(icon, mUserPrefs.sensitivity);

    final AppEntry appEntry = new AppEntry();
    appEntry.className = className;
    appEntry.packageName = packageName;
    appEntry.id = appEntry.packageName + "/" + appEntry.className;
    appEntry.colorDistribution = paletteDistribution;
    appEntry.label = getLabel(mPackageManager, info, apkFile);

    mAppEntries.add(appEntry);

    Utilities.saveIcon(getContext(), appEntry.id, icon);
    increaseProgress();
  }

  private void increaseProgress() {
    int finishedTasks = mFinishedTasksCount.incrementAndGet();
    updateProgress(finishedTasks * IMAGE_PROCESSING_PERCENTAGE / mSize);
  }

  private void removeEntry(String pkgName) {
    mAdapter.delete(AppEntry.class, "package_name = ?", new String[]{pkgName});
    OverlayService.startAndUpdate(getContext());
  }

  public static Bitmap getIcon(PackageManager packageManager, ResolveInfo info, File apkFile) {
    if (apkFile.exists()) {
      Drawable icon = info.loadIcon(packageManager);
      return Utilities.drawableToBitmap(icon);
    }
    return null;
  }

  private static String getLabel(PackageManager packageManager, ResolveInfo info, File apkFile) {
    if (apkFile.exists()) {
      CharSequence label = info.loadLabel(packageManager);
      return label != null ? label.toString() : info.activityInfo.applicationInfo.packageName;
    }
    return info.activityInfo.applicationInfo.packageName;
  }
}

package app.color.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import app.color.service.IndexApps;
import com.telly.groundy.Groundy;

public final class PackageHelper {

  private PackageHelper() {
  }

  /**
   * Helper for determining if the configuration has changed in an interesting
   * way so we need to rebuild the app list.
   */
  public static class InterestingConfigChanges {
    final Configuration mLastConfiguration = new Configuration();
    int mLastDensity;

    public boolean applyNewConfig(Resources res) {
      int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
      boolean densityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
      if (densityChanged
          || (configChanges & (ActivityInfo.CONFIG_LOCALE
          | ActivityInfo.CONFIG_UI_MODE
          | ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
        mLastDensity = res.getDisplayMetrics().densityDpi;
        return true;
      }
      return false;
    }
  }

  /**
   * Helper class to look for interesting changes to the installed apps
   * so that the loader can be updated.
   */
  public static class PackageIntentReceiver extends BroadcastReceiver {

    public PackageIntentReceiver(Context context) {
      IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
      filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
      filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
      filter.addDataScheme("package");
      context.registerReceiver(this, filter);
      // Register for events related to sdcard installation.
      IntentFilter sdFilter = new IntentFilter();
      sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
      sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
      context.registerReceiver(this, sdFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      boolean shouldUpdateIcon = Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())
          || Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction());
      boolean shouldRemoveEntry = Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction());
      if (shouldUpdateIcon || shouldRemoveEntry) {
        String packageName = intent.getData().getSchemeSpecificPart();
        Groundy.create(IndexApps.class)
            .arg(IndexApps.ARG_REMOVE_ENTRY, shouldRemoveEntry)
            .arg(IndexApps.ARG_PACKAGE_NAME, packageName)
            .queueUsing(context);
      }
    }
  }
}

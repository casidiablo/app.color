package app.color.utils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.SqlAdapter;

import java.util.Collection;

import app.color.activity.TutorialActivity;
import app.color.model.AppEntry;
import app.color.model.AppLog;

import static android.content.Intent.ACTION_UNINSTALL_PACKAGE;
import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static app.color.utils.Utilities.report;

public class AppUtils {
  private static final String SCHEME = "package";

  static Typeface sFont;

  public static void launchApp(Context context, AppEntry app, Collection<Integer> filterUsed, long elapsedTime) {
    final Intent intent = new Intent();
    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
    intent.setClassName(app.packageName, app.className);

    try {
      context.startActivity(intent);

      app.launchCounts++;
      SqlAdapter adapter = Persistence.getAdapter(context);
      adapter.update(app, "_id = ?", new String[]{String.valueOf(app.id)});

      AppLog appLog = new AppLog();
      appLog.appId = app.id;
      appLog.launchTime = System.currentTimeMillis();
      if (filterUsed != null) {
        for (Integer filter : filterUsed) {
          appLog.filterUsed |= 0xf << (4 * filter);
        }
      }
      appLog.elapsedTime = elapsedTime;

      adapter.store(appLog);
    } catch (Exception e) {
      report(e);
    }
  }

  public static void launchTutorial(Context context) {
    context.startActivity(new Intent(context, TutorialActivity.class)
        .addFlags(FLAG_ACTIVITY_NEW_TASK));
  }

  public static void launchPlayStore(Context context, AppEntry app) {
    final String packageName = app == null ? null : app.packageName;
    launchPlayStore(context, "details?id", packageName);
  }

  public static void searchPlayStore(Context context, String query) {
    launchPlayStore(context, "search?q", query);
  }

  private static void launchPlayStore(Context context, String urlPart, String appName) {
    if (appName == null) {
      try {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.android.vending")
            .addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
      } catch (Exception anfe) {
        report(anfe);
      }
      return;
    }

    try {
      String url = String.format("market://%s=%s", urlPart, appName);
      context.startActivity(new Intent(ACTION_VIEW, Uri.parse(url))
          .addFlags(FLAG_ACTIVITY_NEW_TASK));
    } catch (Exception anfe) {
      report(anfe);
      try {
        String url = String.format("http://play.google.com/store/apps/%s=%s", urlPart, appName);
        context.startActivity(new Intent(ACTION_VIEW, Uri.parse(url))
            .addFlags(FLAG_ACTIVITY_NEW_TASK));
      } catch (Exception e) {
        report(e);
      }
    }
  }

  public static void uninstall(Context context, AppEntry app) {
    try {
      Uri packageUri = Uri.parse("package:" + app.packageName);
      Intent uninstallIntent = new Intent(ACTION_UNINSTALL_PACKAGE, packageUri);
      uninstallIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(uninstallIntent);
    } catch (Exception e) {
      report(e);
    }
  }

  public static void launch(Context context, Class<?> cls) {
    try {
      context.startActivity(new Intent(context, cls).addFlags(FLAG_ACTIVITY_NEW_TASK));
    } catch (Exception anfe) {
      report(anfe);
    }
  }

  public static void launchActivity(Context context, Class<?> cls) {
    try {
      context.startActivity(new Intent(context, cls));
    } catch (Exception anfe) {
      report(anfe);
    }
  }

  public static boolean launchAppInfo(Context context, String packageName) {
    try {
      Intent intent = new Intent();
      intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
      intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      Uri uri = Uri.fromParts(SCHEME, packageName, null);
      intent.setData(uri);
      context.startActivity(intent);
      return true;
    } catch (Exception e) {
      report(e);
    }
    return false;
  }

  public static void launch(Activity activity, Class<?> cls, Bundle extras, int requestCode) {
    try {
      Intent intent = new Intent(activity, cls);
      if (extras != null) {
        intent.putExtras(extras);
      }
      activity.startActivityForResult(intent, requestCode);
    } catch (Exception anfe) {
      report(anfe);
    }
  }

  public static void launch(Fragment fragment, Class<?> cls, Bundle extras, int requestCode) {
    try {
      Intent intent = new Intent(fragment.getActivity(), cls);
      if (extras != null) {
        intent.putExtras(extras);
      }
      fragment.startActivityForResult(intent, requestCode);
    } catch (Exception anfe) {
      report(anfe);
    }
  }

  public static Typeface getFont(Context context) {
    if (sFont == null) {
      sFont = Typeface.createFromAsset(context.getAssets(), "app_color_font.otf");
    }
    return sFont;
  }

}

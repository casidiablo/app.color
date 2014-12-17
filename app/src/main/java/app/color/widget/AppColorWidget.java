package app.color.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import app.color.R;
import app.color.service.OverlayService;

public class AppColorWidget extends AppWidgetProvider {
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    super.onUpdate(context, appWidgetManager, appWidgetIds);
    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_color_widget);
    ComponentName watchWidget = new ComponentName(context, AppColorWidget.class);

    remoteViews.setOnClickPendingIntent(R.id.app_icon_widget, getOverlayPendingIntent(context));
    appWidgetManager.updateAppWidget(watchWidget, remoteViews);
  }

  private PendingIntent getOverlayPendingIntent(Context context) {
    Intent intent = new Intent(context, OverlayService.class);
    intent.putExtra(OverlayService.SHOW_OVERLAY, true);
    intent.putExtra(OverlayService.FROM_WIDGET, true);
    return PendingIntent.getService(context, 0, intent, 0);
  }
}

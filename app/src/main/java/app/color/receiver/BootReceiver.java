package app.color.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import app.color.service.OverlayService;

public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    OverlayService.start(context, false);
  }
}

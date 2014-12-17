package app.color.activity;

import android.app.Activity;
import android.os.Bundle;
import app.color.service.OverlayService;

public class OverlayActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    OverlayService.start(this, true);
    finish();
  }
}

package app.color.view;

import android.content.Context;

import app.color.R;
import app.color.activity.SettingsActivity;
import app.color.utils.AppUtils;

public class OverflowOptions extends OptionsView {

  public static final int[] IDS = {R.string.open_settings, R.string.launch_play_store,
      R.string.help};

  public OverflowOptions(Context context, OnOptionFired onOptionFired) {
    super(context, onOptionFired);
    setup();
  }

  @Override
  protected int handleOptionClicked(int position) {
    int id = IDS[position];
    switch (id) {
      case R.string.open_settings:
        AppUtils.launch(getContext(), SettingsActivity.class);
        break;
      case R.string.launch_play_store:
        AppUtils.launchPlayStore(getContext(), null);
        break;
      case R.string.help:
        AppUtils.launchTutorial(getContext());
        break;
    }
    return EXIT;
  }

  @Override
  protected String[] getStrings() {
    Context context = getContext();
    if (context == null) {
      return new String[]{};
    }
    String[] strings = new String[IDS.length];
    for (int i = 0; i < IDS.length; i++) {
      int resId = IDS[i];
      strings[i] = context.getString(resId);
    }
    return strings;
  }
}

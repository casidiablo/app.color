package app.color.view;

import android.content.Context;
import android.view.View;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.SqlAdapter;

import java.util.HashSet;

import app.color.R;
import app.color.model.AppEntry;
import app.color.utils.AppUtils;
import app.color.utils.ColorHelper;

public class AppOptionsView extends OptionsView {
  final AppEntry mAppEntry;
  final int[] mStringsIds;

  public AppOptionsView(Context context, OnOptionFired optionFired, AppEntry appEntry) {
    super(context, optionFired);
    mAppEntry = appEntry;
    int markFavorite = mAppEntry.favorite ? R.string.unmark_favorite : R.string.mark_as_favorite;
    int hideApp = mAppEntry.hidden ? R.string.show_app : R.string.hide_app;
    mStringsIds = new int[]{R.string.launch_app, R.string.launch_in_play_store,
        R.string.register_gesture, markFavorite, hideApp, R.string.app_info, R.string.uninstall_app};

    setup();
  }

  @Override
  protected int handleOptionClicked(int position) {
    String entryId = mAppEntry.id;
    int id = mStringsIds[position];
    switch (id) {
      case R.string.launch_app:
        mOnOptionFired.onAppLaunched(mAppEntry);
        return EXIT;
      case R.string.launch_play_store:
        AppUtils.launchPlayStore(getContext(), mAppEntry);
        return EXIT;
      case R.string.uninstall_app:
        AppUtils.uninstall(getContext(), mAppEntry);
        return EXIT;
      case R.string.hide_app:
      case R.string.show_app: {
        mAppEntry.hidden = id == R.string.hide_app;
        SqlAdapter adapter = Persistence.getAdapter(getContext());
        adapter.update(mAppEntry, "_id = ?", new String[]{String.valueOf(entryId)});
        return UPDATE_ADAPTER;
      }
      case R.string.register_gesture:
        mOnOptionFired.onCreateGesture(mAppEntry);
        return DO_NOTHING;
      case R.string.app_info:
        AppUtils.launchAppInfo(getContext(), mAppEntry.packageName);
        return EXIT;
      case R.string.mark_as_favorite:
      case R.string.unmark_favorite:
        mAppEntry.favorite = id == R.string.mark_as_favorite;
        SqlAdapter adapter = Persistence.getAdapter(getContext());
        adapter.update(mAppEntry, "_id = ?", new String[]{String.valueOf(entryId)});
        return DO_NOTHING;
    }
    return DO_NOTHING;
  }

  @Override
  protected String[] getStrings() {
    String[] strings = new String[mStringsIds.length];
    for (int i = 0; i < mStringsIds.length; i++) {
      int resId = mStringsIds[i];
      if (resId == R.string.launch_app) {
        strings[i] = getContext().getString(resId, mAppEntry.label);
      } else {
        strings[i] = getContext().getString(resId);
      }
    }
    return strings;
  }

  @Override
  protected View getHeaderView() {
    HashSet<Integer> appColors = new HashSet<Integer>();
    for (int i = 0; i < ColorHelper.PALETTE_SIZE; i++) {
      if ((mAppEntry.colorDistribution & (0xf << (i * 4))) != 0) {
        appColors.add(i);
      }
    }

    ColorIndicatorView colorIndicatorView = new ColorIndicatorView(getContext());
    colorIndicatorView.updateColors(appColors);
    colorIndicatorView.setMinimumHeight((int) getResources().getDimension(R.dimen.color_indicator_height));
    return colorIndicatorView;
  }
}

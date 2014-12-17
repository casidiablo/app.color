
package app.color.activity;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.SqlAdapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import app.color.R;
import app.color.model.AppEntry;
import app.color.utils.AppUtils;
import app.color.utils.ColorHelper;
import app.color.utils.GestureManager;
import app.color.view.ColorProgressBar;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class GesturesListActivity extends ListActivity {
  private static final int STATUS_SUCCESS = 0;
  private static final int STATUS_CANCELLED = 1;
  private static final int STATUS_NO_STORAGE = 2;
  private static final int STATUS_NOT_LOADED = 3;

  private static final int MENU_ID_REMOVE = 2;

  private final Comparator<NamedGesture> mSorter = new Comparator<NamedGesture>() {
    public int compare(NamedGesture object1, NamedGesture object2) {
      if (object1.label == null && object2.label == null) {
        return 0;
      }
      if (object1.label == null) {
        return -1;
      }
      return object1.label.compareToIgnoreCase(object2.label);
    }
  };

  private GesturesAdapter mAdapter;
  private GesturesLoadTask mTask;

  @InjectView(android.R.id.empty) TextView mEmpty;
  @InjectView(R.id.progress_bar) ColorProgressBar mColorProgressBar;
  @InjectView(R.id.app_name) TextView appName;

  private GestureManager mGestureManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.gestures_list);
    ButterKnife.inject(this);

    appName.setText(R.string.app_gestures);
    appName.setTypeface(AppUtils.getFont(this));

    mGestureManager = GestureManager.getInstance(GesturesListActivity.this);

    mAdapter = new GesturesAdapter(this);
    setListAdapter(mAdapter);

    loadGestures();

    mColorProgressBar.setProgress(100);
    mColorProgressBar.mBackColor = getResources().getColor(R.color.flat_bg);

    registerForContextMenu(getListView());
  }

  private void loadGestures() {
    if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
      mTask.cancel(true);
    }
    mTask = (GesturesLoadTask) new GesturesLoadTask().execute();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
      mTask.cancel(true);
      mTask = null;
    }
  }

  private void checkForEmpty() {
    if (mAdapter.getCount() == 0) {
      mEmpty.setText(R.string.no_gestures);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
                                  ContextMenu.ContextMenuInfo menuInfo) {

    super.onCreateContextMenu(menu, v, menuInfo);

    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    menu.setHeaderTitle(((TextView) info.targetView).getText());

    menu.add(0, MENU_ID_REMOVE, 0, R.string.gestures_delete);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)
        item.getMenuInfo();
    final NamedGesture gesture = (NamedGesture) menuInfo.targetView.getTag();

    switch (item.getItemId()) {
      case MENU_ID_REMOVE:
        deleteGesture(gesture);
        return true;
    }

    return super.onContextItemSelected(item);
  }

  private void deleteGesture(NamedGesture gesture) {
    mGestureManager.delete(gesture.name, gesture.gesture);

    final GesturesAdapter adapter = mAdapter;
    adapter.setNotifyOnChange(false);
    adapter.remove(gesture);
    adapter.sort(mSorter);
    checkForEmpty();
    adapter.notifyDataSetChanged();
    Toast.makeText(this, R.string.gestures_delete_success, Toast.LENGTH_SHORT).show();
  }

  private class GesturesLoadTask extends AsyncTask<Void, NamedGesture, java.lang.Integer> {
    private int mThumbnailSize;
    private int mThumbnailInset;
    private int mPathColor;

    @Override
    protected void onPreExecute() {
      super.onPreExecute();

      final Resources resources = getResources();
      mPathColor = ColorHelper.getFlatColor(ColorHelper.RED_INDEX);
      mThumbnailInset = (int) resources.getDimension(R.dimen.gesture_thumbnail_inset);
      mThumbnailSize = (int) resources.getDimension(R.dimen.gesture_thumbnail_size);

      mAdapter.setNotifyOnChange(false);
      mAdapter.clear();
    }

    @Override
    protected Integer doInBackground(Void... params) {
      if (isCancelled()) return STATUS_CANCELLED;
      if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        return STATUS_NO_STORAGE;
      }

      SqlAdapter adapter = Persistence.getAdapter(GesturesListActivity.this);
      if (mGestureManager.isWorking()) {
        for (String name : mGestureManager.getGestureEntries()) {
          if (isCancelled()) break;
          for (Gesture gesture : mGestureManager.getGestures(name)) {
            final Bitmap bitmap = gesture.toBitmap(mThumbnailSize, mThumbnailSize,
                mThumbnailInset, mPathColor);
            final NamedGesture namedGesture = new NamedGesture();
            namedGesture.gesture = gesture;
            AppEntry foundApp = adapter.findFirst(AppEntry.class, "_id = ?", new String[]{name});
            namedGesture.name = name;
            if (foundApp != null) {
              namedGesture.label = foundApp.label;
            }

            mAdapter.addBitmap(namedGesture.gesture.getID(), bitmap);
            publishProgress(namedGesture);
          }
        }

        return STATUS_SUCCESS;
      }

      return STATUS_NOT_LOADED;
    }

    @Override
    protected void onProgressUpdate(NamedGesture... values) {
      super.onProgressUpdate(values);
      final GesturesAdapter adapter = mAdapter;
      adapter.setNotifyOnChange(false);

      for (NamedGesture gesture : values) {
        adapter.add(gesture);
      }

      adapter.sort(mSorter);
      adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(Integer result) {
      super.onPostExecute(result);

      if (result == STATUS_NO_STORAGE) {
        getListView().setVisibility(View.GONE);
        mEmpty.setVisibility(View.VISIBLE);
        mEmpty.setText(getString(R.string.gestures_error_loading));
      } else {
        checkForEmpty();
      }
    }
  }

  static class NamedGesture {
    String name;
    String label;
    Gesture gesture;
  }

  private class GesturesAdapter extends ArrayAdapter<NamedGesture> {
    private final LayoutInflater mInflater;
    private final Map<Long, Drawable> mThumbnails = Collections.synchronizedMap(
        new HashMap<Long, Drawable>());

    public GesturesAdapter(Context context) {
      super(context, 0);
      mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    void addBitmap(Long id, Bitmap bitmap) {
      mThumbnails.put(id, new BitmapDrawable(getResources(), bitmap));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = mInflater.inflate(R.layout.gestures_item, parent, false);
      }

      final NamedGesture gesture = getItem(position);
      final TextView label = (TextView) convertView;

      label.setTag(gesture);
      label.setText(gesture.label);
      label.setCompoundDrawablesWithIntrinsicBounds(mThumbnails.get(gesture.gesture.getID()),
          null, null, null);

      return convertView;
    }
  }
}

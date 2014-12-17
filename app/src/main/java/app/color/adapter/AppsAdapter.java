package app.color.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.java.util.ArrayDeque;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.SqlAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.color.R;
import app.color.glue.ToggleFiltersState;
import app.color.model.AppEntry;
import app.color.model.GlobalAppLog;
import app.color.model.UserPrefs;
import app.color.utils.BitmapLruCache;
import app.color.utils.ColorHelper;
import app.color.utils.Utilities;
import app.color.view.FilterView;
import app.color.view.PlayGroundView;

import static app.color.R.string.empty_exact_plural;
import static app.color.R.string.empty_exact_single;
import static app.color.R.string.empty_search;

public class AppsAdapter extends BaseAdapter implements SectionIndexer {

  public static final int ORDER_ALPHA = 0;
  public static final int ORDER_USAGE = 1;
  private static final long INTERVAL_UPDATE = 3600000;
  final List<AppEntry> mApps;
  final Context mContext;
  final ArrayDeque<Integer> mFilters;
  final PlayGroundView.TextFilter mTextFilter;
  private UserPrefs mUserPrefs;
  final List<AppEntry> mFilteredApps = new ArrayList<AppEntry>();
  final Map<String, Integer> mGlobalAppLog = new HashMap<String, Integer>();
  final SparseIntArray mSectionsMappings = new SparseIntArray();
  final BitmapLruCache mCache;
  final ForegroundColorSpan mHighlightColor;
  private final ToggleFiltersState mFilterChecker;
  private int mCurrentSorting = ORDER_ALPHA;
  private long mLastUpdatedTime;
  private int mNumColumns = PlayGroundView.NUM_COLUMNS_PORTRAIT;
  private final EmptyListener mEmptyListener;

  public AppsAdapter(Context context, ArrayDeque<Integer> filters, List<AppEntry> apps,
                     PlayGroundView.TextFilter textFilter, ToggleFiltersState filterChecker,
                     UserPrefs userPrefs, EmptyListener emptyListener) {
    mFilterChecker = filterChecker;
    mContext = context;
    mFilters = filters;
    mTextFilter = textFilter;
    mUserPrefs = userPrefs;
    mEmptyListener = emptyListener;
    mCache = new BitmapLruCache(context);
    mApps = filterHiddenApps(apps);
    sort(mApps);
    mHighlightColor = new ForegroundColorSpan(context.getResources().getColor(R.color.highlight_color));
  }

  @Override
  public int getCount() {
    int size = getRealCount();
    int fillRow = mNumColumns - (size % mNumColumns);
    if (fillRow == mNumColumns) {
      fillRow = 0;
    }
    return size + mNumColumns + fillRow;
  }

  private int getRealCount() {
    boolean filterApplied = filterApplied();
    return filterApplied ? mFilteredApps.size() : mApps.size();
  }

  @Override
  public AppEntry getItem(int position) {
    List<AppEntry> getFrom = filterApplied() ? mFilteredApps : mApps;
    if (position >= getFrom.size()) {
      return null;
    }
    return getFrom.get(position);
  }

  @Override
  public long getItemId(int position) {
    AppEntry item = getItem(position);
    if (item == null) {
      return 0;
    }
    return item.id.hashCode();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final ViewHolder holder;
    if (convertView == null) {
      convertView = LayoutInflater.from(mContext).inflate(R.layout.app_item_grid, null);

      holder = new ViewHolder();
      holder.label = (TextView) convertView.findViewById(R.id.app_label);
      holder.icon = (ImageView) convertView.findViewById(R.id.app_icon);
      holder.placeHolder = convertView.findViewById(R.id.placeholder);

      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    final AppEntry appEntry = getItem(position);
    if (appEntry == null) {
      if (position >= getCount() - (mNumColumns * 2)) {
        holder.icon.setVisibility(View.GONE);
        holder.label.setVisibility(View.GONE);
        holder.placeHolder.setVisibility(View.VISIBLE);
      } else {
        holder.icon.setVisibility(View.VISIBLE);
        holder.icon.setImageBitmap(null);
        holder.label.setVisibility(View.VISIBLE);
        holder.label.setText("");
        holder.placeHolder.setVisibility(View.GONE);
      }
      return convertView;
    }
    holder.icon.setVisibility(View.VISIBLE);
    holder.label.setVisibility(View.VISIBLE);
    holder.placeHolder.setVisibility(View.GONE);

    String currentFilter = mTextFilter.getCurrentFilter();
    if (!TextUtils.isEmpty(currentFilter)) {
      int index = appEntry.label.toLowerCase().indexOf(currentFilter.toLowerCase());
      if (index >= 0) {
        Spannable colorfulLabel = new SpannableString(appEntry.label);
        colorfulLabel.setSpan(mHighlightColor, index, index + currentFilter.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.label.setText(colorfulLabel);
      }
    }

    Bitmap icon = getIconFor(appEntry);
    holder.icon.setImageBitmap(icon);
    holder.label.setText(appEntry.label);


    return convertView;
  }

  public Bitmap getIconFor(AppEntry appEntry) {
    String iconId = appEntry.id;
    Bitmap icon = mCache.get(iconId);
    if (icon == null) {
      boolean notInCache = false;
      Bitmap appIcon = Utilities.retrieveIcon(mContext, iconId);
      if (appIcon == null) {
        notInCache = true;
        appIcon = Utilities.resolveIcon(mContext, appEntry);
      }
      if (appIcon == null) {
        Log.wtf("adapter", "App icon for " + appEntry.label + " is null", new NullPointerException());
        appIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);
      }
      icon = Utilities.createBitmapThumbnail(mContext, appIcon);
      if (icon != null) {
        if (notInCache) {
          Utilities.saveIcon(mContext, iconId, icon);
        }
        mCache.set(iconId, icon);
      }
    }
    return icon;
  }

  private boolean filterApplied() {
    return !mFilteredApps.isEmpty() || !TextUtils.isEmpty(mTextFilter.getCurrentFilter())
        || mFilterChecker.exactFilterEnabled();
  }

  public void updateFilteredApps() {
    final ArrayList<AppEntry> filteredApps = new ArrayList<AppEntry>();

    for (AppEntry app : new ArrayList<AppEntry>(mApps)) {
      String currentFilter = mTextFilter.getCurrentFilter();
      if (!TextUtils.isEmpty(currentFilter) && !app.label.toLowerCase().contains(currentFilter.toLowerCase())) {
        continue;
      }

      ArrayList<Integer> colorFilters = new ArrayList<Integer>(mFilters);
      boolean includeApp;
      if (mFilterChecker.exactFilterEnabled()) {
        includeApp = hasExactlyTheseColors(app, colorFilters);
      } else {
        includeApp = hasSomeColors(app, colorFilters);
      }
      if (includeApp) {
        filteredApps.add(app);
      }
    }
    sort(filteredApps);

    synchronized (mFilteredApps) {
      mFilteredApps.clear();
      mFilteredApps.addAll(filteredApps);
    }
  }

  private boolean hasSomeColors(AppEntry app, ArrayList<Integer> colorFilters) {
    boolean hasAllColors = true;
    for (Integer colorFilter : colorFilters) {
      if (!appHasColor(app, colorFilter)) {
        hasAllColors = false;
        break;
      }
    }
    return hasAllColors;
  }

  private static boolean hasExactlyTheseColors(AppEntry app, List<Integer> colors) {
    int colorValue = 0;
    for (Integer color : colors) {
      colorValue |= (0xF << (4 * color));
    }
    return colorValue == app.colorDistribution;
  }

  private static boolean appHasColor(AppEntry app, int colorIndex) {
    final int colorValue = app.colorDistribution & (0xF << (4 * colorIndex));
    return colorValue != 0;
  }

  public Set<Integer> getColorsToIgnore() {
    final HashSet<Integer> indexesToRemove = new HashSet<Integer>(ColorHelper.INDEXES);
    indexesToRemove.removeAll(mFilters);

    // if already has maximum amount of filters or just one app, ignore all
    if (mFilters.size() == 4 || mFilteredApps.size() == 1) {
      return indexesToRemove;
    }

    for (Integer colorIndex : ColorHelper.INDEXES) {
      if (!mFilters.contains(colorIndex)) {
        for (AppEntry filteredApp : new ArrayList<AppEntry>(mFilteredApps)) {
          if (appHasColor(filteredApp, colorIndex)) {
            indexesToRemove.remove(colorIndex);
          }
        }
      }
    }

    return indexesToRemove;
  }

  @Override
  public Object[] getSections() {
    List<AppEntry> apps;
    if (mFilteredApps.isEmpty()) {
      apps = mApps;
    } else {
      apps = mFilteredApps;
    }

    mSectionsMappings.clear();

    int x = 0;
    char lastUsedLetter = '\0';
    for (int i = 0; i < apps.size(); i++) {
      AppEntry app = apps.get(i);
      char firstLetter = Character.toUpperCase(app.label.charAt(0));
      if (firstLetter != lastUsedLetter) {
        mSectionsMappings.put(x, i);
        lastUsedLetter = firstLetter;
        x++;
      }
    }

    Character[] sectionsArray = new Character[mSectionsMappings.size()];
    for (int i = 0; i < mSectionsMappings.size(); i++) {
      int location = mSectionsMappings.get(i);
      sectionsArray[i] = Character.toUpperCase(apps.get(location).label.charAt(0));
    }
    return sectionsArray;
  }

  @Override
  public int getPositionForSection(int section) {
    return mSectionsMappings.get(section);
  }

  @Override
  public int getSectionForPosition(int position) {
    for (int i = 0; i < mSectionsMappings.size(); i++) {
      int positionFromMapping = mSectionsMappings.get(i);
      if (positionFromMapping <= position) {
        return positionFromMapping;
      }
    }
    return 0;
  }

  public void updateApps(List<AppEntry> apps) {
    List<AppEntry> filteredApps = filterHiddenApps(apps);
    synchronized (mApps) {
      mApps.clear();
      mApps.addAll(filteredApps);
      sort(mApps);
      notifyDataSetChanged();
      updateFilteredApps();
    }
  }

  public AppEntry findByPackage(String pkg) {
    if (TextUtils.isEmpty(pkg)) {
      return null;
    }
    //noinspection StatementWithEmptyBody
    for (AppEntry app : mApps) {
      if (app.packageName.equals(pkg)) {
        return app;
      }
    }

    return null;
  }

  private List<AppEntry> filterHiddenApps(List<AppEntry> apps) {
    if (mUserPrefs == null || mUserPrefs.showHiddenApps) {
      return apps;
    }
    List<AppEntry> filteredApps = new ArrayList<AppEntry>();
    for (AppEntry app : apps) {
      if (!app.hidden) {
        filteredApps.add(app);
      }
    }
    return filteredApps;
  }

  private void sort(List<AppEntry> apps) {
    switch (mCurrentSorting) {
      case ORDER_ALPHA:
        Collections.sort(apps, ALPHA_COMPARATOR);
        break;
      case ORDER_USAGE:
        Collections.sort(apps, USAGE_COMPARATOR);
        break;
    }
  }

  public void sortByAlpha() {
    if (mCurrentSorting == ORDER_ALPHA) {
      return;
    }

    mCurrentSorting = ORDER_ALPHA;
    Collections.sort(mApps, ALPHA_COMPARATOR);
    Collections.sort(mFilteredApps, ALPHA_COMPARATOR);
    notifyDataSetChanged();
  }

  @Override
  public void notifyDataSetChanged() {
    super.notifyDataSetChanged();
    if (getRealCount() != 0) {
      mEmptyListener.adapterIsNotEmpty();
      return;
    }

    String state, action;
    final String currentSearch = mTextFilter.getCurrentFilter();
    if (currentSearch != null) {
      state = mContext.getString(empty_search, currentSearch);
      action = currentSearch;
    } else {
      if (mFilters.size() == 1) {
        state = mContext.getString(empty_exact_single);
      } else {
        state = mContext.getString(empty_exact_plural);
      }
      action = EmptyListener.CLEAR_EXACT;
    }

    mEmptyListener.adapterIsEmpty(state, action);
  }

  public interface EmptyListener {
    String CLEAR_FILTER = "app.color.clear_filter";
    String CLEAR_EXACT = "app.color.clear_exact";

    void adapterIsEmpty(String state, String action);

    void adapterIsNotEmpty();
  }

  public void sortByUsage() {
    if (mCurrentSorting == ORDER_USAGE) {
      return;
    }

    mCurrentSorting = ORDER_USAGE;

    if (shouldUpdateUsageDb()) {
      SqlAdapter persistence = Persistence.getAdapter(mContext);

      List<GlobalAppLog> globalAppLog = persistence.findAll(GlobalAppLog.class);
      mGlobalAppLog.clear();
      for (GlobalAppLog appLog : globalAppLog) {
        appendCount(appLog.packageName, appLog.count);
      }

      for (AppEntry appEntry : mApps) {
        String pkgName = appEntry.packageName;
        int count = appEntry.launchCounts;
        appendCount(pkgName, count);
      }

      mLastUpdatedTime = System.currentTimeMillis();
    }

    Collections.sort(mApps, USAGE_COMPARATOR);
    Collections.sort(mFilteredApps, USAGE_COMPARATOR);

    notifyDataSetChanged();
  }

  private void appendCount(String pkgName, int count) {
    if (mGlobalAppLog.containsKey(pkgName)) {
      mGlobalAppLog.put(pkgName, mGlobalAppLog.get(pkgName) + count);
    } else {
      mGlobalAppLog.put(pkgName, count);
    }
  }

  private boolean shouldUpdateUsageDb() {
    return mLastUpdatedTime + INTERVAL_UPDATE < System.currentTimeMillis();
  }

  public void setNumColumns(int numColumns) {
    mNumColumns = numColumns;
    notifyDataSetChanged();
  }

  private static class ViewHolder {
    ImageView icon;
    TextView label;
    View placeHolder;
  }

  static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
    @Override
    public int compare(AppEntry foo, AppEntry bar) {
      return foo.label.compareToIgnoreCase(bar.label);
    }
  };

  final Comparator<AppEntry> USAGE_COMPARATOR = new Comparator<AppEntry>() {
    @Override
    public int compare(AppEntry foo, AppEntry bar) {
      boolean containsFoo = mGlobalAppLog.containsKey(foo.packageName);
      boolean containsBar = mGlobalAppLog.containsKey(bar.packageName);
      if (containsFoo && containsBar) {
        int sort = mGlobalAppLog.get(bar.packageName) - mGlobalAppLog.get(foo.packageName);
        if (sort == 0) {
          return ALPHA_COMPARATOR.compare(foo, bar);
        }
        return sort;
      }
      if (containsFoo) {
        return -1;
      }
      if (containsBar) {
        return 1;
      }
      return ALPHA_COMPARATOR.compare(foo, bar);
    }
  };

  public void updateUserPrefs(UserPrefs userPrefs) {
    mUserPrefs = userPrefs;
  }
}

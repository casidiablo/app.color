package app.color.core;

import android.app.Application;

import com.codeslap.persistence.DatabaseSpec;
import com.codeslap.persistence.PersistenceConfig;
import com.crashlytics.android.Crashlytics;

import app.color.BuildConfig;
import app.color.cache.CacheService;
import app.color.cache.GenericCacheService;
import app.color.model.AppEntry;
import app.color.model.AppLog;
import app.color.model.GlobalAppLog;
import app.color.model.Sec;
import app.color.model.UserPrefs;
import timber.log.Timber;

public class AppColor extends Application {
  public static final String PREFS = "user_prefs";
  private final CacheService<CacheService> mCachePool = new GenericCacheService<CacheService>(CacheService.class);
  public static final String CURRENT_USER = "app.color.CURRENT_USER";

  @Override
  public void onCreate() {
    super.onCreate();
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }
    Crashlytics.start(this);

    final DatabaseSpec db = PersistenceConfig.registerSpec(1);
    db.match(AppEntry.class, AppLog.class, GlobalAppLog.class);

    PersistenceConfig.getPreference(PREFS).match(UserPrefs.class, Sec.class);
  }

  /**
   * @param type the type of the cache
   * @return a cache service that holds objects of the specified type only
   */
  public <T> CacheService<T> getCache(final Class<T> type) {
    String id = "cache-impl:" + type.getName();
    CacheService cache = mCachePool.get(id);
    if (cache != null) {
      //noinspection unchecked
      return (CacheService<T>) cache;
    }
    CacheService<T> cacheService = new GenericCacheService<T>(type);
    mCachePool.save(id, cacheService);
    return cacheService;
  }
}

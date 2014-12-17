package app.color.cache;

import android.content.Context;

import app.color.core.AppColor;

public class CacheUtils {
  /**
   * Shortcut to get a cache service instance
   */
  public static <T> CacheService<T> getCache(Context context, Class<T> type) {
    AppColor app = (AppColor) context.getApplicationContext();
    return app.getCache(type);
  }

  /**
   * Shortcut to get an object from cache service
   */
  public static <T> T get(Context context, Class<T> type, String id) {
    return getCache(context, type).get(id);
  }

  public static <T> void put(Context context, String id, T object) {
    if (object == null) {
      throw new NullPointerException();
    }

    //noinspection unchecked
    Class<T> aClass = (Class<T>) object.getClass();
    CacheService<T> cache = getCache(context, aClass);
    cache.save(id, object);
  }
}

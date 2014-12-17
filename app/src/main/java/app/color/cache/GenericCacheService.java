package app.color.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class GenericCacheService<T> implements CacheService<T> {
  private final Map<String, T> mPool = Collections.synchronizedSortedMap(new TreeMap<String, T>());

  @SuppressWarnings("UnusedParameters")
  public GenericCacheService(Class<T> type) {
  }

  @Override
  public T get(String id) {
    if (id == null) {
      return null;
    }
    try {
      return mPool.get(id);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Collection<T> getAll() {
    return new ArrayList<T>(mPool.values());
  }

  @Override
  public void save(String id, T object) {
    if (id != null && object != null) {
      mPool.put(id, object);
    }
  }

  @Override
  public void remove(String id) {
    if (id == null) {
      return;
    }
    mPool.remove(id);
  }

  @Override
  public void clear() {
    mPool.clear();
  }

}

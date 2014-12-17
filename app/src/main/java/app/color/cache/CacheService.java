package app.color.cache;

import java.util.Collection;

/**
 * Generic cache of objects.
 *
 * @param <T>
 */
public interface CacheService<T> {
  /**
   * @param id key to search in the cache
   * @return the cached object whose id is <code>id</code>. Null if it does not
   * exists or <code>id</code> is null.
   */
  T get(String id);

  /**
   * @return all the objects currently in the cache
   */
  Collection<T> getAll();

  /**
   * Saves an object in the cache
   *
   * @param id     unique object identifier
   * @param object the object to cache
   */
  void save(String id, T object);

  /**
   * Removes an object from the cache. Does nothing if id is null or not object found.
   *
   * @param id the identifier of the object to remove
   */
  void remove(String id);

  /**
   * Removes all objects from this cache pool
   */
  void clear();
}

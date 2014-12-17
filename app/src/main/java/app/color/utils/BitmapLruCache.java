package app.color.utils;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A memory cache which uses a least-recently used eviction policy.
 */
public class BitmapLruCache {
  final LinkedHashMap<String, Bitmap> map;
  private final int maxSize;

  private int size;
  private int putCount;
  private int evictionCount;
  private int hitCount;
  private int missCount;

  /**
   * Create a cache using an appropriate portion of the available RAM as the maximum size.
   */
  public BitmapLruCache(Context context) {
    this(BitmapUtils.calculateMemoryCacheSize(context));
  }

  /**
   * Create a cache with a given maximum size in bytes.
   */
  public BitmapLruCache(int maxSize) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Max size must be positive.");
    }
    this.maxSize = maxSize;
    this.map = new LinkedHashMap<String, Bitmap>(0, 0.75f, true);
  }

  public Bitmap get(String key) {
    if (key == null) {
      throw new NullPointerException("key == null");
    }

    Bitmap mapValue;
    synchronized (this) {
      mapValue = map.get(key);
      if (mapValue != null) {
        hitCount++;
        return mapValue;
      }
      missCount++;
    }

    return null;
  }

  public void set(String key, Bitmap bitmap) {
    if (key == null || bitmap == null) {
      throw new NullPointerException("key == null || bitmap == null");
    }

    Bitmap previous;
    synchronized (this) {
      putCount++;
      size += BitmapUtils.getBitmapBytes(bitmap);
      previous = map.put(key, bitmap);
      if (previous != null) {
        size -= BitmapUtils.getBitmapBytes(previous);
      }
    }

    trimToSize(maxSize);
  }

  private void trimToSize(int maxSize) {
    while (true) {
      String key;
      Bitmap value;
      synchronized (this) {
        if (size < 0 || (map.isEmpty() && size != 0)) {
          throw new IllegalStateException(
              getClass().getName() + ".sizeOf() is reporting inconsistent results!");
        }

        if (size <= maxSize || map.isEmpty()) {
          break;
        }

        Map.Entry<String, Bitmap> toEvict = map.entrySet().iterator().next();
        key = toEvict.getKey();
        value = toEvict.getValue();
        map.remove(key);
        size -= BitmapUtils.getBitmapBytes(value);
        evictionCount++;
      }
    }
  }

  /**
   * Clear the cache.
   */
  public final void evictAll() {
    trimToSize(-1); // -1 will evict 0-sized elements
  }

  /**
   * Returns the sum of the sizes of the entries in this cache.
   */
  public final synchronized int size() {
    return size;
  }

  /**
   * Returns the maximum sum of the sizes of the entries in this cache.
   */
  public final synchronized int maxSize() {
    return maxSize;
  }

  public final synchronized void clear() {
    evictAll();
  }

  /**
   * Returns the number of times {@link #get} returned a value.
   */
  public final synchronized int hitCount() {
    return hitCount;
  }

  /**
   * Returns the number of times {@link #get} returned {@code null}.
   */
  public final synchronized int missCount() {
    return missCount;
  }

  /**
   * Returns the number of times {@link #set(String, Bitmap)} was called.
   */
  public final synchronized int putCount() {
    return putCount;
  }

  /**
   * Returns the number of values that have been evicted.
   */
  public final synchronized int evictionCount() {
    return evictionCount;
  }
}

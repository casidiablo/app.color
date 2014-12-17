package app.color.model;

import app.color.utils.ColorHelper;
import com.codeslap.persistence.PrimaryKey;

public class AppEntry {
  @PrimaryKey
  public String id;
  public String packageName;
  public String label;
  public String className;
  public int colorDistribution;
  public boolean favorite;
  public int launchCounts;
  public String category;
  public int categoryRetries;
  public boolean hidden;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AppEntry appEntry = (AppEntry) o;

    if (colorDistribution != appEntry.colorDistribution) return false;
    if (favorite != appEntry.favorite) return false;
    if (launchCounts != appEntry.launchCounts) return false;
    if (category != null ? !category.equals(appEntry.category) : appEntry.category != null) return false;
    if (className != null ? !className.equals(appEntry.className) : appEntry.className != null) return false;
    if (id != null ? !id.equals(appEntry.id) : appEntry.id != null) return false;
    if (label != null ? !label.equals(appEntry.label) : appEntry.label != null) return false;
    if (packageName != null ? !packageName.equals(appEntry.packageName) : appEntry.packageName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
    result = 31 * result + (label != null ? label.hashCode() : 0);
    result = 31 * result + (className != null ? className.hashCode() : 0);
    result = 31 * result + colorDistribution;
    result = 31 * result + (favorite ? 1 : 0);
    result = 31 * result + launchCounts;
    result = 31 * result + (category != null ? category.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder colors = new StringBuilder();
    String separator = "";
    for (int i = 0; i < ColorHelper.PALETTE_SIZE; i++) {
      final int fifteentage = colorDistribution & (0xf << (4 * i));
      if (fifteentage != 0) {
        colors.append(separator);
        colors.append(ColorHelper.getColorName(i)).append(" => ").append(fifteentage >> (4 * i));
        separator = ", ";
      }
    }
    return label + "[" + colors + "] {" + category + "}";
  }
}

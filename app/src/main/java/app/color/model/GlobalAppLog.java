package app.color.model;

import com.codeslap.persistence.PrimaryKey;

public class GlobalAppLog {
  @PrimaryKey
  public long id;
  public String packageName;
  public long date;
  public int count;


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GlobalAppLog that = (GlobalAppLog) o;

    if (count != that.count) return false;
    if (date != that.date) return false;
    if (id != that.id) return false;
    if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
    result = 31 * result + (int) (date ^ (date >>> 32));
    result = 31 * result + count;
    return result;
  }

  @Override
  public String toString() {
    return "GlobalAppLog{" +
        "id=" + id +
        ", packageName='" + packageName + '\'' +
        ", date=" + date +
        ", count=" + count +
        '}';
  }
}

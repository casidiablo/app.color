package app.color.model;

import com.codeslap.persistence.PrimaryKey;

public class AppLog {
  @PrimaryKey
  public long id;
  public String appId;
  public long launchTime;
  public int filterUsed;
  public long elapsedTime;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AppLog appLog = (AppLog) o;

    if (elapsedTime != appLog.elapsedTime) return false;
    if (filterUsed != appLog.filterUsed) return false;
    if (id != appLog.id) return false;
    if (launchTime != appLog.launchTime) return false;
    if (appId != null ? !appId.equals(appLog.appId) : appLog.appId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (appId != null ? appId.hashCode() : 0);
    result = 31 * result + (int) (launchTime ^ (launchTime >>> 32));
    result = 31 * result + filterUsed;
    result = 31 * result + (int) (elapsedTime ^ (elapsedTime >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "AppLog{" +
        "id=" + id +
        ", appId='" + appId + '\'' +
        ", launchTime=" + launchTime +
        ", filterUsed=" + filterUsed +
        ", elapsedTime=" + elapsedTime +
        '}';
  }
}

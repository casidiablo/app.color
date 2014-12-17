package app.color.model;

import com.codeslap.persistence.pref.Preference;

public class UserPrefs {

  @Preference(value = "show_notification", defaultValue = "false")
  public boolean showNotification;

  @Preference(value = "haptic_feedback", defaultValue = "false")
  public boolean hapticFeedback;

  @Preference(value = "enabled", defaultValue = "true")
  public boolean enabled;

  @Preference(value = "sensitivity", defaultValue = "20")
  public int sensitivity;

  @Preference(value = "show_active_area", defaultValue = "true")
  public boolean showActiveArea;

  @Preference(value = "active_area_height", defaultValue = "30")
  public int activeAreaHeight;

  @Preference(value = "active_area_right", defaultValue = "true")
  public boolean activeAreaRight;

  @Preference(value = "handle_gravity", defaultValue = "48") // 48 = Gravity.TOP
  public int handleGravity;

  @Preference(value = "tap_to_select", defaultValue = "false")
  public boolean tapToSelectColor;

  @Preference(value = "virgin", defaultValue = "true")
  public boolean virgin;

  @Preference(value = "show_hidden_apps", defaultValue = "false")
  public boolean showHiddenApps;
}

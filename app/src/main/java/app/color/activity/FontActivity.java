package app.color.activity;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import app.color.utils.AppUtils;

public class FontActivity extends Activity {
  protected void setupFont(int viewGroupId) {
    Typeface font = AppUtils.getFont(this);
    ViewGroup viewGroup = (ViewGroup) findViewById(viewGroupId);
    applyFontTo(font, viewGroup);
  }

  private void applyFontTo(Typeface font, ViewGroup viewGroup) {
    for (int i = 0; i < viewGroup.getChildCount(); i++) {
      View childAt = viewGroup.getChildAt(i);
      if (childAt instanceof TextView) {
        TextView textView = (TextView) childAt;
        textView.setTypeface(font);
      } else if (childAt instanceof ViewGroup) {
        applyFontTo(font, (ViewGroup) childAt);
      }
    }
  }
}

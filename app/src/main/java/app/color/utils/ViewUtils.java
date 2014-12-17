package app.color.utils;

import android.text.Editable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class ViewUtils {
  public static void setVisible(View view, boolean visible) {
    if (view != null) {
      view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
  }

  public static void enable(View view, boolean enabled) {
    if (view != null) {
      view.setEnabled(enabled);
    }
  }

  public static void hideKbd(EditText view) {
    if (view != null) {
      InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }

  public static void showKbd(View view) {
    if (view != null) {
      InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(INPUT_METHOD_SERVICE);
      imm.showSoftInput(view, 0);
    }
  }

  public static String contents(EditText title) {
    if (title == null) {
      return "";
    }
    Editable text = title.getText();
    if (text == null) {
      return "";
    }
    return text.toString();
  }
}

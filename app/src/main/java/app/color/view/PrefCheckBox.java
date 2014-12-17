package app.color.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckedTextView;

public class PrefCheckBox extends CheckedTextView {

  public OnCheckedChangeListener mCheckListener;

  public PrefCheckBox(Context context) {
    this(context, null);
  }

  public PrefCheckBox(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PrefCheckBox(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        toggle();
        if (mCheckListener != null) {
          mCheckListener.onCheckedChanged(getId(), isChecked());
        }
      }
    });
  }

  /**
   * Interface definition for a callback to be invoked when the checked state
   * of a the checkbox changed.
   */
  public static interface OnCheckedChangeListener {
    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param id        item id
     * @param isChecked The new checked state of buttonView.
     */
    void onCheckedChanged(int id, boolean isChecked);
  }
}

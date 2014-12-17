package app.color.view;

import android.content.Context;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import app.color.model.AppEntry;
import app.color.R;
import app.color.utils.AppUtils;

public abstract class OptionsView extends FrameLayout {
  public static final int EXIT = 0;
  public static final int DO_NOTHING = 1;
  public static final int UPDATE_ADAPTER = 2;

  protected final OnOptionFired mOnOptionFired;

  public OptionsView(Context context, OnOptionFired onOptionFired) {
    super(context);
    mOnOptionFired = onOptionFired;

    setFocusable(true);
    setFocusableInTouchMode(true);
    requestFocus();

    inflate(context, R.layout.options_list, this);
  }

  protected void setup() {
    String[] strings = getStrings();

    final Typeface font = AppUtils.getFont(getContext());

    final ListView listView = (ListView) findViewById(R.id.list_view);
    View headerView = getHeaderView();
    if (headerView != null) {
      listView.addHeaderView(headerView);
    }

    listView.setAdapter(new ArrayAdapter<String>(getContext(), R.layout.list_item, R.id.item, strings) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView itemText = (TextView) view.findViewById(R.id.item);
        itemText.setTypeface(font);
        return view;
      }
    });
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        int whatsNext = handleOptionClicked(position - listView.getHeaderViewsCount());
        mOnOptionFired.optionFired(OptionsView.this, whatsNext);
      }
    });

    setClickable(true);
    setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        mOnOptionFired.optionFired(OptionsView.this, OptionsView.DO_NOTHING);
      }
    });
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      if (event.getAction() == KeyEvent.ACTION_UP) {
        mOnOptionFired.optionFired(this, OptionsView.DO_NOTHING);
      }
      return true;
    }
    return super.dispatchKeyEvent(event);
  }

  protected View getHeaderView() {
    return null;
  }

  /**
   * @param position the clicked position
   * @return one of these: {@link app.color.view.OptionsView#EXIT},
   * {@link app.color.view.OptionsView#UPDATE_ADAPTER}, {@link app.color.view.OptionsView#DO_NOTHING}.
   */
  protected abstract int handleOptionClicked(int position);

  /**
   * @return an array of options
   */
  protected abstract String[] getStrings();

  public interface OnOptionFired {

    void onAppLaunched(AppEntry app);

    void optionFired(OptionsView view, int whatsNext);

    void onCreateGesture(AppEntry appEntry);
  }
}

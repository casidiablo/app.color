package app.color.view;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import app.color.R;

public class HelpDialog extends DialogFragment {

  static final String HELP_ID = "app.color.arg.HELP_ID";

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    InternalDialog internalDialog = new InternalDialog(getActivity());
    int help = getArguments().getInt(HELP_ID);
    String[] stringArray = getResources().getStringArray(help);
    internalDialog.setTitle(stringArray[0]);
    internalDialog.setContent(stringArray[1]);
    return internalDialog;
  }

  public static HelpDialog show(FragmentManager fm, int helpId) {
    HelpDialog helpDialog = new HelpDialog();
    Bundle args = new Bundle();
    args.putInt(HELP_ID, helpId);
    helpDialog.setArguments(args);
    helpDialog.show(fm, HelpDialog.class.getName());
    return helpDialog;
  }

  static class InternalDialog extends Dialog {

    private final TextView mTitle;
    private final TextView mContent;

    public InternalDialog(Context context) {
      super(context, R.style.FlatDialog);
      setContentView(R.layout.help_dialog);

      mTitle = (TextView) findViewById(R.id.tittle);
      mContent = (TextView) findViewById(R.id.content);
    }

    @Override
    public void setTitle(CharSequence title) {
      mTitle.setText(title);
    }

    @Override
    public void setTitle(int title) {
      mTitle.setText(title);
    }

    public void setContent(String content) {
      mContent.setText(content);
    }
  }
}

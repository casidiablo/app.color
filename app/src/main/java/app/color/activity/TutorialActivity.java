package app.color.activity;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.color.R;
import app.color.glue.AnimationListenerAdapter;
import app.color.paint.AppColorScreenLayer;
import app.color.paint.BackgroundScreenLayer;
import app.color.paint.OverlayScreenLayer;
import app.color.utils.AppUtils;
import app.color.view.CellPhoneView;
import app.color.view.ColorProgressBar;
import app.color.view.FingerView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class TutorialActivity extends Activity {

  static final int LAUNCH_APP_COLOR = 0;
  static final int FIRST_DEMO = 1;
  static final int SECOND_DEMO = 2;
  static final int THIRD_DEMO = 3;
  static final int CLEAR_AND_RELAUNCH = 10;
  static final int LAUNCH_APP_COLOR_OVERLAY = 4;

  static final int LAUNCH_INTERVAL = 2000;
  static final int LAUNCH_OVERLAY_DELAY = 32;
  static final int LAUNCH_AFTER_CLEANING = 1000;
  static final int REPEAT_INTERVAL = 4000;
  private static final int[] TUTORIAL_LABELS = {R.string.swipe_explanation, R.string.filter_explanation,
      R.string.filter_explanation2, R.string.filter_explanation3};
  private OverlayScreenLayer mOverlayLayer;

  private AppColorScreenLayer mAppColorLayer;
  private int mCurrentTutorial = 0;

  @InjectView(R.id.phone) CellPhoneView mCellPhoneView;
  @InjectView(R.id.progress_bar) ColorProgressBar mColorProgressBar;
  @InjectView(R.id.tutorial_text) TextView mTutorialLabel;
  @InjectView(R.id.previous) View mPreviousBtn;
  @InjectView(R.id.next) View mNextBtn;
  @InjectView(R.id.finger) FingerView mFinger;

  private int mLastMessage = Integer.MIN_VALUE;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tutorial_activity);
    ButterKnife.inject(this);

    mColorProgressBar.setProgress(100);
    mColorProgressBar.mBackColor = getResources().getColor(R.color.flat_bg);

    Typeface font = AppUtils.getFont(this);

    mTutorialLabel.setTypeface(font);
    mTutorialLabel.setText(TUTORIAL_LABELS[0]);

    mCellPhoneView.addScreenLayer(new BackgroundScreenLayer(this));

    mOverlayLayer = new OverlayScreenLayer(mCellPhoneView);
    mAppColorLayer = new AppColorScreenLayer(TutorialActivity.this);

    mFinger.setSwipeListener(new FingerView.SwipeListener() {
      @Override
      public void onSwipe(boolean toLeft) {
        if (canMoveTo(toLeft)) {
          moveToTutorial(toLeft);
        }
      }
    });
    mTutorialHandler.sendEmptyMessageDelayed(LAUNCH_APP_COLOR, LAUNCH_INTERVAL);

    ViewTreeObserver vtobs = mCellPhoneView.getViewTreeObserver();
    if (vtobs != null) {
      vtobs.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          int[] location = new int[2];
          mCellPhoneView.getLocationOnScreen(location);
          mFinger.setTouchBoundaries(location);
        }
      });
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    cancelTutorialMessages();
    mInternalHandler.removeMessages(LAUNCH_APP_COLOR_OVERLAY);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mLastMessage != Integer.MIN_VALUE) {
      mTutorialHandler.sendEmptyMessage(mLastMessage);
    }
  }

  private boolean canMoveTo(boolean toLeft) {
    if (toLeft) {
      return mCurrentTutorial + 1 < TUTORIAL_LABELS.length;
    } else {
      return mCurrentTutorial - 1 >= 0;
    }
  }

  private void moveToTutorial(boolean toLeft) {
    if (toLeft) {
      mCurrentTutorial++;
    } else {
      mCurrentTutorial--;
    }
    mCurrentTutorial = Math.max(0, Math.min(mCurrentTutorial, TUTORIAL_LABELS.length - 1));

    updateTutorial(toLeft);
    updateNavButtons();
  }

  @OnClick({R.id.previous, R.id.next})
  public void onClick(View view) {
    int id = view.getId();
    moveToTutorial(id == R.id.next);
  }

  private void updateTutorial(final boolean toLeft) {
    Animation anim = AnimationUtils.loadAnimation(this, toLeft ? R.anim.slide_out_to_left : R.anim.slide_out_to_right);
    if (anim != null) {
      anim.setAnimationListener(new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
          Animation endAnimation = AnimationUtils.loadAnimation(TutorialActivity.this, toLeft ? R.anim.slide_in_from_right : R.anim.slide_in_from_left);
          if (endAnimation != null) {
            endAnimation.setInterpolator(new OvershootInterpolator());
            mTutorialLabel.startAnimation(endAnimation);
          }
          mTutorialLabel.setText(TUTORIAL_LABELS[mCurrentTutorial]);
        }
      });
      mTutorialLabel.startAnimation(anim);
    }


    cancelTutorialMessages();
    mFinger.cancelAllAnimations();

    switch (mCurrentTutorial) {
      case 0:
        mTutorialHandler.sendEmptyMessage(LAUNCH_APP_COLOR);
        break;
      case 1:
        mTutorialHandler.sendEmptyMessage(FIRST_DEMO);
        break;
      case 2:
        mTutorialHandler.sendEmptyMessage(SECOND_DEMO);
        break;
      case 3:
        mTutorialHandler.sendEmptyMessage(THIRD_DEMO);
        break;
    }
  }

  private void cancelTutorialMessages() {
    mTutorialHandler.removeMessages(FIRST_DEMO);
    mTutorialHandler.removeMessages(SECOND_DEMO);
    mTutorialHandler.removeMessages(THIRD_DEMO);
    mTutorialHandler.removeMessages(LAUNCH_APP_COLOR);
    mTutorialHandler.removeMessages(CLEAR_AND_RELAUNCH);
  }

  private void updateNavButtons() {
    mNextBtn.setVisibility(View.VISIBLE);
    mPreviousBtn.setVisibility(View.VISIBLE);

    if (mCurrentTutorial == 0) {
      mPreviousBtn.setVisibility(View.INVISIBLE);
    } else if (mCurrentTutorial + 1 == TUTORIAL_LABELS.length) {
      mNextBtn.setVisibility(View.INVISIBLE);
    }
  }

  private final Handler mInternalHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      switch (msg.what) {
        case LAUNCH_APP_COLOR_OVERLAY:
          mCellPhoneView.addScreenLayer(mAppColorLayer);
          mCellPhoneView.invalidateScreenLayers();
          break;
      }
    }
  };

  private final Handler mTutorialHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      removeAllLayers();
      switch (msg.what) {
        case LAUNCH_APP_COLOR:
          cancelTutorialMessages();
          launchAppColor();
          break;
        case FIRST_DEMO:
          cancelTutorialMessages();
          playDemo(0);
          break;
        case SECOND_DEMO:
          cancelTutorialMessages();
          playDemo(1);
          break;
        case THIRD_DEMO:
          cancelTutorialMessages();
          playDemo(2);
          break;
        case CLEAR_AND_RELAUNCH:
          cancelTutorialMessages();
          removeAllLayers();
          switch (msg.arg1) {
            case FIRST_DEMO:
            case SECOND_DEMO:
            case THIRD_DEMO:
              prepareFilterLayers();
              break;
          }
          mTutorialHandler.sendEmptyMessageDelayed(msg.arg1, LAUNCH_AFTER_CLEANING);
          break;
      }
      if (msg.what != CLEAR_AND_RELAUNCH) {
        mLastMessage = msg.what;
        Message message = Message.obtain(mTutorialHandler, CLEAR_AND_RELAUNCH, msg.what, 0);
        if (message != null) {
          mTutorialHandler.sendMessageDelayed(message, REPEAT_INTERVAL);
        }
      }
    }
  };

  private void playDemo(int demo) {
    prepareForFilterDemo();

    int[] locationOffset = getPhoneOffset();
    List<Pair<Float, Float>> toTouch = new ArrayList<Pair<Float, Float>>();
    final DemoSet demoSet = DEMOS.get(demo);
    final int[] colors = demoSet.colors;
    for (int i = 0; i < demoSet.colors.length; i++) {
      Pair<Float, Float> coord = mAppColorLayer.getCoordsForColor(colors[i], locationOffset[0], locationOffset[1]);
      toTouch.add(coord);
    }

    FingerView.TouchListener touchListener = new FingerView.TouchListener() {
      @Override
      public void onTouched(int position) {
        mAppColorLayer.setFilter(demoSet.namesArray.get(position));
        mAppColorLayer.addMarkAt(colors[position]);
        mCellPhoneView.invalidate();
      }
    };
    mFinger.path(toTouch, touchListener, demoSet.drag);
  }

  private int[] getPhoneOffset() {
    int[] location = new int[2];
    mCellPhoneView.getLocationOnScreen(location);
    float screenPositionX = mCellPhoneView.getScreenX();
    float screenPositionY = mCellPhoneView.getScreenY();
    location[0] += screenPositionX;
    location[1] += screenPositionY;
    return location;
  }

  private void prepareForFilterDemo() {
    mFinger.cancelAllAnimations();
    mOverlayLayer.show();
    mCellPhoneView.addScreenLayer(mOverlayLayer);
    mCellPhoneView.addScreenLayer(mAppColorLayer);
    mCellPhoneView.invalidateScreenLayers();
  }

  private void removeAllLayers() {
    mAppColorLayer.clear();
    mCellPhoneView.removeScreenLayer(mOverlayLayer);
    mCellPhoneView.removeScreenLayer(mAppColorLayer);
    mCellPhoneView.invalidate();
  }

  private void prepareFilterLayers() {
    mAppColorLayer.clear();
    mCellPhoneView.addScreenLayer(mOverlayLayer);
    mCellPhoneView.addScreenLayer(mAppColorLayer);
    mCellPhoneView.invalidate();
  }

  private void launchAppColor() {
    mCellPhoneView.addScreenLayer(mOverlayLayer);
    mCellPhoneView.invalidateScreenLayers();
    mOverlayLayer.fadeIn();

    int[] phoneOffset = getPhoneOffset();
    int yOffset = phoneOffset[1];
    mFinger.swipeToLeft(yOffset, new FingerView.OnSwipeListener() {
      @Override
      public void onSwipeComplete() {
        mInternalHandler.sendEmptyMessageDelayed(LAUNCH_APP_COLOR_OVERLAY, LAUNCH_OVERLAY_DELAY);
      }
    });
  }

  static class DemoSet {
    final List<List<String>> namesArray;
    final int[] colors;
    final boolean drag;

    DemoSet(List<List<String>> namesArray, int[] colors, boolean drag) {
      this.namesArray = namesArray;
      this.colors = colors;
      this.drag = drag;
    }
  }

  static final List<DemoSet> DEMOS = new ArrayList<DemoSet>();

  static {
    final List<String> BLUES = Arrays.asList("Chrome", "Delicious", "Dropbox", "Facebook",
        "Flickr", "Google Drive", "Google", "LinkedIn", "Skype", "Tumblr", "Twitter", "Vimeo",
        "Wordpress", "Play Store", "Kik");
    final List<String> BLUES_GREENS = Arrays.asList("Chrome", "Google Drive", "Kik");
    final List<String> BLUES_YELLOWS = Arrays.asList("Chrome", "Google Drive");

    final List<String> WHITES = Arrays.asList("Delicious", "Digg", "Facebook", "Flickr",
        "Gmail", "Google", "Google+", "Last fm", "LinkedIn", "Paypal", "Pinterest", "Pocket",
        "Skype", "Stumbleupon", "Tumblr", "Twitter", "Vimeo", "Yahoo");
    final List<String> WHITES_PURPLE = Arrays.asList("Yahoo");

    final List<String> GREENS = Arrays.asList("Chrome", "Evernote", "Google Drive",
        "Kik", "Play Store", "Spotify");
    final List<String> GREENS_BLACK = Arrays.asList("Evernote", "Kik", "Spotify");

    //noinspection unchecked
    DemoSet demo1 = new DemoSet(Arrays.asList(BLUES, BLUES_GREENS, BLUES_YELLOWS),
        new int[]{4, 3, 2}, true);
    DEMOS.add(demo1);

    //noinspection unchecked
    DemoSet demo2 = new DemoSet(Arrays.asList(WHITES, WHITES_PURPLE),
        new int[]{6, 5}, true);
    DEMOS.add(demo2);

    //noinspection unchecked
    DemoSet demo3 = new DemoSet(Arrays.asList(GREENS, GREENS_BLACK),
        new int[]{3, 7}, false);
    DEMOS.add(demo3);
  }
}

package app.color.utils;

import android.content.Context;
import android.gesture.*;

import java.io.File;
import java.util.ArrayList;

import static app.color.utils.Utilities.report;

public class GestureManager {
  public static final String GESTURES_FILE_NAME = "noicanicula";
  private static GestureManager sInstance;

  private final GestureLibrary mGestureLibrary;
  private boolean mIsWorking;

  private GestureManager(Context context) {
    Context appContext = context.getApplicationContext();
    File gesturesFile = new File(appContext.getFilesDir(), GESTURES_FILE_NAME);
    mGestureLibrary = GestureLibraries.fromFile(gesturesFile);
    mGestureLibrary.setOrientationStyle(GestureStore.ORIENTATION_SENSITIVE);
    mIsWorking = mGestureLibrary.load();
  }

  public Gesture get(String id) {
    return mGestureLibrary.getGestures(id).get(0);
  }

  public ArrayList<Prediction> recognize(Gesture lastGesture) {
    return mGestureLibrary.recognize(lastGesture);
  }

  @SuppressWarnings("UnusedDeclaration")
  public void deleteGestures() {
    try {
      for (String gestureId : new ArrayList<String>(mGestureLibrary.getGestureEntries())) {
        mGestureLibrary.removeEntry(gestureId);
      }
      mGestureLibrary.save();
      mIsWorking = mGestureLibrary.load();
    } catch (Exception e) {
      report(e);
    }
  }

  public static GestureManager getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new GestureManager(context);
    }
    return sInstance;
  }

  public void delete(String name) {
    mGestureLibrary.removeEntry(name);
  }

  public void delete(String name, Gesture gesture) {
    mGestureLibrary.removeGesture(name, gesture);
    mGestureLibrary.save();
  }

  public void save(String id, Gesture gesture) {
    mGestureLibrary.addGesture(id, gesture);
    mGestureLibrary.save();
    mIsWorking = mGestureLibrary.load();
  }

  public boolean isEmpty() {
    return mGestureLibrary.getGestureEntries().isEmpty();
  }

  public boolean isWorking() {
    return mIsWorking;
  }

  public Iterable<? extends String> getGestureEntries() {
    return mGestureLibrary.getGestureEntries();
  }

  public ArrayList<Gesture> getGestures(String name) {
    return mGestureLibrary.getGestures(name);
  }
}

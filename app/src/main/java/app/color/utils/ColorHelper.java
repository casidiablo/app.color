package app.color.utils;

import android.graphics.Color;

import java.util.HashSet;
import java.util.Set;

public class ColorHelper {
  public static final int PALETTE_SIZE = 8;
  public static final int RED_INDEX = 0;
  public static final int ORANGE_INDEX = 1;
  public static final int YELLOW_INDEX = 2;
  public static final int GREEN_INDEX = 3;
  public static final int BLUE_INDEX = 4;
  public static final int PURPLE_INDEX = 5;
  public static final int WHITE_INDEX = 6;
  public static final int BLACK_INDEX = 7;
  static final int[] FLAT_COLORS = new int[]{
      Color.parseColor("#c0392b"),
      Color.parseColor("#d35400"),
      Color.parseColor("#f1c40f"),
      Color.parseColor("#27ae60"),
      Color.parseColor("#2980b9"),
      Color.parseColor("#8e44ad"),
      Color.parseColor("#ecf0f1"),
      Color.parseColor("#151e26")};
  static final String[] FLAT_COLORS_NAMES = new String[]{
      "red",
      "orange",
      "yellow",
      "green",
      "blue",
      "purple",
      "white",
      "black"};
  public static final Set<Integer> INDEXES = new HashSet<Integer>();

  static {
    for (int i = 0; i < PALETTE_SIZE; i++) {
      INDEXES.add(i);
    }
  }

  public static int getFlatColor(int index) {
    return FLAT_COLORS[index];
  }

  public static String getColorName(int index) {
    return FLAT_COLORS_NAMES[index];
  }
}

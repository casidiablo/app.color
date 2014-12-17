#include <string.h>
#include <android/log.h>
#include <jni.h>
#include <stdio.h>
#include <android/bitmap.h>

#define  LOG_TAG    "madness"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define MIN3(x,y,z)  ((y) <= (z) ?              \
                      ((x) <= (y) ? (x) : (y))  \
                      :                         \
                      ((x) <= (z) ? (x) : (z)))

#define MAX3(x,y,z)  ((y) >= (z) ?              \
                      ((x) >= (y) ? (x) : (y))  \
                      :                         \
                      ((x) >= (z) ? (x) : (z)))

//#define RED     0
//#define ORANGE  1
//#define YELLOW  2
//#define GREEN   3
//#define BLUE    4
//#define PURPLE  5
#define WHITE   6
#define BLACK   7

const int COLOR_RANGES[] = {336, 16, 39, 69, 168, 243};

typedef struct
{
  uint8_t red;
  uint8_t green;
  uint8_t blue;
  uint8_t alpha;
} argb;

static void
rgbToHsv(float r, float g, float b, float* hsv) {
  float h = 0, s = 0, v = 0;

  // make rgb 0..1
  r /= 255, g /= 255, b /= 255;

  // find max and min rgb
  float rgb_min = MIN3(r, g, b);
  float rgb_max = MAX3(r, g, b);
  // compute value
  v = rgb_max;
  if(v == 0) {
    hsv[0] = hsv[1] = hsv[2] = 0.0;
    return;
  }

  // normalize value to 1
  r /= v;
  g /= v;
  b /= v;
  rgb_min = MIN3(r, g, b);
  rgb_max = MAX3(r, g, b);

  // compute saturation
  s = rgb_max - rgb_min;
  if(s == 0) {
    hsv[0] = h;
    hsv[1] = s;
    hsv[2] = v;
  }

  // normalize saturation to 1
  r = (r - rgb_min) / (rgb_max - rgb_min);
  g = (g - rgb_min) / (rgb_max - rgb_min);
  b = (b - rgb_min) / (rgb_max - rgb_min);
  rgb_min = MIN3(r, g, b);
  rgb_max = MAX3(r, g, b);

  // compute hue
  if(rgb_max == r) {
    h = 60.0 * (g - b);
    if(h < 0.0) {
      h += 360.0;
    }
  } else if(rgb_max == g) {
    h = 120.0 + 60.0 * (b - r);
  } else {
    h = 240.0 + 60.0 * (r - g);
  }

  hsv[0] = h;
  hsv[1] = s;
  hsv[2] = v;
}

static int isGray(float* hsv) {
  int tooLight = hsv[1] < 0.14f;
  if (tooLight) {
    return 1;
  }

  int tooDark = hsv[2] < 0.20f;
  if (tooDark) {
    return 1;
  }

  return hsv[1] < 0.25f && hsv[2] < 0.35f;
}

static int getColorIndex(float* hsv) {
  if(isGray(hsv)) {
    if (hsv[2] < 0.4f) {
      return BLACK;
    }
    return WHITE;
  }

  int i;
  for (i = 0; i < 6; i++) {
    float hue = hsv[0];
    int start = COLOR_RANGES[i];
    int end   = COLOR_RANGES[(i + 1) % 6];
    if(start < end && start <= hue && hue < end) {
      return i;
    }
    if(start > end && (hue >= start || hue < end)) {
      return i;
    }
  }

  return BLACK;// TODO revisit this
}

static int
build_palette(AndroidBitmapInfo* info, void* pixels, int sensitivity) {
  int xx, yy;

  float hsv[3];
  int histogram[8] = {0, 0, 0, 0, 0, 0, 0, 0};
  int total = 0;
  for(yy = 0; yy < info->height; yy++) {
    argb* line = (argb*)pixels;
    for(xx =0; xx < info->width; xx++) {
      if(line[xx].alpha == 255) { //only take into account non transparent
        rgbToHsv(line[xx].red, line[xx].green, line[xx].blue, hsv);
        int color = getColorIndex(hsv);
        histogram[color]++;
        total++;
      }
    }
    pixels = (char*)pixels + info->stride;
  }

  int palette_flags = 0;
  int i;
  for(i = 0; i < 8; i++) {
    if(histogram[i] >= total / sensitivity) {
      palette_flags |= 0xf << 4 * i;
    }
  }
  return palette_flags;
}

jint
Java_app_color_madness_Mad_palette(JNIEnv* env,
                                   jobject thiz,
                                   jobject bitmap,
                                   jint sensitivity) {
  AndroidBitmapInfo  info;
  void*              pixels;
  int                ret;

  if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
    LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
    return;
  }

  if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
    LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    return;
  }

  int palette_flags = build_palette(&info, pixels, sensitivity);

  AndroidBitmap_unlockPixels(env, bitmap);

  return palette_flags;
}

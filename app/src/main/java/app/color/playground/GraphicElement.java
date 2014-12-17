package app.color.playground;

public class GraphicElement {
  public float x;
  public float y;
  public float width;
  public float height;
  public int payload = Integer.MIN_VALUE;
  public boolean enabled;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GraphicElement that = (GraphicElement) o;

    if (height != that.height) return false;
    if (width != that.width) return false;
    if (x != that.x) return false;
    //noinspection RedundantIfStatement
    if (y != that.y) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
    result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
    result = 31 * result + (width != +0.0f ? Float.floatToIntBits(width) : 0);
    result = 31 * result + (height != +0.0f ? Float.floatToIntBits(height) : 0);
    return result;
  }

  @Override
  public String toString() {
    return "GraphicElement{" +
        "x=" + x +
        ", y=" + y +
        ", width=" + width +
        ", height=" + height +
        '}';
  }
}

package app.color.playground;

public class ColorElement extends GraphicElement {
  public final int colorIndex;
  public boolean selected;

  public ColorElement(int colorIndex) {
    this.colorIndex = colorIndex;
  }
}

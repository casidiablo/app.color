package app.color.model;

public class Sec {
  public String payload;

  @SuppressWarnings("UnusedDeclaration")
  public Sec() {
  }

  private Sec(String payload) {
    this.payload = payload;
  }

  public static Sec with(String payload) {
    return new Sec(payload);
  }
}

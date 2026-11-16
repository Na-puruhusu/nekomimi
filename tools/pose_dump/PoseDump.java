package io.github.napuruhusu.nekomimi.client;
import io.github.napuruhusu.nekomimi.state.NekoPose;
public class PoseDump {
  public static void main(String[] a) {
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (NekoPose p : NekoPose.values()) {
      float[] out = new float[CatPoses.SIZE];
      CatPoses.compute(p, out, 10f, 0f, 0f, new CatPoses.Motion(0f, 0f, false, 0f));
      if (!first) sb.append(","); first = false;
      sb.append("\"").append(p.name()).append("\":[");
      for (int i = 0; i < out.length; i++) sb.append(i==0?"":",").append(out[i]);
      sb.append("]");
    }
    System.out.println(sb.append("}"));
  }
}

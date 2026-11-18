package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoLeap;
import io.github.napuruhusu.nekomimi.state.NekoPose;

/** Prints the part placements of every pose, and of the crouch and the long jump, as JSON (used by preview_pose.py). */
public class PoseDump {
    private static void add(StringBuilder sb, String name, NekoPose pose, float pitch, CatPoses.Motion motion) {
        float[] out = new float[CatPoses.SIZE];
        CatPoses.compute(pose, out, 10f, pitch, 0f, motion);
        if (sb.length() > 1) {
            sb.append(",");
        }
        sb.append("\"").append(name).append("\":[");
        for (int i = 0; i < out.length; i++) {
            sb.append(i == 0 ? "" : ",").append(out[i]);
        }
        sb.append("]");
    }

    public static void main(String[] args) {
        float pitch = args.length > 0 ? Float.parseFloat(args[0]) : 0f;
        StringBuilder sb = new StringBuilder("{");
        for (NekoPose p : NekoPose.values()) {
            add(sb, p.name(), p, pitch, CatPoses.Motion.STILL);
        }
        add(sb, "CROUCH", NekoPose.NONE, pitch, new CatPoses.Motion(0f, 0f, false, 0f, NekoLeap.NONE, true, false));
        add(sb, "SWIM", NekoPose.NONE, pitch, new CatPoses.Motion(0f, 0.5f, false, 0f, NekoLeap.NONE, false, false, true));
        add(sb, "LONG_RISE", NekoPose.NONE, pitch, new CatPoses.Motion(0f, 0f, true, 0f, NekoLeap.LONG, false, true));
        add(sb, "LONG_FALL", NekoPose.NONE, pitch, new CatPoses.Motion(0f, 0f, true, 0f, NekoLeap.LONG, false, false));
        System.out.println(sb.append("}"));
    }
}

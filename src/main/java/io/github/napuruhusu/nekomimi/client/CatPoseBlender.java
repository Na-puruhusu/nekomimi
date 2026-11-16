package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoPose;

import java.util.WeakHashMap;

/**
 * Fades the cat body from one pose to the next, so sitting down, getting up and stretching are
 * movements and not jumps. One track per player. Only numbers and no game classes, so it can be tried
 * outside the game.
 *
 * While the pose stays the same the output is exactly the target (so a walking gait is not smeared);
 * when it changes, the output starts from what was last shown and moves over to the new target.
 */
public final class CatPoseBlender {
    /** How long a change of pose takes, in seconds. */
    private static final float FADE_SECONDS = 0.4F;
    /** A step longer than this (a teleport, a new world) is treated as one short step. */
    private static final float MAX_STEP = 0.1F;

    /** The fade of one player. */
    public static final class Track {
        private final float[] last = new float[CatPoses.SIZE];
        private final float[] from = new float[CatPoses.SIZE];
        private NekoPose pose;
        private float fade = 1.0F;
        private float lastTime = Float.NaN;
        private boolean started;
    }

    private final WeakHashMap<Object, Track> tracks = new WeakHashMap<>();

    public Track trackFor(Object owner) {
        return tracks.computeIfAbsent(owner, k -> new Track());
    }

    public void clear() {
        tracks.clear();
    }

    /**
     * Blends the target placement (already computed for `pose`) into `out`.
     *
     * @param nowTicks age of the player in ticks with the fraction of the tick
     */
    public static void blend(Track track, NekoPose pose, float[] target, float nowTicks, float[] out) {
        float dt = Float.isNaN(track.lastTime) ? 0.0F : (nowTicks - track.lastTime) / 20.0F;
        if (dt < 0.0F || dt > MAX_STEP) {
            dt = dt < 0.0F ? 0.0F : MAX_STEP;
        }
        track.lastTime = nowTicks;

        if (!track.started) {
            // First time we see this player: show the target at once.
            track.started = true;
            track.pose = pose;
            track.fade = 1.0F;
        } else if (track.pose != pose) {
            System.arraycopy(track.last, 0, track.from, 0, CatPoses.SIZE);
            track.pose = pose;
            track.fade = 0.0F;
        }

        if (track.fade >= 1.0F) {
            System.arraycopy(target, 0, out, 0, CatPoses.SIZE);
        } else {
            track.fade = Math.min(1.0F, track.fade + dt / FADE_SECONDS);
            float w = track.fade * track.fade * (3.0F - 2.0F * track.fade);
            for (int i = 0; i < CatPoses.SIZE; i++) {
                out[i] = track.from[i] + (target[i] - track.from[i]) * w;
            }
        }
        System.arraycopy(out, 0, track.last, 0, CatPoses.SIZE);
    }
}

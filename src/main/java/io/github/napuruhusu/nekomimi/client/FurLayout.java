package io.github.napuruhusu.nekomimi.client;

/**
 * Numbers shared by the fur models and the fur textures. tools/fur_texture.py draws the textures
 * from the same numbers and refuses to run if they differ from the ones below.
 *
 * How the fur works ("shell" fur): every ear and every tail segment is drawn as FURRY_LAYERS boxes
 * of growing size, one inside the other. The inner box is solid. Each outer box uses a texture in
 * which only some texels are opaque, so it shows up as separate hairs; the farther out the box, the
 * fewer hairs are left. Seen together they look like a coat of fur with a soft edge.
 *
 * The boxes are built TEXELS times larger than they are drawn and then scaled down by PART_SCALE.
 * That packs TEXELS x TEXELS texture pixels into every model unit, so the hairs are fine.
 */
public final class FurLayout {
    /** Outer shells on top of the solid base box. */
    public static final int SHELLS = 10;
    /** Boxes per part: the base and the shells. Layer 0 is the base, layer SHELLS the outermost. */
    public static final int LAYERS = SHELLS + 1;

    /** Texture pixels per model unit. */
    public static final int TEXELS = 4;
    /** Scale that turns the enlarged boxes back to their real size (1 / TEXELS). */
    public static final float PART_SCALE = 0.25F;

    /** How far the fur sticks out from the surface, in model units (1/16 block). */
    public static final float TAIL_FUR_LENGTH = 1.1F;
    public static final float EAR_FUR_LENGTH = 0.4F;

    // ---- Tail texture: 3 variants x LAYERS regions ----
    public static final int TAIL_TEX_WIDTH = 256;
    public static final int TAIL_TEX_HEIGHT = 128;
    /** Tail segment box, in texels: width and depth, and height. */
    public static final int TAIL_BOX_WIDTH = 8;
    public static final int TAIL_BOX_HEIGHT = 8;
    /** Footprint of one box in the texture: (2 * depth + 2 * width) x (depth + height). */
    public static final int TAIL_REGION_WIDTH = 32;
    public static final int TAIL_REGION_HEIGHT = 16;
    public static final int TAIL_COLUMNS = 8;

    public static final int TAIL_VARIANT_FUR = 0;
    public static final int TAIL_VARIANT_STRIPE = 1;
    public static final int TAIL_VARIANT_TIP = 2;

    // ---- Ear texture: the base box and the tip box, LAYERS regions each ----
    public static final int EAR_TEX_WIDTH = 256;
    public static final int EAR_TEX_HEIGHT = 64;
    /** Ear base box 12 x 8 x 8 texels (3 x 2 x 2 units); ear tip box 4 x 4 x 8 texels (1 x 1 x 2 units). */
    public static final int EAR_BASE_WIDTH = 12;
    public static final int EAR_BASE_HEIGHT = 8;
    public static final int EAR_BASE_DEPTH = 8;
    public static final int EAR_TIP_WIDTH = 4;
    public static final int EAR_TIP_HEIGHT = 4;
    public static final int EAR_TIP_DEPTH = 8;
    public static final int EAR_BASE_REGION_WIDTH = 40;
    public static final int EAR_BASE_REGION_HEIGHT = 16;
    public static final int EAR_BASE_COLUMNS = 6;
    public static final int EAR_TIP_REGION_WIDTH = 24;
    public static final int EAR_TIP_REGION_HEIGHT = 12;
    public static final int EAR_TIP_COLUMNS = 10;
    /** The tip regions start below the base regions. */
    public static final int EAR_TIP_TOP = 32;

    private FurLayout() {
    }

    /** Growth of a shell beyond the base box, in texels, for a fur of the given length in model units. */
    public static float grow(int layer, float furLength) {
        return layer * furLength * TEXELS / SHELLS;
    }

    /** How far out a layer is, 0 (base) to 1 (outermost). */
    public static float depth(int layer) {
        return layer / (float) SHELLS;
    }

    public static int tailU(int variant, int layer) {
        return ((variant * LAYERS + layer) % TAIL_COLUMNS) * TAIL_REGION_WIDTH;
    }

    public static int tailV(int variant, int layer) {
        return ((variant * LAYERS + layer) / TAIL_COLUMNS) * TAIL_REGION_HEIGHT;
    }

    public static int earBaseU(int layer) {
        return (layer % EAR_BASE_COLUMNS) * EAR_BASE_REGION_WIDTH;
    }

    public static int earBaseV(int layer) {
        return (layer / EAR_BASE_COLUMNS) * EAR_BASE_REGION_HEIGHT;
    }

    public static int earTipU(int layer) {
        return (layer % EAR_TIP_COLUMNS) * EAR_TIP_REGION_WIDTH;
    }

    public static int earTipV(int layer) {
        return EAR_TIP_TOP + (layer / EAR_TIP_COLUMNS) * EAR_TIP_REGION_HEIGHT;
    }
}

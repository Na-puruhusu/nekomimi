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

    // ---- Ear texture: three stacked boxes (a triangle seen from the front), LAYERS regions each ----
    public static final int EAR_TEX_WIDTH = 256;
    public static final int EAR_TEX_HEIGHT = 64;
    /** Number of boxes in one ear. Piece 0 is the wide bottom, the last one the point. */
    public static final int EAR_PIECES = 3;
    /**
     * Size of each piece in texels (width, height, depth): 3 x 1.5 x 2, 2 x 1.5 x 2 and 1 x 1 x 1 units,
     * stacked on the head so that the ear narrows to a point.
     */
    public static final int[][] EAR_BOX = {{12, 6, 8}, {8, 6, 8}, {4, 4, 4}};
    /** Regions of a piece sit in rows of EAR_COLUMNS[piece], starting EAR_TOP[piece] texels from the top. */
    public static final int[] EAR_COLUMNS = {6, 8, 16};
    public static final int[] EAR_TOP = {0, 28, 56};

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

    /** Width of the footprint of a piece in the texture: 2 * depth + 2 * width. */
    public static int earRegionWidth(int piece) {
        return 2 * EAR_BOX[piece][2] + 2 * EAR_BOX[piece][0];
    }

    /** Height of the footprint of a piece in the texture: depth + height. */
    public static int earRegionHeight(int piece) {
        return EAR_BOX[piece][2] + EAR_BOX[piece][1];
    }

    public static int earU(int piece, int layer) {
        return (layer % EAR_COLUMNS[piece]) * earRegionWidth(piece);
    }

    public static int earV(int piece, int layer) {
        return EAR_TOP[piece] + (layer / EAR_COLUMNS[piece]) * earRegionHeight(piece);
    }
}

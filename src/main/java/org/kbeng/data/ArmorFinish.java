package org.kbeng.data;

/** Describes the light response used by a rendered armor piece. */
@DataClass
public enum ArmorFinish {
    NONE(0),
    MATTE(1),
    PEARL(2),
    METAL(3);

    private final int shaderValue;

    ArmorFinish(int shaderValue) {
        this.shaderValue = shaderValue;
    }

    /** Returns the value consumed by the armor material shader. */
    public int getShaderValue() {
        return shaderValue;
    }

    /** Resolves the finish assigned to an armor tier. */
    public static ArmorFinish forTier(Tier tier) {
        if (tier == null) return NONE;
        return switch (tier) {
            case COPPER -> MATTE;
            case IRON -> PEARL;
            case STEEL, GOLDEN, PLATINUM, DIAMOND -> METAL;
            default -> NONE;
        };
    }
}

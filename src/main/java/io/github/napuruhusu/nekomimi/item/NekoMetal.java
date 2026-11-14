package io.github.napuruhusu.nekomimi.item;

import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;

/**
 * Which armor stats an accessory has. NONE is the cosmetic-only version; the others use the
 * vanilla helmet / leggings stats of that metal (defense, toughness, durability, enchantability...).
 * The look when worn is the same for all of them.
 */
public enum NekoMetal {
    NONE("", false),
    IRON("iron", false),
    GOLD("gold", false),
    DIAMOND("diamond", false),
    NETHERITE("netherite", true);

    private final String id;
    private final boolean fireResistant;

    NekoMetal(String id, boolean fireResistant) {
        this.id = id;
        this.fireResistant = fireResistant;
    }

    public String getId() {
        return id;
    }

    public boolean isFireResistant() {
        return fireResistant;
    }

    public ArmorMaterial material() {
        return switch (this) {
            case IRON -> ArmorMaterials.IRON;
            case GOLD -> ArmorMaterials.GOLD;
            case DIAMOND -> ArmorMaterials.DIAMOND;
            case NETHERITE -> ArmorMaterials.NETHERITE;
            case NONE -> NekoArmorMaterial.INSTANCE;
        };
    }
}

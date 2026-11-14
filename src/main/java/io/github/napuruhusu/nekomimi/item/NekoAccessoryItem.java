package io.github.napuruhusu.nekomimi.item;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.client.NekoClientExtensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * A wearable cat ear / cat tail.
 * Ears use the head slot and the tail uses the legs slot, and both are drawn with a custom model.
 * The metal decides the armor stats only; the worn look depends on the color alone.
 */
public class NekoAccessoryItem extends ArmorItem {

    public enum Kind {
        EARS(ArmorItem.Type.HELMET, "nekomimi"),
        TAIL(ArmorItem.Type.LEGGINGS, "neko_tail");

        private final ArmorItem.Type armorType;
        private final String prefix;

        Kind(ArmorItem.Type armorType, String prefix) {
            this.armorType = armorType;
            this.prefix = prefix;
        }

        /** Id of the cosmetic version, which is also the base name of the textures. */
        public String itemId(NekoColor color) {
            return prefix + "_" + color.getId();
        }

        public String itemId(NekoColor color, NekoMetal metal) {
            return metal == NekoMetal.NONE ? itemId(color) : itemId(color) + "_" + metal.getId();
        }
    }

    private final String armorTexture;

    public NekoAccessoryItem(Kind kind, NekoColor color, NekoMetal metal) {
        super(metal.material(), kind.armorType, properties(metal));
        this.armorTexture = Nekomimi.MODID + ":textures/models/armor/" + kind.itemId(color) + ".png";
    }

    private static Item.Properties properties(NekoMetal metal) {
        Item.Properties properties = new Item.Properties().stacksTo(1);
        return metal.isFireResistant() ? properties.fireResistant() : properties;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return armorTexture;
    }

    // Note: Forge calls this from inside the Item constructor, so no field of this class may be used here.
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(NekoClientExtensions.INSTANCE);
    }
}

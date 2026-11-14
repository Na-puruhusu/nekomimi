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

        public String itemId(NekoColor color) {
            return prefix + "_" + color.getId();
        }
    }

    private final Kind kind;
    private final String armorTexture;

    public NekoAccessoryItem(Kind kind, NekoColor color) {
        super(NekoArmorMaterial.INSTANCE, kind.armorType, new Item.Properties().stacksTo(1));
        this.kind = kind;
        this.armorTexture = Nekomimi.MODID + ":textures/models/armor/" + kind.itemId(color) + ".png";
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return armorTexture;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(NekoClientExtensions.of(kind));
    }
}

package io.github.napuruhusu.nekomimi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Swaps the default armor model for the cat ears / cat tail model when the item is worn.
 *
 * Forge calls Item#initializeClient from inside the Item constructor, before the fields of our
 * item subclass are assigned. So this class must not depend on any per-item field: which model to
 * use is decided from the slot being rendered (head = ears, legs = tail).
 */
public final class NekoClientExtensions implements IClientItemExtensions {
    public static final NekoClientExtensions INSTANCE = new NekoClientExtensions();

    private static CatAccessoryModel earsModel;
    private static CatAccessoryModel tailModel;

    private NekoClientExtensions() {
    }

    /** Called on resource reload so models are baked again from the new model set. */
    public static void clearCache() {
        earsModel = null;
        tailModel = null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                  EquipmentSlot slot, HumanoidModel<?> original) {
        if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.LEGS) {
            return original;
        }
        boolean tail = slot == EquipmentSlot.LEGS;
        CatAccessoryModel model = getModel(tail);
        // Copy the wearer's current pose (head rotation, sneaking, riding, baby scale...).
        ((HumanoidModel) original).copyPropertiesTo(model);
        if (tail) {
            model.applyTail(TailPhysics.update(entity));
        }
        return model;
    }

    private static CatAccessoryModel getModel(boolean tail) {
        if (!tail) {
            if (earsModel == null) {
                earsModel = new CatAccessoryModel(
                        Minecraft.getInstance().getEntityModels().bakeLayer(NekoModels.EARS_LAYER), false);
            }
            return earsModel;
        }
        if (tailModel == null) {
            tailModel = new CatAccessoryModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(NekoModels.TAIL_LAYER), true);
        }
        return tailModel;
    }
}

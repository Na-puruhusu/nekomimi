package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.item.NekoAccessoryItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Swaps the default armor model for the cat ears / cat tail model when the item is worn. */
public final class NekoClientExtensions implements IClientItemExtensions {
    private static final NekoClientExtensions EARS = new NekoClientExtensions(NekoAccessoryItem.Kind.EARS);
    private static final NekoClientExtensions TAIL = new NekoClientExtensions(NekoAccessoryItem.Kind.TAIL);

    private static CatAccessoryModel earsModel;
    private static CatAccessoryModel tailModel;

    private final NekoAccessoryItem.Kind kind;

    private NekoClientExtensions(NekoAccessoryItem.Kind kind) {
        this.kind = kind;
    }

    public static NekoClientExtensions of(NekoAccessoryItem.Kind kind) {
        return kind == NekoAccessoryItem.Kind.EARS ? EARS : TAIL;
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
        CatAccessoryModel model = getModel(kind);
        // Copy the wearer's current pose (head rotation, sneaking, riding, baby scale...).
        ((HumanoidModel) original).copyPropertiesTo(model);
        if (kind == NekoAccessoryItem.Kind.TAIL) {
            model.animateTail(entity);
        }
        return model;
    }

    private static CatAccessoryModel getModel(NekoAccessoryItem.Kind kind) {
        if (kind == NekoAccessoryItem.Kind.EARS) {
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

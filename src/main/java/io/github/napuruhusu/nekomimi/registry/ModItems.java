package io.github.napuruhusu.nekomimi.registry;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.item.NekoAccessoryItem;
import io.github.napuruhusu.nekomimi.item.NekoColor;
import io.github.napuruhusu.nekomimi.item.NekoMetal;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Nekomimi.MODID);

    /** color -> metal -> item. NekoMetal.NONE is the cosmetic-only version. */
    public static final Map<NekoColor, Map<NekoMetal, RegistryObject<Item>>> EARS = new EnumMap<>(NekoColor.class);
    public static final Map<NekoColor, Map<NekoMetal, RegistryObject<Item>>> TAILS = new EnumMap<>(NekoColor.class);

    static {
        for (NekoColor color : NekoColor.values()) {
            EARS.put(color, registerAll(NekoAccessoryItem.Kind.EARS, color));
            TAILS.put(color, registerAll(NekoAccessoryItem.Kind.TAIL, color));
        }
    }

    private static Map<NekoMetal, RegistryObject<Item>> registerAll(NekoAccessoryItem.Kind kind, NekoColor color) {
        Map<NekoMetal, RegistryObject<Item>> items = new EnumMap<>(NekoMetal.class);
        for (NekoMetal metal : NekoMetal.values()) {
            items.put(metal, ITEMS.register(
                    kind.itemId(color, metal),
                    () -> new NekoAccessoryItem(kind, color, metal)));
        }
        return items;
    }

    private ModItems() {
    }
}

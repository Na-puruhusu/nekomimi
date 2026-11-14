package io.github.napuruhusu.nekomimi.registry;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.item.NekoAccessoryItem;
import io.github.napuruhusu.nekomimi.item.NekoColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Nekomimi.MODID);

    public static final Map<NekoColor, RegistryObject<Item>> EARS = new EnumMap<>(NekoColor.class);
    public static final Map<NekoColor, RegistryObject<Item>> TAILS = new EnumMap<>(NekoColor.class);

    static {
        for (NekoColor color : NekoColor.values()) {
            EARS.put(color, ITEMS.register(
                    NekoAccessoryItem.Kind.EARS.itemId(color),
                    () -> new NekoAccessoryItem(NekoAccessoryItem.Kind.EARS, color)));
            TAILS.put(color, ITEMS.register(
                    NekoAccessoryItem.Kind.TAIL.itemId(color),
                    () -> new NekoAccessoryItem(NekoAccessoryItem.Kind.TAIL, color)));
        }
    }

    private ModItems() {
    }
}

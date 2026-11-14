package io.github.napuruhusu.nekomimi.registry;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.item.NekoColor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nekomimi.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Nekomimi.MODID))
                    .icon(() -> new ItemStack(ModItems.EARS.get(NekoColor.WHITE).get()))
                    .displayItems((parameters, output) -> {
                        for (NekoColor color : NekoColor.values()) {
                            output.accept(ModItems.EARS.get(color).get());
                        }
                        for (NekoColor color : NekoColor.values()) {
                            output.accept(ModItems.TAILS.get(color).get());
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }
}

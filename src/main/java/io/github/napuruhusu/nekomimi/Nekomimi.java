package io.github.napuruhusu.nekomimi;

import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.registry.ModCreativeTabs;
import io.github.napuruhusu.nekomimi.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Nekomimi.MODID)
public class Nekomimi {
    public static final String MODID = "nekomimi";

    public Nekomimi() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        NekoNetwork.register();
    }
}

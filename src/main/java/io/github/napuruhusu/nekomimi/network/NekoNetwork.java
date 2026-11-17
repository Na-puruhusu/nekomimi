package io.github.napuruhusu.nekomimi.network;

import io.github.napuruhusu.nekomimi.Nekomimi;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class NekoNetwork {
    private static final String PROTOCOL = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Nekomimi.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private NekoNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SetStatePacket.class,
                SetStatePacket::encode, SetStatePacket::decode, SetStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, SyncStatePacket.class,
                SyncStatePacket::encode, SyncStatePacket::decode, SyncStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, LeapPacket.class,
                LeapPacket::encode, LeapPacket::decode, LeapPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, SyncLeapPacket.class,
                SyncLeapPacket::encode, SyncLeapPacket::decode, SyncLeapPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}

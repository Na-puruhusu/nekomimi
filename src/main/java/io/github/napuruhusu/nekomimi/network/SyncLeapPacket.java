package io.github.napuruhusu.nekomimi.network;

import io.github.napuruhusu.nekomimi.client.ClientSync;
import io.github.napuruhusu.nekomimi.state.NekoLeap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Server to client: a player made a boosted jump (sent to everyone who can see them). */
public final class SyncLeapPacket {
    private final UUID player;
    private final int leap;

    public SyncLeapPacket(UUID player, int leap) {
        this.player = player;
        this.leap = leap;
    }

    public static void encode(SyncLeapPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.player);
        buf.writeByte(packet.leap);
    }

    public static SyncLeapPacket decode(FriendlyByteBuf buf) {
        return new SyncLeapPacket(buf.readUUID(), buf.readByte());
    }

    public static void handle(SyncLeapPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        NekoLeap leap = NekoLeap.byIndex(packet.leap);
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientSync.applyLeap(packet.player, leap)));
        ctx.setPacketHandled(true);
    }
}

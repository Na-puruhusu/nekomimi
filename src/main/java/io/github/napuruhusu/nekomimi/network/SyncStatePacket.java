package io.github.napuruhusu.nekomimi.network;

import io.github.napuruhusu.nekomimi.client.ClientSync;
import io.github.napuruhusu.nekomimi.state.NekoEmotion;
import io.github.napuruhusu.nekomimi.state.NekoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Server to client: the state of one player, sent to that player and to everyone who can see them. */
public final class SyncStatePacket {
    private final UUID player;
    private final boolean cat;
    private final int emotion;

    public SyncStatePacket(UUID player, boolean cat, int emotion) {
        this.player = player;
        this.cat = cat;
        this.emotion = emotion;
    }

    public static void encode(SyncStatePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.player);
        buf.writeBoolean(packet.cat);
        buf.writeByte(packet.emotion);
    }

    public static SyncStatePacket decode(FriendlyByteBuf buf) {
        return new SyncStatePacket(buf.readUUID(), buf.readBoolean(), buf.readByte());
    }

    public static void handle(SyncStatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        NekoState state = new NekoState(packet.cat, NekoEmotion.byIndex(packet.emotion));
        // The lambdas keep client-only classes from being loaded on a dedicated server.
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientSync.apply(packet.player, state)));
        ctx.setPacketHandled(true);
    }
}

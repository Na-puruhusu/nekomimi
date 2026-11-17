package io.github.napuruhusu.nekomimi.network;

import io.github.napuruhusu.nekomimi.server.NekoServer;
import io.github.napuruhusu.nekomimi.state.NekoLeap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: "I just made a boosted jump of this kind." Only used so that others can see the motion. */
public final class LeapPacket {
    private final int leap;

    public LeapPacket(int leap) {
        this.leap = leap;
    }

    public static void encode(LeapPacket packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.leap);
    }

    public static LeapPacket decode(FriendlyByteBuf buf) {
        return new LeapPacket(buf.readByte());
    }

    public static void handle(LeapPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer sender = ctx.getSender();
        if (sender != null) {
            ctx.enqueueWork(() -> NekoServer.requestLeap(sender, NekoLeap.byIndex(packet.leap)));
        }
        ctx.setPacketHandled(true);
    }
}

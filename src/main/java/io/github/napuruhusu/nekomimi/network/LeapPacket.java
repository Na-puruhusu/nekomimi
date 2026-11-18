package io.github.napuruhusu.nekomimi.network;

import io.github.napuruhusu.nekomimi.server.NekoServer;
import io.github.napuruhusu.nekomimi.state.NekoLeap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: "I just started (or ended) this kind of action." Only used so that others can see the motion. */
public final class LeapPacket {
    private final int leap;
    /** Extra data of the action: the side the cat faces (0-3) when hanging or climbing. */
    private final int extra;

    public LeapPacket(int leap, int extra) {
        this.leap = leap;
        this.extra = extra;
    }

    public static void encode(LeapPacket packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.leap);
        buf.writeByte(packet.extra);
    }

    public static LeapPacket decode(FriendlyByteBuf buf) {
        return new LeapPacket(buf.readByte(), buf.readByte());
    }

    public static void handle(LeapPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer sender = ctx.getSender();
        if (sender != null) {
            ctx.enqueueWork(() -> NekoServer.requestLeap(sender, NekoLeap.byIndex(packet.leap), packet.extra));
        }
        ctx.setPacketHandled(true);
    }
}

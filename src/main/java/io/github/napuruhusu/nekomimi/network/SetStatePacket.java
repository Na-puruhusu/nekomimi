package io.github.napuruhusu.nekomimi.network;

import io.github.napuruhusu.nekomimi.server.NekoServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: "I want to be in this mode with this emotion." The server checks it. */
public final class SetStatePacket {
    private final boolean cat;
    private final int emotion;

    public SetStatePacket(boolean cat, int emotion) {
        this.cat = cat;
        this.emotion = emotion;
    }

    public static void encode(SetStatePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.cat);
        buf.writeByte(packet.emotion);
    }

    public static SetStatePacket decode(FriendlyByteBuf buf) {
        return new SetStatePacket(buf.readBoolean(), buf.readByte());
    }

    public static void handle(SetStatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                NekoServer.requestState(sender, packet.cat, packet.emotion);
            }
        });
        ctx.setPacketHandled(true);
    }
}

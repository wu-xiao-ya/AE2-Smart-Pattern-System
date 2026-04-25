package com.wuxiaoya.techstart.network;

import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SetPatternModFiltersPacket(String[] excludedInputModIds, String[] excludedOutputModIds) {
    public static void encode(SetPatternModFiltersPacket packet, FriendlyByteBuf buf) {
        writeStringArray(buf, packet.excludedInputModIds);
        writeStringArray(buf, packet.excludedOutputModIds);
    }

    public static SetPatternModFiltersPacket decode(FriendlyByteBuf buf) {
        return new SetPatternModFiltersPacket(readStringArray(buf), readStringArray(buf));
    }

    public static void handle(SetPatternModFiltersPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (sender.containerMenu instanceof PatternEditorMenu menu) {
                menu.applyExcludedModFiltersFromClient(packet.excludedInputModIds, packet.excludedOutputModIds, sender);
            }
        });
        context.setPacketHandled(true);
    }

    private static void writeStringArray(FriendlyByteBuf buf, String[] values) {
        String[] source = values == null ? new String[0] : values;
        buf.writeVarInt(source.length);
        for (String value : source) {
            buf.writeUtf(value == null ? "" : value);
        }
    }

    private static String[] readStringArray(FriendlyByteBuf buf) {
        int size = Math.max(0, buf.readVarInt());
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String value = buf.readUtf();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values.toArray(new String[0]);
    }
}

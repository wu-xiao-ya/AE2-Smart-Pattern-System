package com.ae2smartpatternsystem.network;

import com.ae2smartpatternsystem.menu.PatternEditorMenu;
import com.ae2smartpatternsystem.core.model.FilterMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SetPatternModFiltersPacket(
        FilterMode inputMode,
        String[] inputIds,
        FilterMode outputMode,
        String[] outputIds) {
    private static final int MAX_IDS_PER_SIDE = 512;
    private static final int MAX_ID_LENGTH = 64;

    public static void encode(SetPatternModFiltersPacket packet, FriendlyByteBuf buf) {
        validate(packet);
        buf.writeEnum(packet.inputMode);
        writeStringArray(buf, packet.inputIds);
        buf.writeEnum(packet.outputMode);
        writeStringArray(buf, packet.outputIds);
    }

    public static SetPatternModFiltersPacket decode(FriendlyByteBuf buf) {
        return new SetPatternModFiltersPacket(
                buf.readEnum(FilterMode.class),
                readStringArray(buf),
                buf.readEnum(FilterMode.class),
                readStringArray(buf));
    }

    public static void handle(SetPatternModFiltersPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !isValid(packet)) {
                return;
            }
            if (sender.containerMenu instanceof PatternEditorMenu menu) {
                menu.applyModFiltersFromClient(
                        packet.inputMode,
                        packet.inputIds,
                        packet.outputMode,
                        packet.outputIds,
                        sender);
            }
        });
        context.setPacketHandled(true);
    }

    private static void writeStringArray(FriendlyByteBuf buf, String[] values) {
        String[] source = values == null ? new String[0] : values;
        buf.writeVarInt(source.length);
        for (String entry : source) {
            String value = entry == null ? "" : entry;
            buf.writeUtf(value, MAX_ID_LENGTH);
        }
    }

    private static String[] readStringArray(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_IDS_PER_SIDE) {
            throw new IllegalArgumentException("Too many mod filter ids: " + size);
        }
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String value = buf.readUtf(MAX_ID_LENGTH);
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values.toArray(new String[0]);
    }

    static void validate(SetPatternModFiltersPacket packet) {
        if (!isValid(packet)) {
            throw new IllegalArgumentException("Invalid pattern mod filter payload");
        }
    }

    static boolean isValid(SetPatternModFiltersPacket packet) {
        return packet != null
                && packet.inputMode != null
                && packet.outputMode != null
                && isValidStringArray(packet.inputIds)
                && isValidStringArray(packet.outputIds);
    }

    private static boolean isValidStringArray(String[] values) {
        if (values == null) {
            return true;
        }
        if (values.length > MAX_IDS_PER_SIDE) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.length() > MAX_ID_LENGTH) {
                return false;
            }
        }
        return true;
    }
}

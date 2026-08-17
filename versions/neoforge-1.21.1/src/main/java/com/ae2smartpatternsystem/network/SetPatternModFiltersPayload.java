package com.ae2smartpatternsystem.network;

import com.ae2smartpatternsystem.TechStartNeoForge;
import com.ae2smartpatternsystem.menu.PatternEditorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SetPatternModFiltersPayload(
        int inputMode,
        List<String> inputIds,
        int outputMode,
        List<String> outputIds) implements CustomPacketPayload {
    public static final Type<SetPatternModFiltersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TechStartNeoForge.MODID, "set_pattern_mod_filters"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPatternModFiltersPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SetPatternModFiltersPayload decode(RegistryFriendlyByteBuf buf) {
            return new SetPatternModFiltersPayload(
                    readMode(buf, "input"),
                    readStringList(buf),
                    readMode(buf, "output"),
                    readStringList(buf));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SetPatternModFiltersPayload payload) {
            writeMode(buf, payload.inputMode, "input");
            writeStringList(buf, payload.inputIds);
            writeMode(buf, payload.outputMode, "output");
            writeStringList(buf, payload.outputIds);
        }
    };

    @Override
    public Type<SetPatternModFiltersPayload> type() {
        return TYPE;
    }

    public static void handle(SetPatternModFiltersPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!isValid(payload)) {
            return;
        }
        if (serverPlayer.containerMenu instanceof PatternEditorMenu menu) {
            menu.applyModFiltersFromClient(
                    payload.inputMode,
                    payload.inputIds,
                    payload.outputMode,
                    payload.outputIds,
                    serverPlayer
            );
        }
    }

    private static void writeStringList(RegistryFriendlyByteBuf buf, List<String> values) {
        List<String> source = values == null ? List.of() : values;
        if (source.size() > PatternEditorMenu.MAX_MOD_FILTER_IDS) {
            throw new IllegalArgumentException("Too many mod filter IDs: " + source.size());
        }
        buf.writeVarInt(source.size());
        for (String value : source) {
            if (value != null && value.length() > PatternEditorMenu.MAX_MOD_FILTER_ID_LENGTH) {
                throw new IllegalArgumentException("Mod filter ID is too long");
            }
            buf.writeUtf(value == null ? "" : value, PatternEditorMenu.MAX_MOD_FILTER_ID_LENGTH);
        }
    }

    private static List<String> readStringList(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > PatternEditorMenu.MAX_MOD_FILTER_IDS) {
            throw new IllegalArgumentException("Invalid mod filter ID count: " + size);
        }
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String value = buf.readUtf(PatternEditorMenu.MAX_MOD_FILTER_ID_LENGTH);
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static int readMode(RegistryFriendlyByteBuf buf, String side) {
        int mode = buf.readVarInt();
        if (!isValidMode(mode)) {
            throw new IllegalArgumentException("Invalid " + side + " mod filter mode: " + mode);
        }
        return mode;
    }

    private static void writeMode(RegistryFriendlyByteBuf buf, int mode, String side) {
        if (!isValidMode(mode)) {
            throw new IllegalArgumentException("Invalid " + side + " mod filter mode: " + mode);
        }
        buf.writeVarInt(mode);
    }

    private static boolean isValidMode(int mode) {
        return mode == PatternEditorMenu.FILTER_MODE_WHITELIST
                || mode == PatternEditorMenu.FILTER_MODE_BLACKLIST;
    }

    static boolean isValid(SetPatternModFiltersPayload payload) {
        return payload != null
                && isValidMode(payload.inputMode)
                && isValidMode(payload.outputMode)
                && isValidStringList(payload.inputIds)
                && isValidStringList(payload.outputIds);
    }

    private static boolean isValidStringList(List<String> values) {
        if (values == null) {
            return true;
        }
        if (values.size() > PatternEditorMenu.MAX_MOD_FILTER_IDS) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.length() > PatternEditorMenu.MAX_MOD_FILTER_ID_LENGTH) {
                return false;
            }
        }
        return true;
    }
}

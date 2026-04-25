package com.wuxiaoya.techstart.network;

import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SetPatternModFiltersPayload(List<String> excludedInputModIds, List<String> excludedOutputModIds) implements CustomPacketPayload {
    public static final Type<SetPatternModFiltersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TechStartNeoForge.MODID, "set_pattern_mod_filters"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPatternModFiltersPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SetPatternModFiltersPayload decode(RegistryFriendlyByteBuf buf) {
            return new SetPatternModFiltersPayload(readStringList(buf), readStringList(buf));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SetPatternModFiltersPayload payload) {
            writeStringList(buf, payload.excludedInputModIds);
            writeStringList(buf, payload.excludedOutputModIds);
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
        if (serverPlayer.containerMenu instanceof PatternEditorMenu menu) {
            menu.applyExcludedModFiltersFromClient(
                    payload.excludedInputModIds.toArray(new String[0]),
                    payload.excludedOutputModIds.toArray(new String[0]),
                    serverPlayer
            );
        }
    }

    private static void writeStringList(RegistryFriendlyByteBuf buf, List<String> values) {
        List<String> source = values == null ? List.of() : values;
        buf.writeVarInt(source.size());
        for (String value : source) {
            buf.writeUtf(value == null ? "" : value);
        }
    }

    private static List<String> readStringList(RegistryFriendlyByteBuf buf) {
        int size = Math.max(0, buf.readVarInt());
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String value = buf.readUtf();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }
}

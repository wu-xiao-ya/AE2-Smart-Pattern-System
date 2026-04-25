package com.wuxiaoya.techstart.network;

import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetPatternSlotAmountPayload(int slotId, int amount) implements CustomPacketPayload {
    public static final Type<SetPatternSlotAmountPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TechStartNeoForge.MODID, "set_pattern_slot_amount"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPatternSlotAmountPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetPatternSlotAmountPayload::slotId,
            ByteBufCodecs.VAR_INT,
            SetPatternSlotAmountPayload::amount,
            SetPatternSlotAmountPayload::new
    );

    @Override
    public Type<SetPatternSlotAmountPayload> type() {
        return TYPE;
    }

    public static void handle(SetPatternSlotAmountPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        TechStartNeoForge.LOGGER.info("Received SetPatternSlotAmountPayload slot={} amount={}", payload.slotId, payload.amount);
        if (serverPlayer.containerMenu instanceof PatternEditorMenu menu) {
            menu.applyPatternSlotAmountFromClient(payload.slotId, payload.amount, serverPlayer);
        }
    }
}

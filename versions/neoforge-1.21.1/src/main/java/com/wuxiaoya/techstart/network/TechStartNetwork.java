package com.wuxiaoya.techstart.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class TechStartNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private TechStartNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(SetPatternSlotPayload.TYPE, SetPatternSlotPayload.STREAM_CODEC, SetPatternSlotPayload::handle);
        registrar.playToServer(SetPatternSlotAmountPayload.TYPE, SetPatternSlotAmountPayload.STREAM_CODEC, SetPatternSlotAmountPayload::handle);
        registrar.playToServer(SetPatternModFiltersPayload.TYPE, SetPatternModFiltersPayload.STREAM_CODEC, SetPatternModFiltersPayload::handle);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}

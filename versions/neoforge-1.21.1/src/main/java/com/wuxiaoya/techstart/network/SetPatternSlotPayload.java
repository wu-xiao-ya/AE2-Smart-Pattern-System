package com.wuxiaoya.techstart.network;

import com.wuxiaoya.techstart.TechStartNeoForge;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetPatternSlotPayload(int slotId, ItemStack marker) implements CustomPacketPayload {
    public static final Type<SetPatternSlotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TechStartNeoForge.MODID, "set_pattern_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPatternSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetPatternSlotPayload::slotId,
            ItemStack.OPTIONAL_STREAM_CODEC,
            SetPatternSlotPayload::marker,
            SetPatternSlotPayload::new
    );

    @Override
    public Type<SetPatternSlotPayload> type() {
        return TYPE;
    }

    public static void handle(SetPatternSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.containerMenu instanceof PatternEditorMenu menu) {
            menu.applyMarkerStackFromClient(payload.slotId, payload.marker.copy(), serverPlayer);
        }
    }
}

package com.ae2smartpatternsystem.network;

import com.ae2smartpatternsystem.core.model.FilterMode;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetPatternModFiltersPacketTest {
    @Test
    void codecRoundTripPreservesIndependentModesAndIds() {
        SetPatternModFiltersPacket source = new SetPatternModFiltersPacket(
                FilterMode.WHITELIST,
                new String[]{"create", "ae2"},
                FilterMode.BLACKLIST,
                new String[]{"minecraft"});
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SetPatternModFiltersPacket.encode(source, buffer);
        SetPatternModFiltersPacket decoded = SetPatternModFiltersPacket.decode(buffer);

        assertEquals(source.inputMode(), decoded.inputMode());
        assertArrayEquals(source.inputIds(), decoded.inputIds());
        assertEquals(source.outputMode(), decoded.outputMode());
        assertArrayEquals(source.outputIds(), decoded.outputIds());
    }

    @Test
    void codecRejectsOversizedListsAndIdsInsteadOfTruncating() {
        String[] tooMany = new String[513];
        Arrays.fill(tooMany, "create");
        SetPatternModFiltersPacket tooManyPacket = new SetPatternModFiltersPacket(
                FilterMode.BLACKLIST, tooMany, FilterMode.BLACKLIST, new String[0]);
        SetPatternModFiltersPacket tooLongPacket = new SetPatternModFiltersPacket(
                FilterMode.BLACKLIST, new String[]{"a".repeat(65)}, FilterMode.BLACKLIST, new String[0]);

        assertThrows(IllegalArgumentException.class, () ->
                SetPatternModFiltersPacket.encode(tooManyPacket, new FriendlyByteBuf(Unpooled.buffer())));
        assertThrows(IllegalArgumentException.class, () ->
                SetPatternModFiltersPacket.encode(tooLongPacket, new FriendlyByteBuf(Unpooled.buffer())));
    }

    @Test
    void serverValidationRejectsNullModesAndOversizedValues() {
        assertFalse(SetPatternModFiltersPacket.isValid(new SetPatternModFiltersPacket(
                null, new String[0], FilterMode.BLACKLIST, new String[0])));
        assertFalse(SetPatternModFiltersPacket.isValid(new SetPatternModFiltersPacket(
                FilterMode.BLACKLIST, new String[513], FilterMode.BLACKLIST, new String[0])));
        assertFalse(SetPatternModFiltersPacket.isValid(new SetPatternModFiltersPacket(
                FilterMode.BLACKLIST, new String[]{"a".repeat(65)}, FilterMode.BLACKLIST, new String[0])));
    }
}

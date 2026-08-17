package com.ae2smartpatternsystem.network;

import com.ae2smartpatternsystem.menu.PatternEditorMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetPatternModFiltersPayloadTest {
    @Test
    void codecRoundTripPreservesIndependentModesAndIds() {
        SetPatternModFiltersPayload source = new SetPatternModFiltersPayload(
                PatternEditorMenu.FILTER_MODE_WHITELIST,
                List.of("create", "ae2"),
                PatternEditorMenu.FILTER_MODE_BLACKLIST,
                List.of("minecraft"));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.OTHER);

        SetPatternModFiltersPayload.STREAM_CODEC.encode(buffer, source);
        SetPatternModFiltersPayload decoded = SetPatternModFiltersPayload.STREAM_CODEC.decode(buffer);

        assertEquals(source, decoded);
    }

    @Test
    void codecRejectsInvalidModeAndOversizedValues() {
        SetPatternModFiltersPayload invalidMode = new SetPatternModFiltersPayload(
                99, List.of(), PatternEditorMenu.FILTER_MODE_BLACKLIST, List.of());
        SetPatternModFiltersPayload tooMany = new SetPatternModFiltersPayload(
                PatternEditorMenu.FILTER_MODE_BLACKLIST,
                java.util.stream.IntStream.range(0, 513).mapToObj(index -> "mod" + index).toList(),
                PatternEditorMenu.FILTER_MODE_BLACKLIST,
                List.of());
        SetPatternModFiltersPayload tooLong = new SetPatternModFiltersPayload(
                PatternEditorMenu.FILTER_MODE_BLACKLIST,
                List.of("a".repeat(65)),
                PatternEditorMenu.FILTER_MODE_BLACKLIST,
                List.of());

        assertFalse(SetPatternModFiltersPayload.isValid(invalidMode));
        assertFalse(SetPatternModFiltersPayload.isValid(tooMany));
        assertFalse(SetPatternModFiltersPayload.isValid(tooLong));
        assertThrows(IllegalArgumentException.class, () ->
                SetPatternModFiltersPayload.STREAM_CODEC.encode(
                        new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.OTHER),
                        invalidMode));
    }
}

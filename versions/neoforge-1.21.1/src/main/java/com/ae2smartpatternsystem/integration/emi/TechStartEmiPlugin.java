package com.ae2smartpatternsystem.integration.emi;

import com.ae2smartpatternsystem.client.PatternEditorScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class TechStartEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(PatternEditorScreen.class, new PatternEditorEmiDragDropHandler());
    }
}

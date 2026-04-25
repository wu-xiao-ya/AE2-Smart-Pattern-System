package com.wuxiaoya.techstart.integration.emi;

import com.wuxiaoya.techstart.client.PatternEditorScreen;
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

package com.ae2smartpatternsystem;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

public class TileEntityPatternExpander extends TileEntity implements ITickable {
    @Override
    public void update() {
        // AE2S-only 1.12.2 no longer uses the legacy pattern expander grid logic.
    }

    public void onBlockDestroyed() {
        // No-op after removing the legacy 1.12.2 provider implementation.
    }
}

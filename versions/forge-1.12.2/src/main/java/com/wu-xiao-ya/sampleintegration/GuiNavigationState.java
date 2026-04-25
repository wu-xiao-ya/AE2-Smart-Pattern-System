package com.lwx1145.sampleintegration;

/**
 * Lightweight GUI navigation handoff state between search and filter pages.
 */
public final class GuiNavigationState {

    private static String pendingFilterEntryId;

    private GuiNavigationState() {
    }

    public static synchronized void setPendingFilterEntryId(String entryId) {
        pendingFilterEntryId = entryId;
    }

    public static synchronized String consumePendingFilterEntryId() {
        String result = pendingFilterEntryId;
        pendingFilterEntryId = null;
        return result;
    }
}

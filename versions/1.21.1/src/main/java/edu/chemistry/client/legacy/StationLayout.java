package edu.chemistry.client.legacy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where everything goes in a station menu's console layout (used when Legacy4J is installed). Coordinates are relative
 * to the panel's top-left corner. Sizes follow Legacy4J's own furnace menu: 21 pixel slot frames, or 13 on small
 * (480p) screens.
 */
public final class StationLayout {
    public final boolean sd;
    /** slot frame size */
    public final int slot;
    public int width;
    public int height;
    public int titleX;
    public int titleY;
    public int invLabelX;
    public int invLabelY;
    /** menu slot index -> {x, y, frame size} */
    public final Map<Integer, int[]> slots = new LinkedHashMap<>();
    /** named areas -> {x, y, width, height} */
    public final Map<String, int[]> parts = new LinkedHashMap<>();

    public StationLayout(boolean sd) {
        this.sd = sd;
        this.slot = sd ? 13 : 21;
        this.width = baseWidth(sd);
        this.titleX = sd ? 7 : 14;
        this.titleY = sd ? 5 : 11;
    }

    /** the furnace's panel width */
    public static int baseWidth(boolean sd) {
        return sd ? 130 : 214;
    }

    /** a measurement for the normal layout, shrunk to match on small screens */
    public int s(int hd) {
        return this.sd ? Math.round(hd * 13 / 21.0F) : hd;
    }

    /** the big frame Legacy4J gives a furnace's result slot */
    public int resultSize() {
        return this.sd ? 21 : 32;
    }

    public int arrowWidth() {
        return this.sd ? 16 : 33;
    }

    public int arrowHeight() {
        return this.sd ? 14 : 24;
    }

    public void put(int index, int x, int y, int size) {
        this.slots.put(index, new int[] {x, y, size});
    }

    public int[] part(String name, int x, int y, int w, int h) {
        int[] r = {x, y, w, h};
        this.parts.put(name, r);
        return r;
    }

    public int[] part(String name) {
        return this.parts.get(name);
    }

    /**
     * The player's inventory (27 slots, then the hotbar, as menus add them), below everything else and laid out like
     * Legacy4J's furnace: label, three rows, a small gap, the hotbar. Sets the panel height.
     */
    public void inventory(int firstIndex, int x, int labelY) {
        this.invLabelX = x;
        this.invLabelY = labelY;
        int top = labelY + (this.sd ? 10 : 13);
        int hotbar = labelY + (this.sd ? 54 : 82);
        for (int i = 0; i < 27; i++) this.put(firstIndex + i, x + i % 9 * this.slot, top + i / 9 * this.slot, this.slot);
        for (int i = 0; i < 9; i++) this.put(firstIndex + 27 + i, x + i * this.slot, hotbar, this.slot);
        this.height = labelY + (this.sd ? 74 : 117);
    }
}

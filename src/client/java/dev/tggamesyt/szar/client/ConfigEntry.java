package dev.tggamesyt.szar.client;

public class ConfigEntry {
    public final String id;
    public final String displayName;
    public final boolean defaultValue;
    public final String linkedResourcePack; // null if no resourcepack linked

    private boolean value;

    public ConfigEntry(String id, String displayName, boolean defaultValue, String linkedResourcePack) {
        this.id = id;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.linkedResourcePack = linkedResourcePack;
        this.value = defaultValue;
    }

    // Convenience constructor — no resourcepack
    public ConfigEntry(String id, String displayName, boolean defaultValue) {
        this(id, displayName, defaultValue, null);
    }

    public boolean get() { return value; }
    public void set(boolean v) { value = v; }
    public boolean hasResourcePack() { return linkedResourcePack != null; }
}
package net.badgersmc.enthusiahub.module.modules.hotbar;

import org.bukkit.configuration.file.FileConfiguration;

/** Configuration validation; preserves all pre-existing hotbar reservations. */
public record SurvivalEntrySettings(int slot, String server, boolean selectOnJoin) {
    public static SurvivalEntrySettings read(FileConfiguration config) {
        if (!config.getBoolean("start_survival.enabled", false)) return null;
        int slot = config.getInt("start_survival.slot", 4);
        String server = config.getString("start_survival.server", "").trim();
        if (slot < 0 || slot > 8 || !server.matches("[A-Za-z0-9_-]+")) return null;
        if (config.getBoolean("custom_join_items.enabled")) {
            var items = config.getConfigurationSection("custom_join_items.items");
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    if (items.getInt(key + ".slot", -1) == slot) return null;
                }
            }
        }
        for (String feature : java.util.List.of("player_hider", "pvp_mode", "pvp_sword", "teleportation_bow")) {
            if (config.getBoolean(feature + ".enabled") && config.getInt(feature + ".slot", -1) == slot) return null;
        }
        if (config.getBoolean("teleportation_bow.enabled")
                && config.getInt("teleportation_bow.arrow_slot", -1) == slot) return null;
        return new SurvivalEntrySettings(slot, server, config.getBoolean("start_survival.select_on_join", true));
    }
}

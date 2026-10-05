package net.badgersmc.enthusiahub.module.modules.hotbar;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HotbarConfigTest {
    @Test void packagedDefaultsPreserveLegacyToggleAndSupplyRestoredSwordAndMessages() {
        var defaults = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(
                java.util.Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")), java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(defaults.getBoolean("pvp_sword.enabled"));
        assertTrue(defaults.getBoolean("pvp_mode.enabled"));
        assertTrue(defaults.getBoolean("world_settings.disable_item_durability"));
        assertEquals(List.of(Map.of("SHARPNESS", 1)), HotbarManager.compatibleSwordConfig(defaults.getConfigurationSection("pvp_sword.sword")).getList("enchantments"));
        var messages = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(
                java.util.Objects.requireNonNull(getClass().getResourceAsStream("/messages.yml")), java.nio.charset.StandardCharsets.UTF_8));
        for (var message : net.badgersmc.enthusiahub.config.Messages.values()) {
            if (message.name().startsWith("PVP_SWORD_")) assertTrue(messages.isString("Messages." + message.getPath()));
        }
    }
    @Test void existingLiveStringEnchantmentsAndCurrentMapsBothLoadWithoutChangingConfig() {
        var original = new YamlConfiguration();
        original.set("material", "IRON_SWORD");
        original.set("enchantments", List.of("SHARPNESS:1", "KNOCKBACK:1", new LinkedHashMap<>(Map.of("UNBREAKING", 3))));
        var adapted = HotbarManager.compatibleSwordConfig(original);
        assertEquals("IRON_SWORD", adapted.getString("material"));
        assertEquals(List.of(Map.of("SHARPNESS", 1), Map.of("KNOCKBACK", 1), Map.of("UNBREAKING", 3)), adapted.getList("enchantments"));
        assertEquals("SHARPNESS:1", original.getList("enchantments").getFirst());
    }
}

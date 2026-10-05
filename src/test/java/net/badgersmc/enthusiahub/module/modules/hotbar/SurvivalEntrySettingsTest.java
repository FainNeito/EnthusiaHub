package net.badgersmc.enthusiahub.module.modules.hotbar;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class SurvivalEntrySettingsTest {
    private YamlConfiguration config() {
        var yaml = new YamlConfiguration();
        yaml.set("start_survival.enabled", true);
        yaml.set("start_survival.server", "SMP");
        yaml.set("start_survival.slot", 4);
        return yaml;
    }

    @Test
    void existingConfigurationsStayOffAndValidTargetIsPreserved() {
        assertNull(SurvivalEntrySettings.read(new YamlConfiguration()));
        var settings = SurvivalEntrySettings.read(config());
        assertEquals("SMP", settings.server());
        assertEquals(4, settings.slot());
        assertTrue(settings.selectOnJoin());
    }

    @Test
    void invalidTargetAndSlotCannotRegisterItem() {
        var yaml = config();
        for (String target : List.of("", "SMP other", "[PROXY] SMP")) {
            yaml.set("start_survival.server", target);
            assertNull(SurvivalEntrySettings.read(yaml));
        }
        yaml.set("start_survival.server", "SMP");
        yaml.set("start_survival.slot", 9);
        assertNull(SurvivalEntrySettings.read(yaml));
        yaml.set("start_survival.slot", -1);
        assertNull(SurvivalEntrySettings.read(yaml));
    }

    @Test
    void configuredSelectorAndEveryKnownSpecialItemReserveTheirSlot() {
        var yaml = config();
        yaml.set("custom_join_items.enabled", true);
        yaml.set("custom_join_items.items.server_selector.slot", 4);
        assertNull(SurvivalEntrySettings.read(yaml));
        yaml.set("custom_join_items.items.server_selector.slot", 0);
        assertNotNull(SurvivalEntrySettings.read(yaml));
        for (String feature : List.of("player_hider", "pvp_mode", "pvp_sword", "teleportation_bow")) {
            yaml.set(feature + ".enabled", true);
            yaml.set(feature + ".slot", 4);
            assertNull(SurvivalEntrySettings.read(yaml));
            yaml.set(feature + ".enabled", false);
        }
        yaml.set("teleportation_bow.enabled", true);
        yaml.set("teleportation_bow.arrow_slot", 4);
        assertNull(SurvivalEntrySettings.read(yaml));
    }
}

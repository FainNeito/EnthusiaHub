package net.badgersmc.enthusiahub.module.modules.hotbar.items;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.config.Messages;
import net.badgersmc.enthusiahub.module.modules.hotbar.HotbarManager;
import net.badgersmc.enthusiahub.module.modules.world.BuildMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PvPSwordItemTest {
    @Test void generatedSwordAndAllArmorAreUnbreakableAndWorldExitClearsState() {
        var scheduler = mock(BukkitScheduler.class);
        var task = mock(BukkitTask.class);
        var tick = new AtomicReference<Runnable>();
        var plugin = mock(EnthusiaHubPlugin.class);
        when(plugin.getName()).thenReturn("EnthusiaHub");
        when(plugin.namespace()).thenReturn("enthusiahub");
        var manager = mock(HotbarManager.class);
        when(manager.getPlugin()).thenReturn(plugin);
        var config = new YamlConfiguration();
        config.set("pvp_sword.countdown.duration", 0);
        config.set("pvp_sword.countdown.sound.enabled", false);
        config.set("pvp_sword.armor.random_trim", false);
        for (String piece : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            config.set("pvp_sword.armor." + piece + ".material", "IRON_" + piece.toUpperCase());
        }
        when(manager.getConfig(ConfigType.SETTINGS)).thenReturn(config);
        Messages.setConfiguration(new YamlConfiguration());
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.getLocation()).thenReturn(mock(Location.class));
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L))).thenAnswer(call -> {
            tick.set(call.getArgument(1)); return task;
        });
        var sword = mock(ItemStack.class);
        var swordMeta = mock(ItemMeta.class);
        when(sword.getItemMeta()).thenReturn(swordMeta);
        when(swordMeta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        try (var bukkit = mockStatic(Bukkit.class); var build = mockStatic(BuildMode.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            build.when(BuildMode::getInstance).thenReturn(mock(BuildMode.class));
            var item = new PvPSwordItem(manager, sword, 2, "PVP_SWORD");
            verify(swordMeta).setUnbreakable(true);
            item.onInteract(player);
            var armorMeta = mock(ArmorMeta.class);
            try (var armor = mockConstruction(ItemStack.class, (stack, context) -> when(stack.getItemMeta()).thenReturn(armorMeta))) {
                tick.get().run();
                assertTrue(item.isInPvPMode(player));
                assertEquals(4, armor.constructed().size());
                verify(armorMeta, times(4)).setUnbreakable(true);
                for (var stack : armor.constructed()) verify(stack).setItemMeta(armorMeta);
            }
            item.onWorldExit(new PlayerChangedWorldEvent(player, mock(org.bukkit.World.class)));
            assertFalse(item.isInPvPMode(player));
            verify(player.getInventory()).setHelmet(null);
            verify(task).cancel();
        }
    }

    @Test void switchingAwayCancelsActivationAndShutdownCancelsPendingTasks() {
        var scheduler = mock(BukkitScheduler.class);
        var task = mock(BukkitTask.class);
        var plugin = mock(EnthusiaHubPlugin.class);
        when(plugin.getName()).thenReturn("EnthusiaHub");
        when(plugin.namespace()).thenReturn("enthusiahub");
        var manager = mock(HotbarManager.class);
        when(manager.getPlugin()).thenReturn(plugin);
        var config = new YamlConfiguration();
        when(manager.getConfig(ConfigType.SETTINGS)).thenReturn(config);
        Messages.setConfiguration(new YamlConfiguration());
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getLocation()).thenReturn(mock(Location.class));
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        var sword = mock(ItemStack.class);
        var meta = mock(ItemMeta.class);
        var data = mock(PersistentDataContainer.class);
        when(sword.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(any(), eq(PersistentDataType.STRING))).thenReturn("PVP_SWORD");
        when(inventory.getItem(2)).thenReturn(sword);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L))).thenReturn(task);
        try (var bukkit = mockStatic(Bukkit.class); var build = mockStatic(BuildMode.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            build.when(BuildMode::getInstance).thenReturn(mock(BuildMode.class));
            var item = new PvPSwordItem(manager, sword, 2, "PVP_SWORD");
            item.onPlayerItemHeld(new PlayerItemHeldEvent(player, 0, 2));
            item.onPlayerItemHeld(new PlayerItemHeldEvent(player, 2, 0));
            verify(task).cancel();
            assertFalse(item.isInPvPMode(player));
            item.onPlayerItemHeld(new PlayerItemHeldEvent(player, 0, 2));
            item.shutdown();
            verify(task, times(2)).cancel();
            assertTrue(item.getPvPModePlayers().isEmpty());
        }
    }
}

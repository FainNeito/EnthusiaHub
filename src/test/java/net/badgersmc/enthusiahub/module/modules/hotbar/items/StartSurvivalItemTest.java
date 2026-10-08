package net.badgersmc.enthusiahub.module.modules.hotbar.items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;
import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.module.modules.hotbar.HotbarManager;
import net.badgersmc.enthusiahub.module.modules.hotbar.SurvivalEntrySettings;
import net.badgersmc.enthusiahub.module.modules.world.BuildMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.file.YamlConfiguration;
import net.badgersmc.enthusiahub.config.ConfigType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class StartSurvivalItemTest {
    @Test
    void firstJoinWelcomeRequiresAnActualOwnedEntryAndPreservesJoinSelection() {
        try (var mode = mockStatic(BuildMode.class)) {
            var buildMode = mock(BuildMode.class);
            mode.when(BuildMode::getInstance).thenReturn(buildMode);
            var plugin = mock(EnthusiaHubPlugin.class);
            when(plugin.namespace()).thenReturn("enthusiahub");
            var manager = mock(HotbarManager.class);
            when(manager.getPlugin()).thenReturn(plugin);
            var config = new YamlConfiguration();
            config.set("start_survival.welcome.enabled", true);
            config.set("start_survival.welcome.messages", java.util.List.of("Welcome to Enthusia!", "Use Start Survival to join SMP."));
            when(manager.getConfig(ConfigType.SETTINGS)).thenReturn(config);
            var template = mock(ItemStack.class);
            var meta = mock(ItemMeta.class);
            var data = mock(PersistentDataContainer.class);
            when(template.getItemMeta()).thenReturn(meta);
            when(template.hasItemMeta()).thenReturn(true);
            when(template.clone()).thenReturn(template);
            when(meta.getPersistentDataContainer()).thenReturn(data);
            when(data.get(any(org.bukkit.NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn("START_SURVIVAL");
            var player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            var inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);
            var current = new java.util.concurrent.atomic.AtomicReference<ItemStack>();
            when(inventory.getItem(4)).thenAnswer(call -> current.get());
            doAnswer(call -> { current.set(call.getArgument(1)); return null; }).when(inventory).setItem(eq(4), any(ItemStack.class));
            var entry = new StartSurvivalItem(manager, template, new SurvivalEntrySettings(4, "SMP", true));
            var event = mock(PlayerJoinEvent.class);
            when(event.getPlayer()).thenReturn(player);

            entry.hotbarPlayerJoin(event);
            verify(player).sendMessage("Welcome to Enthusia!");
            verify(player).sendMessage("Use Start Survival to join SMP.");
            verify(inventory).setHeldItemSlot(4);
            clearInvocations(player);
            when(player.hasPlayedBefore()).thenReturn(true);
            entry.hotbarPlayerJoin(event);
            verify(player, never()).sendMessage(anyString());
            when(player.hasPlayedBefore()).thenReturn(false);
            config.set("start_survival.welcome.enabled", false);
            entry.hotbarPlayerJoin(event);
            verify(player, never()).sendMessage(anyString());
            config.set("start_survival.welcome.enabled", true);
            config.set("start_survival.welcome.messages", java.util.List.of());
            entry.hotbarPlayerJoin(event);
            verify(player, never()).sendMessage(anyString());
        }
    }

    @Test
    void welcomeFailsClosedForUnusableEntryAndNeverRunsOnItemRegrant() {
        try (var mode = mockStatic(BuildMode.class)) {
            var buildMode = mock(BuildMode.class);
            mode.when(BuildMode::getInstance).thenReturn(buildMode);
            var plugin = mock(EnthusiaHubPlugin.class);
            when(plugin.namespace()).thenReturn("enthusiahub");
            var manager = mock(HotbarManager.class);
            when(manager.getPlugin()).thenReturn(plugin);
            var config = new YamlConfiguration();
            config.set("start_survival.welcome.enabled", true);
            when(manager.getConfig(ConfigType.SETTINGS)).thenReturn(config);
            var template = mock(ItemStack.class);
            when(template.clone()).thenReturn(template);
            var meta = mock(ItemMeta.class);
            var data = mock(PersistentDataContainer.class);
            when(template.getItemMeta()).thenReturn(meta);
            when(template.hasItemMeta()).thenReturn(true);
            when(meta.getPersistentDataContainer()).thenReturn(data);
            when(data.get(any(org.bukkit.NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn("START_SURVIVAL");
            var player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            var inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);
            var existing = mock(ItemStack.class);
            when(existing.getType()).thenReturn(Material.DIAMOND);
            when(inventory.getItem(4)).thenReturn(existing);
            var entry = new StartSurvivalItem(manager, template, new SurvivalEntrySettings(4, "SMP", true));
            var event = mock(PlayerJoinEvent.class);
            when(event.getPlayer()).thenReturn(player);
            entry.hotbarPlayerJoin(event);
            // Keep an owned entry present so each later guard independently prevents chat.
            when(inventory.getItem(4)).thenReturn(template);
            when(template.getType()).thenReturn(Material.GRASS_BLOCK);
            when(buildMode.isPresent(player.getUniqueId())).thenReturn(true);
            entry.hotbarPlayerJoin(event);
            when(buildMode.isPresent(player.getUniqueId())).thenReturn(false);
            when(manager.inDisabledWorld((org.bukkit.Location) null)).thenReturn(true);
            entry.hotbarPlayerJoin(event);
            when(manager.inDisabledWorld((org.bukkit.Location) null)).thenReturn(false);
            when(player.isOnline()).thenReturn(false);
            entry.hotbarPlayerJoin(event);
            when(player.isOnline()).thenReturn(true);
            entry.giveItem(player);
            verify(player, never()).sendMessage(anyString());
        }
    }

    @Test
    void playerOwnedSlotIsPreservedAndEmptySlotReceivesEntry() {
        try (var mode = mockStatic(BuildMode.class)) {
            var buildMode = mock(BuildMode.class);
            mode.when(BuildMode::getInstance).thenReturn(buildMode);
            var plugin = mock(EnthusiaHubPlugin.class);
            when(plugin.namespace()).thenReturn("enthusiahub");
            var manager = mock(HotbarManager.class);
            when(manager.getPlugin()).thenReturn(plugin);
            var template = mock(ItemStack.class);
            var meta = mock(ItemMeta.class);
            when(template.getItemMeta()).thenReturn(meta);
            when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
            when(template.clone()).thenReturn(template);
            var player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            var inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);
            var existing = mock(ItemStack.class);
            when(existing.getType()).thenReturn(Material.DIAMOND);
            when(inventory.getItem(4)).thenReturn(existing);
            var item = new StartSurvivalItem(manager, template, new SurvivalEntrySettings(4, "SMP", true));
            item.giveItem(player);
            verify(inventory, never()).setItem(anyInt(), any(ItemStack.class));
            when(inventory.getItem(4)).thenReturn(null);
            item.giveItem(player);
            verify(inventory).setItem(4, template);
            when(buildMode.isPresent(player.getUniqueId())).thenReturn(true);
            item.giveItem(player);
            verify(inventory, times(1)).setItem(anyInt(), any(ItemStack.class));
        }
    }

    @Test
    void interactionUsesExistingProxyWireFormatAndBuildDisabledWorldGates() throws Exception {
        try (var mode = mockStatic(BuildMode.class)) {
            var buildMode = mock(BuildMode.class);
            mode.when(BuildMode::getInstance).thenReturn(buildMode);
            var plugin = mock(EnthusiaHubPlugin.class);
            when(plugin.namespace()).thenReturn("enthusiahub");
            var manager = mock(HotbarManager.class);
            when(manager.getPlugin()).thenReturn(plugin);
            var template = mock(ItemStack.class);
            var player = mock(Player.class);
            UUID id = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(id);
            when(player.getName()).thenReturn("NewPlayer");
            when(player.isOnline()).thenReturn(true);
            var item = new StartSurvivalItem(manager, template, new SurvivalEntrySettings(4, "SMP", true));
            item.onInteract(player);
            var capture = ArgumentCaptor.forClass(byte[].class);
            verify(player).sendPluginMessage(eq(plugin), eq("BungeeCord"), capture.capture());
            try (var data = new DataInputStream(new ByteArrayInputStream(capture.getValue()))) {
                assertEquals("ConnectOther", data.readUTF());
                assertEquals("NewPlayer", data.readUTF());
                assertEquals("SMP", data.readUTF());
            }
            when(buildMode.isPresent(id)).thenReturn(true);
            item.onInteract(player);
            when(buildMode.isPresent(id)).thenReturn(false);
            when(manager.inDisabledWorld((org.bukkit.Location) null)).thenReturn(true);
            item.onInteract(player);
            verify(player, times(1)).sendPluginMessage(eq(plugin), eq("BungeeCord"), any(byte[].class));
        }
    }
}

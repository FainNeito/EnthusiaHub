package net.badgersmc.enthusiahub.module.modules.world;

import org.bukkit.Location;
import org.bukkit.block.Barrel;
import org.bukkit.block.Shelf;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.module.ModuleManager;
import net.badgersmc.enthusiahub.module.ModuleType;
import net.badgersmc.enthusiahub.module.modules.hotbar.PvPSwordModule;
import net.badgersmc.enthusiahub.Permissions;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorldProtectTest {
    private void setField(WorldProtect protection, String name, boolean value) throws Exception {
        Field field = WorldProtect.class.getDeclaredField(name);
        field.setAccessible(true); field.set(protection, value);
    }
    private PvPSwordModule withSwordModule(WorldProtect protection) {
        protection.config.set("pvp_sword.enabled", true);
        var plugin = mock(EnthusiaHubPlugin.class);
        var modules = mock(ModuleManager.class);
        var sword = mock(PvPSwordModule.class);
        doReturn(plugin).when(protection).getPlugin();
        when(plugin.getModuleManager()).thenReturn(modules);
        when(modules.getModule(ModuleType.PVP_SWORD)).thenReturn(sword);
        return sword;
    }
    @Test void meleeAndProjectilesRequireBothPlayersToOptInAndPreserveMinigameBypass() throws Exception {
        var protection = protection();
        setField(protection, "playerPvP", true);
        var sword = withSwordModule(protection);
        var attacker = mock(Player.class);
        var victim = mock(Player.class);
        when(victim.getLocation()).thenReturn(mock(Location.class));
        var arrow = mock(Projectile.class);
        when(arrow.getShooter()).thenReturn(attacker);
        for (boolean projectile : new boolean[]{false, true}) {
            for (boolean attackerEnabled : new boolean[]{false, true}) {
                for (boolean victimEnabled : new boolean[]{false, true}) {
                    var event = mock(EntityDamageByEntityEvent.class);
                    when(event.getDamager()).thenReturn(projectile ? arrow : attacker);
                    when(event.getEntity()).thenReturn(victim);
                    when(sword.isInPvPMode(attacker)).thenReturn(attackerEnabled);
                    when(sword.isInPvPMode(victim)).thenReturn(victimEnabled);
                    protection.onEntityDamage(event);
                    if (attackerEnabled && victimEnabled) verify(event, never()).setCancelled(anyBoolean());
                    else verify(event).setCancelled(true);
                    verify(event, never()).setCancelled(false);
                }
            }
            when(attacker.hasPermission(Permissions.PVP_BYPASS.getPermission())).thenReturn(true);
            var bypass = mock(EntityDamageByEntityEvent.class);
            when(bypass.getDamager()).thenReturn(projectile ? arrow : attacker);
            when(bypass.getEntity()).thenReturn(victim);
            protection.onEntityDamage(bypass);
            verify(bypass, never()).setCancelled(anyBoolean());
            when(attacker.hasPermission(Permissions.PVP_BYPASS.getPermission())).thenReturn(false);
        }
        var handler = WorldProtect.class.getMethod("onEntityDamage", EntityDamageByEntityEvent.class)
                .getAnnotation(org.bukkit.event.EventHandler.class);
        assertTrue(handler.ignoreCancelled());
        assertEquals(org.bukkit.event.EventPriority.HIGHEST, handler.priority());
    }
    @Test void activePvPGearIsProtectedEvenWhenGeneralDurabilityIsOff() throws Exception {
        var protection = protection();
        setField(protection, "itemDurability", false);
        var sword = withSwordModule(protection);
        var player = mock(Player.class);
        when(player.getLocation()).thenReturn(mock(Location.class));
        var event = mock(PlayerItemDamageEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(sword.isInPvPMode(player)).thenReturn(true);
        protection.onItemDamage(event);
        verify(event).setCancelled(true);
        clearInvocations(event);
        when(sword.isInPvPMode(player)).thenReturn(false);
        protection.onItemDamage(event);
        verify(event, never()).setCancelled(anyBoolean());
    }
    private WorldProtect protection() throws Exception {
        WorldProtect protection = mock(WorldProtect.class, CALLS_REAL_METHODS);
        Field durability = WorldProtect.class.getDeclaredField("itemDurability");
        durability.setAccessible(true); durability.set(protection, true);
        Field blockInteract = WorldProtect.class.getDeclaredField("blockInteract");
        blockInteract.setAccessible(true); blockInteract.set(protection, true);
        protection.config = new YamlConfiguration();
        doReturn(false).when(protection).inDisabledWorld(any(Location.class));
        return protection;
    }
    @Test void shelfDirectHotbarTransferIsDeniedAtTheBlockInteraction() throws Exception {
        WorldProtect protection = protection();
        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(mock(Location.class));
        org.bukkit.block.Block block = mock(org.bukkit.block.Block.class);
        when(block.getType()).thenReturn(org.bukkit.Material.OAK_SHELF);
        when(block.getState()).thenReturn(mock(Shelf.class));
        org.bukkit.event.player.PlayerInteractEvent event = mock(org.bukkit.event.player.PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player); when(event.getClickedBlock()).thenReturn(block);
        when(event.getAction()).thenReturn(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK);
        doReturn(false).when(protection).tryCooldown(any(), any(), anyLong());
        try (MockedStatic<BuildMode> build = mockStatic(BuildMode.class)) {
            build.when(BuildMode::getInstance).thenReturn(mock(BuildMode.class));
            protection.onBlockInteract(event);
            verify(event).setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            verify(event).setCancelled(true);
        }
    }
    @Test void repeatedWearCannotBreakGearButExcludedWorldsRemainUnchanged() throws Exception {
        WorldProtect protection = protection();
        PlayerItemDamageEvent event = mock(PlayerItemDamageEvent.class);
        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(mock(Location.class));
        when(event.getPlayer()).thenReturn(player);
        for (int i = 0; i < 1000; i++) protection.onItemDamage(event);
        verify(event, times(1000)).setCancelled(true);
        doReturn(true).when(protection).inDisabledWorld(any(Location.class));
        reset(event); when(event.getPlayer()).thenReturn(player);
        protection.onItemDamage(event);
        verify(event, never()).setCancelled(anyBoolean());
    }
    @Test void barrelAndShelfStorageRejectOpenClickAndDragButAllowBuildersAndMenus() throws Exception {
        WorldProtect protection = protection();
        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(mock(Location.class));
        BuildMode buildMode = mock(BuildMode.class);
        try (MockedStatic<BuildMode> build = mockStatic(BuildMode.class)) {
            build.when(BuildMode::getInstance).thenReturn(buildMode);
            for (InventoryHolder holder : new InventoryHolder[]{mock(Barrel.class), mock(Shelf.class)}) {
                Inventory inventory = mock(Inventory.class); when(inventory.getHolder()).thenReturn(holder);
                InventoryView view = mock(InventoryView.class); when(view.getTopInventory()).thenReturn(inventory);
                InventoryOpenEvent open = mock(InventoryOpenEvent.class);
                when(open.getPlayer()).thenReturn(player); when(open.getInventory()).thenReturn(inventory);
                InventoryClickEvent click = mock(InventoryClickEvent.class);
                when(click.getWhoClicked()).thenReturn(player); when(click.getView()).thenReturn(view);
                InventoryDragEvent drag = mock(InventoryDragEvent.class);
                when(drag.getWhoClicked()).thenReturn(player); when(drag.getView()).thenReturn(view);
                protection.onContainerOpen(open); protection.onContainerClick(click); protection.onContainerDrag(drag);
                verify(open).setCancelled(true); verify(click).setCancelled(true); verify(drag).setCancelled(true);
                clearInvocations(open); when(buildMode.isPresent(any())).thenReturn(true);
                protection.onContainerOpen(open); verify(open, never()).setCancelled(anyBoolean());
                when(buildMode.isPresent(any())).thenReturn(false);
            }
            Inventory menu = mock(Inventory.class); when(menu.getHolder()).thenReturn(mock(InventoryHolder.class));
            InventoryOpenEvent open = mock(InventoryOpenEvent.class);
            when(open.getPlayer()).thenReturn(player); when(open.getInventory()).thenReturn(menu);
            protection.onContainerOpen(open); verify(open, never()).setCancelled(anyBoolean());
        }
    }
}


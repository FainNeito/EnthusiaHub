package net.badgersmc.enthusiahub.module.modules.hotbar;

import org.bukkit.Bukkit;
import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.module.Module;
import net.badgersmc.enthusiahub.module.ModuleType;
import net.badgersmc.enthusiahub.module.modules.hotbar.items.PvPSwordItem;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

public class PvPSwordModule extends Module {

    private PvPSwordItem pvpSwordItem;

    public PvPSwordModule(EnthusiaHubPlugin plugin) {
        super(plugin, ModuleType.PVP_SWORD);
    }

    @Override
    public void onEnable() {
        // pvpSwordItem resolved lazily — HotbarManager may not be enabled yet
    }

    @Override
    public void onDisable() {
        if (pvpSwordItem != null) pvpSwordItem.shutdown();
        pvpSwordItem = null;
    }

    private PvPSwordItem resolvePvPSwordItem() {
        if (pvpSwordItem == null) {
            HotbarManager hotbarManager = (HotbarManager) getPlugin().getModuleManager().getModule(ModuleType.HOTBAR_ITEMS);
            if (hotbarManager != null) {
                pvpSwordItem = hotbarManager.getPvPSwordItem();
            }
        }
        return pvpSwordItem;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        PvPSwordItem item = resolvePvPSwordItem();
        if (item == null) return;

        Player player = event.getEntity();
        if (!item.isInPvPMode(player)) return;

        FileConfiguration config = getConfig(ConfigType.SETTINGS);
        Location deathLoc = player.getLocation();

        if (config.getBoolean("pvp_sword.death.lightning", true)) {
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                deathLoc.getWorld().strikeLightningEffect(deathLoc);
            });
        }

        if (config.getBoolean("pvp_sword.death.knockback.enabled", true)) {
            boolean onlyPvPPlayers = config.getBoolean("pvp_sword.death.knockback.only_pvp_players", true);
            double knockbackStrength = config.getDouble("pvp_sword.death.knockback.strength", 1.5);
            double radius = 5.0;

            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                for (Entity entity : deathLoc.getWorld().getNearbyEntities(deathLoc, radius, radius, radius)) {
                    if (entity instanceof Player nearby && !nearby.equals(player)) {
                        if (onlyPvPPlayers && !item.isInPvPMode(nearby)) {
                            continue;
                        }
                        Vector direction = nearby.getLocation().toVector().subtract(deathLoc.toVector());
                        if (direction.lengthSquared() == 0) continue;
                        direction.normalize();
                        direction.setY(0.5);
                        nearby.setVelocity(direction.multiply(knockbackStrength));
                    }
                }
            });
        }

        if (config.getBoolean("pvp_sword.death.remove_items", true)) {
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cleanup handled by PvPSwordItem
    }

    public boolean isInPvPMode(Player player) {
        PvPSwordItem item = resolvePvPSwordItem();
        return item != null && item.isInPvPMode(player);
    }
}

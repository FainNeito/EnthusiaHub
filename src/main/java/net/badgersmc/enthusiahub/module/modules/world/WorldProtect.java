package net.badgersmc.enthusiahub.module.modules.world;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.Permissions;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.config.Messages;
import net.badgersmc.enthusiahub.cooldown.CooldownType;
import net.badgersmc.enthusiahub.module.Module;
import net.badgersmc.enthusiahub.module.ModuleType;
import net.badgersmc.enthusiahub.module.modules.hologram.Hologram;
import net.badgersmc.enthusiahub.module.modules.hotbar.PvPSwordModule;
import net.badgersmc.enthusiahub.module.modules.player.PvPMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class WorldProtect extends Module {
    FileConfiguration config = getConfig(ConfigType.SETTINGS);
    private final List<Material> interactables = Arrays.asList(
            Material.ANVIL,
            Material.ARMOR_STAND,
            Material.BARREL,
            Material.BEACON,
            Material.BREWING_STAND,
            Material.CAULDRON,
            Material.CARTOGRAPHY_TABLE,
            Material.CHISELED_BOOKSHELF,
            Material.COMMAND_BLOCK,
            Material.COMPARATOR,
            Material.COMPOSTER,
            Material.CRAFTING_TABLE,
            Material.CRAFTER,
            Material.DAYLIGHT_DETECTOR,
            Material.DECORATED_POT,
            Material.DISPENSER,
            Material.DROPPER,
            Material.ENCHANTING_TABLE,
            Material.FLETCHING_TABLE,
            Material.FLOWER_POT,
            Material.FURNACE,
            Material.GRINDSTONE,
            Material.HOPPER,
            Material.HOPPER_MINECART,
            Material.ITEM_FRAME,
            Material.JUKEBOX,
            Material.LEAD,
            Material.LEVER,
            Material.LOOM,
            Material.MINECART,
            Material.NOTE_BLOCK,
            Material.PAINTING,
            Material.REDSTONE_WIRE,
            Material.REPEATER,
            Material.RESPAWN_ANCHOR,
            Material.SMITHING_TABLE,
            Material.SMOKER,
            Material.SPAWNER,
            Material.STONECUTTER,
            Material.STRUCTURE_BLOCK,
            Material.TRIAL_SPAWNER,
            Material.BLAST_FURNACE,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL
    );

    private boolean itemDurability;
    private boolean hungerLoss;
    private boolean fallDamage;
    private boolean weatherChange;
    private boolean deathMessage;
    private boolean fireSpread;
    private boolean leafDecay;
    private boolean mobSpawning;
    private boolean blockBurn;
    private boolean voidDeath;
    private boolean itemDrop;
    private boolean itemPickup;
    private boolean blockBreak;
    private boolean blockPlace;
    private boolean blockInteract;
    private boolean playerPvP;
    private boolean playerDrowning;
    private boolean fireDamage;

    public WorldProtect(EnthusiaHubPlugin plugin) {
        super(plugin, ModuleType.WORLD_PROTECT);
    }

    @Override
    public void onEnable() {
        FileConfiguration config = getConfig(ConfigType.SETTINGS);
        itemDurability = config.getBoolean("world_settings.disable_item_durability", true);
        hungerLoss = config.getBoolean("world_settings.disable_hunger_loss");
        fallDamage = config.getBoolean("world_settings.disable_fall_damage");
        playerPvP = config.getBoolean("world_settings.disable_player_pvp");
        voidDeath = config.getBoolean("world_settings.disable_void_death");
        weatherChange = config.getBoolean("world_settings.disable_weather_change");
        deathMessage = config.getBoolean("world_settings.disable_death_message");
        mobSpawning = config.getBoolean("world_settings.disable_mob_spawning");
        itemDrop = config.getBoolean("world_settings.disable_item_drop");
        itemPickup = config.getBoolean("world_settings.disable_item_pickup");
        blockBreak = config.getBoolean("world_settings.disable_block_break");
        blockPlace = config.getBoolean("world_settings.disable_block_place");
        blockInteract = config.getBoolean("world_settings.disable_block_interact");
        blockBurn = config.getBoolean("world_settings.disable_block_burn");
        fireSpread = config.getBoolean("world_settings.disable_block_fire_spread");
        leafDecay = config.getBoolean("world_settings.disable_block_leaf_decay");
        playerDrowning = config.getBoolean("world_settings.disable_drowning");
        fireDamage = config.getBoolean("world_settings.disable_fire_damage");
    }

    @Override
    public void onDisable() {
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (!inDisabledWorld(event.getPlayer().getLocation()) && (itemDurability || isInHubPvP(event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    private boolean isInHubPvP(Player player) {
        if (config.getBoolean("pvp_sword.enabled")) {
            PvPSwordModule sword = (PvPSwordModule) getPlugin().getModuleManager().getModule(ModuleType.PVP_SWORD);
            if (sword != null && sword.isInPvPMode(player)) return true;
        }
        if (config.getBoolean("pvp_mode.enabled")) {
            PvPMode legacy = (PvPMode) getPlugin().getModuleManager().getModule(ModuleType.PVP_MODE);
            return legacy != null && legacy.isPlayerInPvPMode(player.getUniqueId());
        }
        return false;
    }

    private boolean blocksContainer(Player player, Inventory inventory) {
        if (!blockInteract || inDisabledWorld(player.getLocation())) return false;
        if (!(inventory.getHolder() instanceof BlockState) && !(inventory.getHolder() instanceof DoubleChest)) return false;
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return false;
        return !config.getBoolean("legacySystems.permissionsEnabled")
                || !player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && blocksContainer(player, event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && blocksContainer(player, event.getView().getTopInventory())) {
            // Includes shift-click, number-key swaps, offhand swaps and collect-to-cursor.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && blocksContainer(player, event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
    // Prevent sign editing
    @EventHandler(priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) return;
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
            Messages.EVENT_BLOCK_INTERACT.send(player);
        }
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent event) {
        for (Hologram entry : getPlugin().getHologramManager().getHolograms()) {
            for (ArmorStand stand : entry.getStands()) {
                if (stand.equals(event.getRightClicked())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!blockBreak || event.isCancelled()) return;

        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) return;
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);

        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_BREAK, 3)) {
            Messages.EVENT_BLOCK_BREAK.send(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!blockPlace || event.isCancelled()) return;

        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) return;
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.AIR) return;
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String hotbarItem = meta.getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING);
            if (hotbarItem != null) {
                event.setCancelled(true);
                return;
            }
        }

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_PLACE.getPermission())) return;
        }


        event.setCancelled(true);

        if (tryCooldown(event.getPlayer().getUniqueId(), CooldownType.BLOCK_PLACE, 3)) {
            Messages.EVENT_BLOCK_PLACE.send(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Player player = event.getPlayer();
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material type = block.getType();
            // Explicitly deny block use even when another handler permits the held item.

            // Check generic interactables first
            if (block.getState() instanceof org.bukkit.inventory.InventoryHolder || interactables.contains(type)) {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setCancelled(true);
                if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                    Messages.EVENT_BLOCK_INTERACT.send(player);
                }
                return;
            }

            // Check type patterns
            if (type.name().contains("_DOOR") || type.name().contains("_TRAPDOOR") || type.name().contains("_BUTTON") || type.name().contains("_SIGN") || type.name().contains("_GATE") || type.name().contains("CHEST") || type.name().contains("_FENCE_GATE") || type.name().contains("POTTED_") || type.name().contains("_BED") || type.name().contains("_BOAT")) {

                event.setCancelled(true);
                if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                    Messages.EVENT_BLOCK_INTERACT.send(player);
                }
                return;
            }
        } else if (event.getAction() == Action.PHYSICAL && block.getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!blockBurn) return;
        if (inDisabledWorld(event.getBlock().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onFireSpread(BlockIgniteEvent event) {
        if (!fireSpread) return;
        if (inDisabledWorld(event.getBlock().getLocation())) return;
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!hungerLoss) return;

        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (inDisabledWorld(player.getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropEvent(PlayerDropItemEvent event) {
        if (!itemDrop) return;

        Player player = event.getPlayer();

        if (inDisabledWorld(player.getLocation())) return;

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_ITEM_DROP.getPermission())) return;
        }


        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);

        if (tryCooldown(player.getUniqueId(), CooldownType.ITEM_DROP, 3)) {
            Messages.EVENT_ITEM_DROP.send(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupEvent(EntityPickupItemEvent event) {
        if (!itemDrop) return;
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (inDisabledWorld(player.getLocation())) return;
            if (config.getBoolean("legacySystems.permissionsEnabled")) {
                if (player.hasPermission(Permissions.EVENT_ITEM_PICKUP.getPermission())) return;
            }
            if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.ITEM_PICKUP, 3)) {
                Messages.EVENT_ITEM_PICKUP.send(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!leafDecay) return;
        if (inDisabledWorld(event.getBlock().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!mobSpawning) return;
        if (inDisabledWorld(event.getEntity().getLocation())) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!weatherChange || inDisabledWorld(event.getWorld())) return;
        event.setCancelled(event.toWeatherState());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!deathMessage || inDisabledWorld(event.getEntity().getLocation())) return;
        if (BuildMode.getInstance().isPresent(event.getEntity().getUniqueId())) event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (inDisabledWorld(player.getLocation())) return;
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        switch (cause) {
            case FALL:
                if (fallDamage) event.setCancelled(true);
                break;
            case DROWNING:
                if (playerDrowning) event.setCancelled(true);
                break;
            case FIRE:
            case FIRE_TICK:
                if (isInHubPvP(player)) return;
            case LAVA:
                if (fireDamage) event.setCancelled(true);
                break;
            case VOID: {
                if (voidDeath) {
                    player.setFallDistance(0.0F);
                    Location location = ((LobbySpawn) getPlugin().getModuleManager().getModule(ModuleType.LOBBY)).getLocation();
                    if (location == null) return;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> player.teleport(location), 3L);
                    event.setCancelled(true);
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!playerPvP) return;

        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();

        if (inDisabledWorld(victim.getLocation())) return;

        // BadgersMC: bypass for minigames (LumaSG etc.)
        if (victim.hasPermission(Permissions.PVP_BYPASS.getPermission())) return;
        if (event.getDamager() instanceof Player
                && event.getDamager().hasPermission(Permissions.PVP_BYPASS.getPermission())) return;
        if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player
                    && ((Player) proj.getShooter()).hasPermission(Permissions.PVP_BYPASS.getPermission())) return;
        }

        if (config.getBoolean("pvp_sword.enabled")) {
            Player attacker = event.getDamager() instanceof Player direct ? direct :
                    event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter ? shooter : null;
            if (attacker != null) {
                PvPSwordModule sword = (PvPSwordModule) getPlugin().getModuleManager().getModule(ModuleType.PVP_SWORD);
                if (sword == null || !sword.isInPvPMode(attacker) || !sword.isInPvPMode(victim)) event.setCancelled(true);
                return;
            }
        }

        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (config.getBoolean("pvp_mode.enabled")) {
                PvPMode pvpMode = (PvPMode) getPlugin().getModuleManager().getModule(ModuleType.PVP_MODE);
                if (pvpMode.isPlayerInPvPMode(attacker.getUniqueId())) {
                    if (!pvpMode.isPlayerInPvPMode(victim.getUniqueId())) {
                        if (tryCooldown(attacker.getUniqueId(), CooldownType.VICTIM_NOT_IN_PVP_MODE, 3)) {
                            Messages.PVP_MODE_VICTIM_NOT_IN_PVP_MODE.send(attacker, "%victim%", victim.getDisplayName());
                        }
                        event.setCancelled(true);
                    }
                    return;
                }
                if (pvpMode.isPlayerInPvPMode(attacker.getUniqueId()) && pvpMode.isPlayerInPvPMode(victim.getUniqueId()))
                    return;
            }
            event.setCancelled(true);
        }

        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player attacker = (Player) projectile.getShooter();
                if (config.getBoolean("pvp_mode.enabled")) {
                    PvPMode pvpMode = (PvPMode) getPlugin().getModuleManager().getModule(ModuleType.PVP_MODE);
                    if (pvpMode.isPlayerInPvPMode(attacker.getUniqueId()) && pvpMode.isPlayerInPvPMode(victim.getUniqueId()))
                        return;
                }
                event.setCancelled(true);
            }
        }

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (event.getDamager().hasPermission(Permissions.EVENT_PLAYER_PVP.getPermission())) return;
        }


        event.setCancelled(true);
        if (tryCooldown(event.getDamager().getUniqueId(), CooldownType.PLAYER_PVP, 3)) {
            Messages.EVENT_PLAYER_PVP.send(event.getDamager());
        }
    }

    // Prevent destroying of item frame/paintings
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDestroy(HangingBreakByEntityEvent event) {
        if (!blockBreak || inDisabledWorld(event.getEntity().getLocation())) return;
        Entity entity = event.getEntity();
        Entity player = event.getRemover();

        if (entity instanceof Painting || entity instanceof ItemFrame && player instanceof Player) {
            if (player != null) {
                if (config.getBoolean("legacySystems.permissionsEnabled")) {
                    if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) return;
                }
                if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
                event.setCancelled(true);
                if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_BREAK, 3)) {
                    Messages.EVENT_BLOCK_BREAK.send(player);
                }
            }
        }
    }

    // Prevent items being rotated in item frame
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getRightClicked().getLocation())) return;
        Entity entity = event.getRightClicked();
        Entity player = event.getPlayer();
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        if (entity instanceof ItemFrame) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    // Prevent items being taken from item frames
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getEntity().getLocation())) return;
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        if (entity instanceof ItemFrame && damager instanceof Player) {
            Player player = (Player) damager;
            if (config.getBoolean("legacySystems.permissionsEnabled")) {
                if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
            }
            if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    // Prevent books being taken from lecterns
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerTakeLecternBookEvent event) {
        if (!blockInteract || inDisabledWorld(event.getLectern().getLocation())) return;
        Entity player = event.getPlayer();

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
            Messages.EVENT_BLOCK_INTERACT.send(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBoatInteract(PlayerInteractEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Player player = event.getPlayer();
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        // Check for all boat types
        if (event.getItem() != null &&
                (event.getItem().getType().name().contains("_BOAT"))) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    @EventHandler
    public void onPlayerBoatInteraction(PlayerInteractEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        if (entity instanceof Boat) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleBreak(VehicleDestroyEvent event) {
        if (!blockBreak || inDisabledWorld(event.getVehicle().getLocation())) return;

        if (!(event.getAttacker() instanceof Player)) return;
        Player player = (Player) event.getAttacker();

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        if (event.getVehicle() instanceof Boat || event.getVehicle() instanceof Minecart) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_BREAK, 3)) {
                Messages.EVENT_BLOCK_BREAK.send(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (event.getPlayer().hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(event.getPlayer().getUniqueId())) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && (clickedBlock.getType() == Material.CHISELED_BOOKSHELF || clickedBlock.getType() == Material.DECORATED_POT)) {
                event.setCancelled(true);

                if (tryCooldown(event.getPlayer().getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                    Messages.EVENT_BLOCK_INTERACT.send(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onMinecartInteraction(PlayerInteractEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        if (entity instanceof Minecart ||
                entity instanceof StorageMinecart ||
                entity instanceof HopperMinecart ||
                entity instanceof CommandMinecart ||
                entity instanceof ExplosiveMinecart) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMinecartPlacement(PlayerInteractEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Player player = event.getPlayer();
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        // Check for all minecart types
        if (event.getItem() != null &&
                (event.getItem().getType() == Material.MINECART ||
                        event.getItem().getType() == Material.CHEST_MINECART ||
                        event.getItem().getType() == Material.FURNACE_MINECART ||
                        event.getItem().getType() == Material.HOPPER_MINECART ||
                        event.getItem().getType() == Material.TNT_MINECART ||
                        event.getItem().getType() == Material.COMMAND_BLOCK_MINECART)) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }
}

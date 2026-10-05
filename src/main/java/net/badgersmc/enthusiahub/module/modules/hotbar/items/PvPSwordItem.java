package net.badgersmc.enthusiahub.module.modules.hotbar.items;

import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import net.badgersmc.enthusiahub.module.modules.world.BuildMode;
import org.bukkit.persistence.PersistentDataType;
import net.badgersmc.enthusiahub.utility.TextUtil;
import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.config.Messages;
import net.badgersmc.enthusiahub.module.modules.hotbar.HotbarItem;
import net.badgersmc.enthusiahub.module.modules.hotbar.HotbarManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class PvPSwordItem extends HotbarItem {

    private final BukkitScheduler scheduler = Bukkit.getScheduler();
    private final List<UUID> pvpMode = new CopyOnWriteArrayList<>();
    private final Map<UUID, BukkitTask> countdowns = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public PvPSwordItem(HotbarManager hotbarManager, ItemStack item, int slot, String key) {
        super(hotbarManager, item, slot, key);
        markUnbreakable(item);
    }

    @Override
    protected void onInteract(Player player) {
        if (getHotbarManager().inDisabledWorld(player.getLocation()) || BuildMode.getInstance().isPresent(player.getUniqueId())) return;
        // Check if player is already counting down
        if (countdowns.containsKey(player.getUniqueId())) {
            Messages.PVP_SWORD_ALREADY_COUNTING.send(player);
            return;
        }

        boolean isInPvP = pvpMode.contains(player.getUniqueId());
        startCountdown(player, !isInPvP);
    }

    private void startCountdown(Player player, boolean enabling) {
        FileConfiguration config = getHotbarManager().getConfig(ConfigType.SETTINGS);
        int duration = config.getInt("pvp_sword.countdown.duration", 5);

        // Send initial message
        if (enabling) {
            Messages.PVP_SWORD_COUNTDOWN_START_ENABLE.send(player);
        } else {
            Messages.PVP_SWORD_COUNTDOWN_START_DISABLE.send(player);
        }

        AtomicInteger timeLeft = new AtomicInteger(duration);

        BukkitTask task = scheduler.runTaskTimer(getPlugin(), () -> {
            if (!player.isOnline() || getHotbarManager().inDisabledWorld(player.getLocation()) || BuildMode.getInstance().isPresent(player.getUniqueId())) {
                cancelCountdown(player);
                return;
            }

            if (timeLeft.get() <= 0) {
                cancelCountdown(player);

                if (enabling) {
                    enablePvPMode(player);
                } else {
                    disablePvPMode(player);
                }
                return;
            }

            // Send title and subtitle
            String action = enabling ? "ENABLE" : "DISABLE";
            String titleFormat = config.getString("pvp_sword.countdown.title_format", "&c{time}");
            String subtitleFormat = config.getString("pvp_sword.countdown.subtitle_format", "&7Switching to &c{action} &7mode");

            String title = titleFormat.replace("{time}", String.valueOf(timeLeft.get()));
            String subtitle = subtitleFormat.replace("{action}", action);

            String actionbar = config.getString("pvp_sword.countdown.actionbar_format", "&cPvP Mode {action} in &f{time}s")
                    .replace("{action}", action).replace("{time}", String.valueOf(timeLeft.get()));
            player.sendActionBar(TextUtil.color(actionbar));

            player.sendTitle(
                    TextUtil.color(title),
                    TextUtil.color(subtitle),
                    0, 25, 5
            );

            // Play tick sound
            if (config.getBoolean("pvp_sword.countdown.sound.enabled", true)) {
                String tickSoundName = config.getString("pvp_sword.countdown.sound.tick_sound", "BLOCK_NOTE_BLOCK_HAT");
                try {
                    Sound tickSound = Sound.valueOf(tickSoundName);
                    player.playSound(player.getLocation(), tickSound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    getPlugin().getLogger().warning("Invalid tick sound: " + tickSoundName);
                }
            }

            timeLeft.decrementAndGet();
        }, 0L, 20L); // Every second

        countdowns.put(player.getUniqueId(), task);
    }

    private void cancelCountdown(Player player) {
        BukkitTask task = countdowns.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void enablePvPMode(Player player) {
        pvpMode.add(player.getUniqueId());
        Messages.PVP_SWORD_PVP_ENABLED.send(player);

        FileConfiguration config = getHotbarManager().getConfig(ConfigType.SETTINGS);

        // Play completion sound
        if (config.getBoolean("pvp_sword.countdown.sound.enabled", true)) {
            String completeSoundName = config.getString("pvp_sword.countdown.sound.complete_sound", "ENTITY_PLAYER_LEVELUP");
            try {
                Sound completeSound = Sound.valueOf(completeSoundName);
                player.playSound(player.getLocation(), completeSound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                getPlugin().getLogger().warning("Invalid complete sound: " + completeSoundName);
            }
        }

        // Apply armor
        applyArmor(player);
    }

    private void disablePvPMode(Player player) {
        pvpMode.remove(player.getUniqueId());
        Messages.PVP_SWORD_PVP_DISABLED.send(player);

        FileConfiguration config = getHotbarManager().getConfig(ConfigType.SETTINGS);

        // Play completion sound
        if (config.getBoolean("pvp_sword.countdown.sound.enabled", true)) {
            String completeSoundName = config.getString("pvp_sword.countdown.sound.complete_sound", "ENTITY_PLAYER_LEVELUP");
            try {
                Sound completeSound = Sound.valueOf(completeSoundName);
                player.playSound(player.getLocation(), completeSound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                getPlugin().getLogger().warning("Invalid complete sound: " + completeSoundName);
            }
        }

        // Remove armor
        removeArmor(player);
    }

    private void applyArmor(Player player) {
        FileConfiguration config = getHotbarManager().getConfig(ConfigType.SETTINGS);
        boolean randomTrim = config.getBoolean("pvp_sword.armor.random_trim", true);

        // Get random trim if enabled
        TrimMaterial trimMaterial = null;
        TrimPattern trimPattern = null;

        if (randomTrim) {
            try {
                List<TrimMaterial> materials = new java.util.ArrayList<>();
                org.bukkit.Registry.TRIM_MATERIAL.forEach(materials::add);

                List<TrimPattern> patterns = new java.util.ArrayList<>();
                org.bukkit.Registry.TRIM_PATTERN.forEach(patterns::add);

                if (!materials.isEmpty() && !patterns.isEmpty()) {
                    trimMaterial = materials.get(random.nextInt(materials.size()));
                    trimPattern = patterns.get(random.nextInt(patterns.size()));
                }
            } catch (Exception e) {
                getPlugin().getLogger().warning("Could not apply random armor trims (possibly running < 1.20): " + e.getMessage());
            }
        }

        // Apply helmet
        ItemStack helmet = createArmor("helmet", trimMaterial, trimPattern);
        if (helmet != null) player.getInventory().setHelmet(helmet);

        // Apply chestplate
        ItemStack chestplate = createArmor("chestplate", trimMaterial, trimPattern);
        if (chestplate != null) player.getInventory().setChestplate(chestplate);

        // Apply leggings
        ItemStack leggings = createArmor("leggings", trimMaterial, trimPattern);
        if (leggings != null) player.getInventory().setLeggings(leggings);

        // Apply boots
        ItemStack boots = createArmor("boots", trimMaterial, trimPattern);
        if (boots != null) player.getInventory().setBoots(boots);
    }

    private ItemStack createArmor(String armorPiece, TrimMaterial trimMaterial, TrimPattern trimPattern) {
        FileConfiguration config = getHotbarManager().getConfig(ConfigType.SETTINGS);
        ConfigurationSection armorSection = config.getConfigurationSection("pvp_sword.armor." + armorPiece);

        if (armorSection == null) return null;

        String materialName = armorSection.getString("material");
        if (materialName == null) return null;

        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            getPlugin().getLogger().warning("Invalid armor material: " + materialName);
            return null;
        }

        ItemStack armor = new ItemStack(material);
        ItemMeta meta = armor.getItemMeta();

        if (meta != null) {
            meta.setUnbreakable(true);
            // Apply enchantments
            List<String> enchantments = armorSection.getStringList("enchantments");
            for (String enchantString : enchantments) {
                String[] parts = enchantString.split(":");
                if (parts.length == 2) {
                    try {
                        Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                        int level = Integer.parseInt(parts[1]);
                        if (enchant != null) {
                            meta.addEnchant(enchant, level, true);
                        }
                    } catch (Exception e) {
                        getPlugin().getLogger().warning("Invalid enchantment: " + enchantString);
                    }
                }
            }

            // Apply random trim if available
            if (trimMaterial != null && trimPattern != null && meta instanceof ArmorMeta armorMeta) {
                try {
                    armorMeta.setTrim(new ArmorTrim(trimMaterial, trimPattern));
                } catch (Exception e) {
                    getPlugin().getLogger().warning("Could not apply armor trim: " + e.getMessage());
                }
            }

            armor.setItemMeta(meta);
        }

        return armor;
    }

    private void removeArmor(Player player) {
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
    }

    private boolean isSword(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && getKey().equals(meta.getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING));
    }

    public static void markUnbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setUnbreakable(true); item.setItemMeta(meta); }
    }

    public void shutdown() {
        countdowns.values().forEach(BukkitTask::cancel);
        countdowns.clear();
        for (UUID uuid : pvpMode) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) removeArmor(player);
        }
        pvpMode.clear();
    }

    @EventHandler
    public void onWorldExit(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        cancelCountdown(player);
        if (pvpMode.remove(player.getUniqueId())) removeArmor(player);
    }

    public boolean isInPvPMode(Player player) {
        return pvpMode.contains(player.getUniqueId());
    }

    public List<UUID> getPvPModePlayers() {
        return List.copyOf(pvpMode);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cancelCountdown(player);

        // Remove armor if player is in PvP mode
        if (pvpMode.contains(player.getUniqueId())) {
            removeArmor(player);
        }

        pvpMode.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        super.onPlayerDeath(event);
        Player player = event.getEntity();

        // Exit PvP mode on death if configured
        FileConfiguration config = getHotbarManager().getConfig(ConfigType.SETTINGS);
        if (config.getBoolean("pvp_sword.death.exit_pvp_mode", true)) {
            if (pvpMode.contains(player.getUniqueId())) {
                pvpMode.remove(player.getUniqueId());
                // Armor will be removed naturally on death
            }
        }

        // Cancel any active countdown
        cancelCountdown(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (pvpMode.contains(player.getUniqueId())) {
            scheduler.runTask(getPlugin(), () -> {
                if (player.isOnline() && !getHotbarManager().inDisabledWorld(player.getLocation())) applyArmor(player);
            });
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Prevent dropping PvP armor while in PvP mode
        if (pvpMode.contains(player.getUniqueId())) {
            ItemStack item = event.getItemDrop().getItemStack();
            Material type = item.getType();

            // Check if it's armor
            if (type.name().endsWith("_HELMET") ||
                type.name().endsWith("_CHESTPLATE") ||
                type.name().endsWith("_LEGGINGS") ||
                type.name().endsWith("_BOOTS")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());

        if (getHotbarManager().inDisabledWorld(player.getLocation()) || BuildMode.getInstance().isPresent(player.getUniqueId())) {
            return;
        }

        boolean switchingToSword = false;
        boolean switchingFromSword = false;

        // Check if switching TO the PvP sword
        if (newItem != null) {
            if (isSword(newItem)) {
                switchingToSword = true;
            }
        }

        // Check if switching FROM the PvP sword
        if (oldItem != null) {
            if (isSword(oldItem)) {
                switchingFromSword = true;
            }
        }

        if (switchingToSword != switchingFromSword && countdowns.containsKey(player.getUniqueId())) {
            cancelCountdown(player);
            Messages.PVP_SWORD_COUNTDOWN_CANCELLED.send(player);
        }

        // If switching TO sword and not already in PvP mode, start activation countdown
        if (switchingToSword && !switchingFromSword) {
            if (!pvpMode.contains(player.getUniqueId()) && !countdowns.containsKey(player.getUniqueId())) {
                startCountdown(player, true);
            }
        }

        // If switching away from sword while in PvP mode, start deactivation countdown
        if (switchingFromSword && !switchingToSword) {
            if (pvpMode.contains(player.getUniqueId()) && !countdowns.containsKey(player.getUniqueId())) {
                startCountdown(player, false);
            } else if (countdowns.containsKey(player.getUniqueId())) {
                // Cancel countdown if switching away during activation
                cancelCountdown(player);
                Messages.PVP_SWORD_COUNTDOWN_CANCELLED.send(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        super.onInventoryClick(event);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Prevent moving PvP armor while in PvP mode
        if (pvpMode.contains(player.getUniqueId())) {
            // Check current item (item being clicked)
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem != null) {
                Material type = currentItem.getType();

                // Check if it's armor in armor slots (slots 36-39)
                if (event.getSlot() >= 36 && event.getSlot() <= 39 &&
                    (type.name().endsWith("_HELMET") ||
                     type.name().endsWith("_CHESTPLATE") ||
                     type.name().endsWith("_LEGGINGS") ||
                     type.name().endsWith("_BOOTS"))) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Also check cursor item (item being placed) for armor slots
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && event.getSlot() >= 36 && event.getSlot() <= 39) {
                Material type = cursorItem.getType();
                if (type.name().endsWith("_HELMET") ||
                    type.name().endsWith("_CHESTPLATE") ||
                    type.name().endsWith("_LEGGINGS") ||
                    type.name().endsWith("_BOOTS")) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Block shift-click of armor
            if (event.isShiftClick() && currentItem != null) {
                Material type = currentItem.getType();
                if (type.name().endsWith("_HELMET") ||
                    type.name().endsWith("_CHESTPLATE") ||
                    type.name().endsWith("_LEGGINGS") ||
                    type.name().endsWith("_BOOTS")) {
                    event.setCancelled(true);
                }
            }
        }
    }
}

package net.badgersmc.enthusiahub.module.modules.hotbar;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.module.modules.world.BuildMode;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.module.Module;
import net.badgersmc.enthusiahub.module.ModuleType;
import net.badgersmc.enthusiahub.module.modules.hotbar.items.CustomItem;
import net.badgersmc.enthusiahub.module.modules.hotbar.items.PlayerHider;
import net.badgersmc.enthusiahub.utility.ItemStackBuilder;
import net.badgersmc.enthusiahub.module.modules.hotbar.items.PvPSwordItem;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HotbarManager extends Module {

	private List<HotbarItem> hotbarItems;
    private PvPSwordItem pvpSwordItem;
	private int _joinSlot;

	public HotbarManager(EnthusiaHubPlugin plugin) {
		super(plugin, ModuleType.HOTBAR_ITEMS);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		changeToJoinSlot(event.getPlayer());
	}

	@Override
	public void onEnable() {
		hotbarItems = new ArrayList<>();
		FileConfiguration config = getConfig(ConfigType.SETTINGS);

		if (config.getBoolean("hotbar.joinslot")) {
			_joinSlot = config.getInt("hotbar.slot_number");
		}

		if (config.getBoolean("custom_join_items.enabled")) {

			for (String entry : Objects.requireNonNull(config.getConfigurationSection("custom_join_items.items")).getKeys(false)) {
				ItemStack item = ItemStackBuilder.getItemStack(config.getConfigurationSection("custom_join_items.items." + entry)).build();
				CustomItem customItem = new CustomItem(this, item, config.getInt("custom_join_items.items." + entry + ".slot"), entry);

				if (config.contains("custom_join_items.items." + entry + ".permission")) {
					customItem.setPermission(config.getString("custom_join_items.items." + entry + ".permission"));
				}

				customItem.setConfigurationSection(config.getConfigurationSection("custom_join_items.items." + entry));
				customItem.setAllowMovement(config.getBoolean("custom_join_items.disable_inventory_movement"));
				registerHotbarItem(customItem);
			}

		}

		if (config.getBoolean("player_hider.enabled")) {
			ItemStack item = ItemStackBuilder.getItemStack(config.getConfigurationSection("player_hider.not_hidden")).build();
			PlayerHider playerHider = new PlayerHider(this, item, config.getInt("player_hider.slot"), "PLAYER_HIDER");

			playerHider.setAllowMovement(config.getBoolean("player_hider.disable_inventory_movement"));

			registerHotbarItem(playerHider);
		}

        if (config.getBoolean("pvp_sword.enabled")) {
            ItemStack sword = ItemStackBuilder.getItemStack(compatibleSwordConfig(config.getConfigurationSection("pvp_sword.sword"))).build();
            pvpSwordItem = new PvPSwordItem(this, sword, config.getInt("pvp_sword.slot", 2), "PVP_SWORD");
            if (config.contains("pvp_sword.permission")) pvpSwordItem.setPermission(config.getString("pvp_sword.permission"));
            pvpSwordItem.setAllowMovement(config.getBoolean("pvp_sword.disable_inventory_movement", true));
            registerHotbarItem(pvpSwordItem);
        }
        giveItems();
	}

	@Override
	public void onDisable() {
        if (pvpSwordItem != null) pvpSwordItem.shutdown();
        removeItems();
        if (hotbarItems != null) hotbarItems.forEach(HandlerList::unregisterAll);
        pvpSwordItem = null;
	}

    public PvPSwordItem getPvPSwordItem() { return pvpSwordItem; }

    // Historical PvP configuration uses "SHARPNESS:1"; current hotbar builders use YAML maps.
    static ConfigurationSection compatibleSwordConfig(ConfigurationSection source) {
        Objects.requireNonNull(source, "Missing pvp_sword.sword configuration");
        ConfigurationSection copy = new YamlConfiguration().createSection("sword", source.getValues(true));
        List<?> entries = source.getList("enchantments");
        if (entries != null) {
            List<Object> normalized = new ArrayList<>();
            for (Object entry : entries) {
                if (entry instanceof String text) {
                    int separator = text.lastIndexOf(':');
                    if (separator <= 0) throw new IllegalArgumentException("Invalid PvP sword enchantment: " + text);
                    java.util.LinkedHashMap<String, Integer> enchantment = new java.util.LinkedHashMap<>();
                    enchantment.put(text.substring(0, separator).trim(), Integer.parseInt(text.substring(separator + 1).trim()));
                    normalized.add(enchantment);
                } else {
                    normalized.add(entry);
                }
            }
            copy.set("enchantments", normalized);
        }
        return copy;
    }

	public List<HotbarItem> getHotbarItems() {
		return hotbarItems;
	}

	public void registerHotbarItem(HotbarItem hotbarItem) {
		getPlugin().getServer().getPluginManager().registerEvents(hotbarItem, getPlugin());
		hotbarItems.add(hotbarItem);
	}

	public void changeToJoinSlot(Player player) {
		player.getInventory().setHeldItemSlot(_joinSlot);
	}

	public void giveItems(Player player) {
		hotbarItems.stream()
				.filter(p -> !inDisabledWorld(player.getLocation()) && !BuildMode.getInstance().isPresent(player.getUniqueId()))
				.forEach(hotbarItem -> hotbarItem.giveItem(player));
	}

	private void giveItems() {
		Bukkit.getOnlinePlayers().stream()
				.filter(player -> !inDisabledWorld(player.getLocation())
						&& !BuildMode.getInstance().isPresent(player.getUniqueId()))
				.forEach(player -> hotbarItems.forEach(hotbarItem -> hotbarItem.giveItem(player)));
	}

	private void removeItems() {
		Bukkit.getOnlinePlayers().stream()
				.filter(player -> !inDisabledWorld(player.getLocation()))
				.forEach(player -> hotbarItems.forEach(hotbarItem -> hotbarItem.removeItem(player)));
	}

}

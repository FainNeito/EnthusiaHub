package net.badgersmc.enthusiahub.module.modules.hotbar;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.module.modules.world.BuildMode;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.module.Module;
import net.badgersmc.enthusiahub.module.ModuleType;
import net.badgersmc.enthusiahub.module.modules.hotbar.items.CustomItem;
import net.badgersmc.enthusiahub.module.modules.hotbar.items.PlayerHider;
import net.badgersmc.enthusiahub.utility.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HotbarManager extends Module {

	private List<HotbarItem> hotbarItems;
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

        SurvivalEntrySettings entry = SurvivalEntrySettings.read(config);
        if (entry != null) {
            var section = config.getConfigurationSection("start_survival.item");
            if (section == null) {
                var defaults = new org.bukkit.configuration.file.YamlConfiguration();
                section = defaults.createSection("item");
                section.set("material", "GRASS_BLOCK");
                section.set("display_name", "&a&lStart Survival &7(Use item)");
                section.set("lore", List.of("&7Find wilderness, build a home.", "&7Use this item to join the SMP."));
            }
            registerHotbarItem(new net.badgersmc.enthusiahub.module.modules.hotbar.items.StartSurvivalItem(
                    this, ItemStackBuilder.getItemStack(section).build(), entry));
        } else if (config.getBoolean("start_survival.enabled", false)) {
            getPlugin().getLogger().warning("Start Survival disabled: invalid target/slot or a reserved hotbar slot.");
        }
		giveItems();

	}

	@Override
	public void onDisable() {
		removeItems();
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

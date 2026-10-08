package net.badgersmc.enthusiahub.module.modules.hotbar.items;

import net.badgersmc.enthusiahub.action.actions.ProxyAction;
import net.badgersmc.enthusiahub.module.modules.hotbar.HotbarItem;
import net.badgersmc.enthusiahub.module.modules.hotbar.HotbarManager;
import net.badgersmc.enthusiahub.module.modules.hotbar.SurvivalEntrySettings;
import net.badgersmc.enthusiahub.module.modules.world.BuildMode;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.utility.TextUtil;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class StartSurvivalItem extends HotbarItem {
    private final SurvivalEntrySettings settings;

    public StartSurvivalItem(HotbarManager manager, ItemStack item, SurvivalEntrySettings settings) {
        super(manager, item, settings.slot(), "START_SURVIVAL");
        this.settings = settings;
        setAllowMovement(true);
    }

    @Override
    public void giveItem(Player player) {
        if (getHotbarManager().inDisabledWorld(player.getLocation())
                || BuildMode.getInstance().isPresent(player.getUniqueId())) return;
        ItemStack existing = player.getInventory().getItem(getSlot());
        if (existing != null && existing.getType() != Material.AIR
                && existing.getType() != Material.CAVE_AIR && existing.getType() != Material.VOID_AIR
                && !owned(existing)) return;
        super.giveItem(player);
    }

    @Override
    @EventHandler(priority = EventPriority.HIGHEST)
    public void hotbarPlayerJoin(PlayerJoinEvent event) {
        super.hotbarPlayerJoin(event);
        Player player = event.getPlayer();
        if (settings.selectOnJoin() && !BuildMode.getInstance().isPresent(player.getUniqueId())
                && !getHotbarManager().inDisabledWorld(player.getLocation())
                && owned(player.getInventory().getItem(getSlot()))) {
            player.getInventory().setHeldItemSlot(getSlot());
        }
        welcomeFirstJoin(player);
    }

    private void welcomeFirstJoin(Player player) {
        if (!player.isOnline() || player.hasPlayedBefore()
                || BuildMode.getInstance().isPresent(player.getUniqueId())
                || getHotbarManager().inDisabledWorld(player.getLocation())
                || !owned(player.getInventory().getItem(getSlot()))) return;
        var config = getHotbarManager().getConfig(ConfigType.SETTINGS);
        if (config == null || !config.getBoolean("start_survival.welcome.enabled", false)) return;
        var messages = config.contains("start_survival.welcome.messages")
                ? config.getStringList("start_survival.welcome.messages")
                : List.of("&a&lWelcome to Enthusia!",
                        "&fFind a home, complete challenges and earn Raw Gold in Survival.",
                        "&eSelect the Start Survival item and use it to join SMP.");
        for (String message : messages) {
            if (!message.isBlank()) player.sendMessage(TextUtil.color(message));
        }
    }

    @Override
    protected void onInteract(Player player) {
        if (!player.isOnline() || BuildMode.getInstance().isPresent(player.getUniqueId())
                || getHotbarManager().inDisabledWorld(player.getLocation())) return;
        new ProxyAction().execute(getPlugin(), player, settings.server());
    }

    private boolean owned(ItemStack item) {
        return item != null && item.hasItemMeta() && "START_SURVIVAL".equals(item.getItemMeta()
                .getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING));
    }
}

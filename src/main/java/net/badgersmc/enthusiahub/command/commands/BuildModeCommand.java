package net.badgersmc.enthusiahub.command.commands;

import cl.bgmp.minecraft.util.commands.CommandContext;
import cl.bgmp.minecraft.util.commands.annotations.Command;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.Permissions;
import net.badgersmc.enthusiahub.module.modules.world.BuildMode;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.config.Messages;
import net.badgersmc.enthusiahub.module.ModuleType;
import net.badgersmc.enthusiahub.module.modules.player.PvPMode;
import net.badgersmc.enthusiahub.module.modules.hotbar.PvPSwordModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BuildModeCommand {
	private final EnthusiaHubPlugin plugin;
	private static final EnthusiaHubPlugin PLUGIN = JavaPlugin.getPlugin(EnthusiaHubPlugin.class);

	public BuildModeCommand(EnthusiaHubPlugin plugin) {
		this.plugin = plugin;
	}

	@Command(
			aliases = {"buildmode"},
			desc = "Toggle build mode."
	)
	public void buildMode(final CommandContext args, final CommandSender sender) throws CommandException {
		FileConfiguration config = PLUGIN.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
		final BuildMode bm = BuildMode.getInstance();
		Player target;
		if(args.argsLength() >= 1){
			if (!(sender.hasPermission(Permissions.COMMAND_BUILD_MODE_OTHERS.getPermission()))) {
				Messages.NO_PERMISSION.send(sender);
				return;
			}
			target = Bukkit.getPlayer(args.getString(0));
			if(target == null){
				Messages.BUILD_MODE_COMMAND_TARGET_NOT_FOUND.send(sender, "%target%", args.getString(0));
				return;
			}

			if (config.getBoolean("pvp_mode.enabled") && !config.getBoolean("multiple_worlds")) {
				PvPMode pvpMode = (PvPMode) plugin.getModuleManager().getModule(ModuleType.PVP_MODE);
				if (pvpMode.isPlayerInPvPMode(target.getUniqueId())) {
					Messages.BUILD_MODE_COMMAND_TARGET_IN_PVP_MODE.send(sender, "%target%", target.getDisplayName());
					return;
				}
			}



		}else{
			if (!(sender instanceof Player)) throw new CommandException("Console cannot use build mode");
			target = (Player) sender;

			if (config.getBoolean("pvp_mode.enabled") && !config.getBoolean("multiple_worlds")) {
				if(((PvPMode) plugin.getModuleManager().getModule(ModuleType.PVP_MODE)).isPlayerInPvPMode(target.getUniqueId())){
					Messages.BUILD_MODE_COMMAND_IN_PVP_MODE.send(sender);
					return;
				}
			}

			if (!(sender.hasPermission(Permissions.COMMAND_BUILD_MODE.getPermission())) || !(sender.hasPermission(Permissions.COMMAND_BUILD_MODE_OTHERS.getPermission()))) {
				if(BuildMode.getInstance().isPresent(((Player) sender).getUniqueId())){
					remove(bm, target);
					return;
				}
				Messages.NO_PERMISSION.send(sender);
				return;
			}
		}

        PvPSwordModule swordMode = (PvPSwordModule) plugin.getModuleManager().getModule(ModuleType.PVP_SWORD);
        if (swordMode != null && swordMode.isInPvPMode(target)) {
            Messages.BUILD_MODE_COMMAND_TARGET_IN_PVP_MODE.send(sender, "%target%", target.getDisplayName());
            return;
        }
		if (bm.isPresent(target.getUniqueId())) {
			remove(bm, target);
			return;
		}
		bm.addPlayer(target);
		Messages.BUILD_MODE_ENABLED.send(target);
		if(args.argsLength() >= 1) Messages.BUILD_MODE_COMMAND_ENABLED_FOR_TARGET.send(sender, "%target%", target.getDisplayName());
	}

	private void remove(BuildMode instance, Player target){
		instance.removePlayer(target.getUniqueId());
		target.setAllowFlight(true);
		Messages.BUILD_MODE_DISABLED.send(target);
	}
}

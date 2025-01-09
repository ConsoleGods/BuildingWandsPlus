package com.optimusprimerdc.buildingwandsplus.commands;

import com.optimusprimerdc.buildingwandsplus.BuildingWandsPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {
    private final BuildingWandsPlus plugin;

    public ReloadCommand(BuildingWandsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("Reload command executed by: " + sender.getName());

        // Handle the /wands reload command for both players and console
        if (sender instanceof Player) {
            // Handle permission for players
            Player player = (Player) sender;
            if (player.hasPermission("buildingwandsplus.command.reload")) {
                plugin.reloadConfig();
                player.sendMessage("§6§lBuilding Wands has reloaded!"); // Message for players
                plugin.getLogger().info("Config reloaded by player: " + player.getName());
            } else {
                player.sendMessage("You do not have permission to reload the config.");
                plugin.getLogger().info("Player " + player.getName() + " tried to reload config without permission.");
            }
        } else {
            // Console users are allowed to use this command by default
            plugin.reloadConfig();
            sender.sendMessage("Config has been reloaded!"); // Message for console
            plugin.getLogger().info("Config reloaded by console.");
        }
        return true;
    }
}

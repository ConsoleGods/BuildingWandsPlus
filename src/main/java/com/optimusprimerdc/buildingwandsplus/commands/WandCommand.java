package com.optimusprimerdc.buildingwandsplus.commands;

import com.optimusprimerdc.buildingwandsplus.BuildingWandsPlus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class WandCommand implements CommandExecutor {
    private final BuildingWandsPlus plugin;

    public WandCommand(BuildingWandsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                ItemStack wand = new ItemStack(Material.STICK); // Example wand item
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6§lBuilding Wand");
                    meta.setLore(Arrays.asList(
                        "",
                        "§6Right Click to Build",
                        "§4Shift Left Click on Block to UNDO",
                        ""
                    ));
                    wand.setItemMeta(meta);
                }
                target.getInventory().addItem(wand);
                sender.sendMessage("Gave a wand to " + target.getName());
                target.sendMessage("You have received a §6§lBuilding Wand!");
                return true;
            } else {
                sender.sendMessage("Player not found.");
                return false;
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("buildingwandsplus.command.reload")) {
                plugin.reloadPluginConfig();
                sender.sendMessage("§6§lBuilding Wands has reloaded!");
                plugin.getLogger().info("Config reloaded by: " + sender.getName());
                return true;
            } else {
                sender.sendMessage("You do not have permission to reload the config.");
                return false;
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("undo")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                plugin.getWandListener().handleUndoOperation(player);
                return true;
            } else {
                sender.sendMessage("Only players can use this command.");
                return false;
            }
        } else {
            sender.sendMessage("Usage: /wands <give|reload|undo> [playername]");
            return false;
        }
    }
}
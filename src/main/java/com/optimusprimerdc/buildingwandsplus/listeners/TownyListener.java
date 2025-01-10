package com.optimusprimerdc.buildingwandsplus.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.object.TownyPermission;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class TownyListener implements Listener {

    private final TownyAPI townyAPI = TownyAPI.getInstance();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Allow OP players and players with override permission to place blocks
        if (player.isOp() || player.hasPermission("buildingwandsplus.override")) {
            return; // OP players and players with override permission can place blocks
        }

        // Check if the player has permission to build in this location
        boolean canBuild = PlayerCacheUtil.getCachePermission(player, event.getBlock().getLocation(), event.getBlock().getType(), TownyPermission.ActionType.BUILD);
        if (!canBuild) {
            player.sendMessage(ChatColor.RED + "You do not have permission to build here!");
            event.setCancelled(true);
        }
    }
}

package com.optimusprimerdc.buildingwandsplus.listeners;

import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.object.TownyPermission;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.Location;

public class TownyListener implements Listener {

    public boolean canBuild(Player player, Location location) {
        // Allow OP players and players with override permission to place blocks
        if (player.isOp() || player.hasPermission("buildingwandsplus.override")) {
            return true; // OP players and players with override permission can place blocks
        }

        // Check if the player has permission to build in this location
        boolean canBuild = PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.BUILD);
        return canBuild;
    }
}

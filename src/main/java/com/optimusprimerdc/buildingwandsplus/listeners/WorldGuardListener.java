package com.optimusprimerdc.buildingwandsplus.listeners;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardListener {

    private final WorldGuardPlugin wgPlugin;

    public WorldGuardListener(Plugin plugin) {
        this.wgPlugin = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");
    }

    public boolean isLocationProtected(Location location, Player player) {
        if (wgPlugin == null || !wgPlugin.isEnabled()) {
            return false; // WorldGuard is not enabled
        }

        // Allow OP players and players with override permission to place blocks
        if (player.isOp() || player.hasPermission("buildingwandsplus.override")) {
            return false; // OP players and players with override permission can place blocks
        }

        // Get WorldGuard and the RegionManager for the current world
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) {
            return false; // No regions in this world
        }

        // Convert Bukkit Location to WorldEdit's BlockVector3
        BlockVector3 blockVector = BukkitAdapter.asBlockVector(location);

        // Get regions applicable to the location
        ApplicableRegionSet regions = regionManager.getApplicableRegions(blockVector);

        // Check if the player has permission in any of the regions
        boolean canBuild = regions.testState(wgPlugin.wrapPlayer(player), Flags.BUILD);
        return !canBuild; // Return true if the player cannot build (location is protected)
    }
}
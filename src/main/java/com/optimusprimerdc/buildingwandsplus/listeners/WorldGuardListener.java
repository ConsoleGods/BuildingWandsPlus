package com.optimusprimerdc.buildingwandsplus.listeners;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardListener {

    public WorldGuardListener(Plugin plugin) {
        // No need to store the plugin reference if it's not used
    }

    public boolean isLocationProtected(Location location, Player player) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {
            return false;
        }

        BlockVector3 blockVector = BukkitAdapter.asBlockVector(location);
        ApplicableRegionSet applicableRegionSet = regionManager.getApplicableRegions(blockVector);

        return !applicableRegionSet.testState(WorldGuardPlugin.inst().wrapPlayer(player), Flags.BUILD);
    }
}
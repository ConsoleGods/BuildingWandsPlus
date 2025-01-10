package com.optimusprimerdc.buildingwandsplus.listeners;

import com.optimusprimerdc.buildingwandsplus.BuildingWandsPlus;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.object.TownyPermission;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {
    private final BuildingWandsPlus plugin;
    private final WorldGuardListener worldGuardListener;
    private final TownyListener townyListener;
    private int maxBlockLimit;
    private Material wandItem;
    private final Map<UUID, LinkedList<List<Block>>> playerBlockHistory = new HashMap<>();
    private final File playerHistoryFile;
    private final File undoHistoryFile;
    private final FileConfiguration playerHistoryConfig;
    private final FileConfiguration undoHistoryConfig;

    public WandListener(BuildingWandsPlus plugin) {
        this.plugin = plugin;
        this.worldGuardListener = new WorldGuardListener(plugin);
        this.townyListener = new TownyListener();
        reloadConfig();
        playerHistoryFile = new File(plugin.getDataFolder(), "playerhistory.yml");
        undoHistoryFile = new File(plugin.getDataFolder(), "undohistory.yml");
        playerHistoryConfig = YamlConfiguration.loadConfiguration(playerHistoryFile);
        undoHistoryConfig = YamlConfiguration.loadConfiguration(undoHistoryFile);
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.maxBlockLimit = config.getInt("max-block-limit", 64);
        this.wandItem = Material.getMaterial(config.getString("wand-item", "STICK").toUpperCase());
        if (this.wandItem == null) {
            this.wandItem = Material.STICK; // Default to STICK if the config value is invalid
        }
    }

    @EventHandler
    public void onPlayerUseWand(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == wandItem) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                handleBlockPlacement(event, player, item);
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
                handleUndoOperation(player);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        if (worldGuardListener.isLocationProtected(location, player)) {
            player.sendMessage(ChatColor.RED + "You cannot break blocks in a protected area!");
            event.setCancelled(true);
        }
    }

    private void handleBlockPlacement(PlayerInteractEvent event, Player player, ItemStack item) {
        // Allow OP players and players with override permission to place blocks
        if (player.isOp() || player.hasPermission("buildingwandsplus.override")) {
            return; // OP players and players with override permission can place blocks
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            Material blockType = clickedBlock.getType();
            BlockData blockData = clickedBlock.getBlockData();
            BlockFace face = event.getBlockFace(); // Get the clicked face

            int length = Math.min(maxBlockLimit, 64); // Ensure the length does not exceed the max limit

            // Build in the direction of the clicked face
            Block targetBlock = clickedBlock;
            List<Block> placedBlocks = new LinkedList<>();
            for (int i = 0; i < length; i++) {
                targetBlock = targetBlock.getRelative(face); // Place in the direction of the clicked face

                if (!targetBlock.getType().isAir()) {
                    // Stop building if a block is already there
                    break;
                }

                // Check if the block placement is in a protected area (WorldGuard)
                if (worldGuardListener.isLocationProtected(targetBlock.getLocation(), player)) {
                    player.sendMessage(ChatColor.RED + "You cannot place blocks in a protected area!");
                    event.setCancelled(true); // Cancel the event to prevent block placement
                    return; // Exit the method to stop further processing
                }

                // Check if the block placement is in a protected area (Towny)
                boolean canBuild = PlayerCacheUtil.getCachePermission(player, targetBlock.getLocation(), targetBlock.getType(), TownyPermission.ActionType.BUILD);
                if (!canBuild) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to build here!");
                    event.setCancelled(true); // Cancel the event to prevent block placement
                    return; // Exit the method to stop further processing
                }

                // Check if the block placement is directly towards the player
                if (isPlayerTooClose(player, targetBlock)) {
                    break;
                }

                // Place block based on game mode
                if (player.getGameMode() == GameMode.CREATIVE) {
                    targetBlock.setType(blockType);
                    targetBlock.setBlockData(blockData);
                    placedBlocks.add(targetBlock);
                } else if (player.getGameMode() == GameMode.SURVIVAL) {
                    if (player.getInventory().contains(blockType)) {
                        targetBlock.setType(blockType);
                        targetBlock.setBlockData(blockData);
                        player.getInventory().removeItem(new ItemStack(blockType, 1));
                        placedBlocks.add(targetBlock);
                    } else {
                        player.sendMessage("§cYou don't have enough blocks.");
                        break;
                    }
                }
            }
            if (!placedBlocks.isEmpty()) {
                playerBlockHistory.computeIfAbsent(player.getUniqueId(), k -> new LinkedList<>()).addFirst(placedBlocks);
                logPlayerAction(player, placedBlocks, playerHistoryConfig, playerHistoryFile);
            }
        }
    }

    /**
     * Check if the block being placed is too close to the player.
     *
     * @param player      The player
     * @param targetBlock The block being placed
     * @return True if the target block is too close to the player, false otherwise
     */
    private boolean isPlayerTooClose(Player player, Block targetBlock) {
        Location playerLocation = player.getLocation();
        Location blockLocation = targetBlock.getLocation();

        // Check distance between player's position and block
        return blockLocation.distance(playerLocation) < 1.5; // Adjust distance as needed
    }

    public void handleUndoOperation(Player player) {
        LinkedList<List<Block>> history = playerBlockHistory.get(player.getUniqueId());
        if (history != null && !history.isEmpty()) {
            List<Block> placedBlocks = history.removeFirst();
            logUndoAction(player, placedBlocks, undoHistoryConfig, undoHistoryFile);
            for (Block block : placedBlocks) {
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    player.getInventory().addItem(new ItemStack(block.getType(), 1));
                }
                block.setType(Material.AIR);
            }
            player.sendMessage("§6§lBuilding Wands: Last operation undone!");
        } else {
            player.sendMessage("§6§lBuilding Wands: No operation to undo!");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerBlockHistory.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Log player actions to a YAML file.
     *
     * @param player The player
     * @param blocks The list of blocks involved in the action
     * @param config The YAML configuration to update
     * @param file   The file to save the configuration to
     */
    private void logPlayerAction(Player player, List<Block> blocks, FileConfiguration config, File file) {
        String playerName = player.getName();
        for (Block block : blocks) {
            Location loc = block.getLocation();
            String path = playerName + "." + System.currentTimeMillis();
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".type", block.getType().toString());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Log undo actions to a YAML file.
     *
     * @param player The player
     * @param blocks The list of blocks involved in the undo action
     * @param config The YAML configuration to update
     * @param file The file to save the configuration to
     */
    private void logUndoAction(Player player, List<Block> blocks, FileConfiguration config, File file) {
        String playerName = player.getName();
        String path = playerName + "." + System.currentTimeMillis();
        Block initialBlock = blocks.get(0);
        Location loc = initialBlock.getLocation();
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".fromType", initialBlock.getType().toString());
        config.set(path + ".toType", Material.AIR.toString());
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Check if the player's bounding box intersects or is adjacent to the target block.
     *
     * @param player      The player
     * @param targetBlock The block being placed
     * @return True if the target block obstructs the player, false otherwise
     */
    private boolean isPlayerObstructed(Player player, Block targetBlock) {
        // Player's bounding box
        double playerMinX = player.getLocation().getX() - 0.3;
        double playerMaxX = player.getLocation().getX() + 0.3;
        double playerMinY = player.getLocation().getY();
        double playerMaxY = player.getLocation().getY() + 1.8; // Height of a player
        double playerMinZ = player.getLocation().getZ() - 0.3;
        double playerMaxZ = player.getLocation().getZ() + 0.3;

        // Target block's bounding box
        double blockMinX = targetBlock.getX();
        double blockMaxX = targetBlock.getX() + 1;
        double blockMinY = targetBlock.getY();
        double blockMaxY = targetBlock.getY() + 1;
        double blockMinZ = targetBlock.getZ();
        double blockMaxZ = targetBlock.getZ() + 1;

        // Check if the player's bounding box intersects with the target block's bounding box
        return playerMaxX > blockMinX && playerMinX < blockMaxX &&
               playerMaxY > blockMinY && playerMinY < blockMaxY &&
               playerMaxZ > blockMinZ && playerMinZ < blockMaxZ;
    }
}

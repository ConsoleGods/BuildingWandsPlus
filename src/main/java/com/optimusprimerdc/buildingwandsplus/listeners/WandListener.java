package com.optimusprimerdc.buildingwandsplus.listeners;

import com.optimusprimerdc.buildingwandsplus.BuildingWandsPlus;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Location;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {
    private final BuildingWandsPlus plugin;
    private int maxBlockLimit;
    private final Map<UUID, LinkedList<List<Block>>> playerBlockHistory = new HashMap<>();

    public WandListener(BuildingWandsPlus plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.maxBlockLimit = config.getInt("max-block-limit", 64);
    }

    @EventHandler
    public void onPlayerUseWand(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == Material.STICK) { // Example wand item
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                handleBlockPlacement(event, player, item);
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
                handleUndoOperation(player);
            }
        }
    }

    private void handleBlockPlacement(PlayerInteractEvent event, Player player, ItemStack item) {
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
    
                // ** Check if the block placement is directly towards the player **
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
                plugin.getLogger().info("Blocks placed by player " + player.getName() + ": " + placedBlocks.size());
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
            for (Block block : placedBlocks) {
                block.setType(Material.AIR);
            }
            player.sendMessage("§6§lBuilding Wands: Last operation undone!");
            plugin.getLogger().info("Undo operation performed by player " + player.getName());
        } else {
            player.sendMessage("§6§lBuilding Wands: No operation to undo!");
            plugin.getLogger().info("No operation to undo for player " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerBlockHistory.remove(event.getPlayer().getUniqueId());
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

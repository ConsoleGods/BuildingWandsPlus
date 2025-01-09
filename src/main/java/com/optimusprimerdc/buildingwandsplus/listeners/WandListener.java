package com.optimusprimerdc.buildingwandsplus.listeners;

import com.optimusprimerdc.buildingwandsplus.BuildingWandsPlus;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockData;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {

    private final BuildingWandsPlus plugin;
    private int maxBlockLimit;
    private int maxRange;
    private Material wandMaterial;
    private final Map<UUID, LinkedList<List<Block>>> playerBlockHistory = new HashMap<>();
    private final File playerHistoryFile;
    private final File undoHistoryFile;
    private final FileConfiguration playerHistoryConfig;
    private final FileConfiguration undoHistoryConfig;

    public WandListener(BuildingWandsPlus plugin) {
        this.plugin = plugin;
        reloadConfig();
        playerHistoryFile = new File(plugin.getDataFolder(), "playerhistory.yml");
        undoHistoryFile = new File(plugin.getDataFolder(), "undohistory.yml");
        playerHistoryConfig = YamlConfiguration.loadConfiguration(playerHistoryFile);
        undoHistoryConfig = YamlConfiguration.loadConfiguration(undoHistoryFile);
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.maxBlockLimit = config.getInt("max-block-limit", 64);
        this.maxRange = config.getInt("wand.max-range", 10);
        String materialName = config.getString("wand.material", "STICK");
        this.wandMaterial = Material.matchMaterial(materialName);
        if (this.wandMaterial == null) {
            this.wandMaterial = Material.STICK; // Default to STICK if the material is invalid
            plugin.getLogger().warning("Invalid wand material in config: " + materialName + ". Defaulting to STICK.");
        }
    }

    @EventHandler
    public void onPlayerUseWand(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == wandMaterial) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§6§lBuilding Wand")) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Block clickedBlock = event.getClickedBlock();
                    if (clickedBlock != null) {
                        double distance = player.getEyeLocation().distance(clickedBlock.getLocation());
                        if (distance <= maxRange) {
                            handleBlockPlacement(event, player, item);
                        } else {
                            player.sendMessage("§cYou are too far away to use the wand on this block.");
                        }
                    }
                } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
                    handleUndoOperation(player);
                }
            }
        }
    }

    private void handleBlockPlacement(PlayerInteractEvent event, Player player, ItemStack item) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            Material blockType = clickedBlock.getType();
            BlockData blockData = clickedBlock.getBlockData(); // Using BlockData

            // Get the direction the player is looking
            Vector direction = player.getLocation().getDirection().normalize(); // Normalize to ensure consistent movement

            int length = Math.min(maxBlockLimit, 64); // Ensure the length does not exceed the max limit

            // Build in the direction of the player's looking direction
            List<Block> placedBlocks = new LinkedList<>();
            for (int i = 0; i < length; i++) {
                // Calculate new position in the direction the player is looking
                Location newLocation = clickedBlock.getLocation().add(direction.multiply(i));
                Block targetBlock = newLocation.getBlock();

                if (!targetBlock.getType().isAir()) {
                    // Stop building if a block is already there
                    break;
                }

                // Check if the block placement is directly towards the player
                if (isPlayerTooClose(player, targetBlock)) {
                    player.sendMessage("§cYou cannot place blocks towards yourself!");
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
                logPlayerAction(player, placedBlocks, playerHistoryConfig, playerHistoryFile);
            }
        }
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
            logPlayerAction(player, placedBlocks, undoHistoryConfig, undoHistoryFile);
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
     * Check if the block being placed is too close to the player.
     *
     * @param player      The player
     * @param targetBlock The block being placed
     * @return True if the target block is too close to the player, false otherwise
     */
    private boolean isPlayerTooClose(Player player, Block targetBlock) {
        Location blockCenter = targetBlock.getLocation().add(0.5, 0.5, 0.5); // Center of the block
        Location playerLocation = player.getLocation();

        // Check if the player's distance from the block is less than 1.5 blocks
        return blockCenter.distance(playerLocation) < 1.5;
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
}

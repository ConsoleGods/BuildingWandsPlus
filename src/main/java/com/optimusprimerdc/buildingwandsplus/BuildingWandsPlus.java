package com.optimusprimerdc.buildingwandsplus;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.optimusprimerdc.buildingwandsplus.commands.WandCommand;
import com.optimusprimerdc.buildingwandsplus.listeners.WandListener;

public class BuildingWandsPlus extends JavaPlugin {

    private WandListener wandListener;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Ensure the config.yml is saved
        wandListener = new WandListener(this);
        getServer().getPluginManager().registerEvents(wandListener, this);
        this.getCommand("wands").setExecutor(new WandCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void reloadPluginConfig() {
        reloadConfig();
        wandListener.reloadConfig();
    }

    public WandListener getWandListener() {
        return wandListener;
    }
}

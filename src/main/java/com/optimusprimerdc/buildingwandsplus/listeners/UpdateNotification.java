package com.optimusprimerdc.buildingwandsplus.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

public class UpdateNotification {

    private static final Logger LOGGER = Bukkit.getLogger();
    private static boolean hasUpdate;
    private static String latestVersion = "";

    /**
     * Check whether a new build with a higher build number than the current build is available.
     */
    public static void doUpdateCheck(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String repo = "ConsoleGods/BuildingWandsPlus"; // Replace with your GitHub repository
                    URL url = new URL("https://api.github.com/repos/" + repo + "/releases/latest");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    JSONObject json = new JSONObject(content.toString());
                    latestVersion = json.getString("tag_name");
                    String currentVersion = plugin.getDescription().getVersion();

                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        hasUpdate = true;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (player.hasPermission("buildingwandsplus.admin")) {
                                    player.sendMessage("§6[BuildingWandsPlus] §eA new update is available: " + latestVersion + " (current version: " + currentVersion + ")");
                                    player.sendMessage("§6[BuildingWandsPlus] §eDownload it from: " + json.getString("html_url"));
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    LOGGER.severe("Failed to check for updates: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20 * 60 * 60); // Check every hour
    }

    /**
     * Trigger an update notification based on permissions. Useful to notify server administrators in-game.
     *
     * @param player The player to notify.
     */
    public static void doUpdateNotification(Player player) {
        if (player.hasPermission("buildingwandsplus.admin") && hasUpdate) {
            player.sendMessage("§6[BuildingWandsPlus] §eA new update is available: " + latestVersion);
            player.sendMessage("§6[BuildingWandsPlus] §eDownload it from: https://github.com/ConsoleGods/BuildingWandsPlus/releases/latest");
        }
    }
}

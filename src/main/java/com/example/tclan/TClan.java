package com.example.tclan;

import com.example.tclan.commands.ClanCommand;
import com.example.tclan.economy.EconomyManager;
import com.example.tclan.managers.ClanManager;
import com.example.tclan.placeholders.ClanPlaceholders;
import com.example.tclan.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class TClan extends JavaPlugin {

    private static TClan instance;
    private ClanManager clanManager;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        instance = this;

        // Configuration
        saveDefaultConfig(); // Copies config.yml from JAR if not present
        getConfig().options().copyDefaults(true);
        saveConfig(); // Saves the config file, ensuring defaults are written if it was newly created

        // Initialize EconomyManager
        economyManager = new EconomyManager(this);
        if (getConfig().getBoolean("economy.enabled", true)) {
            if (economyManager.setupEconomy()) {
                getLogger().info("Vault found and economy hooked successfully!");
            } else {
                getLogger().warning("Vault not found or no economy provider, economy features will be disabled (even if enabled in config).");
                // No need to set economyManager to null, its isEconomyEnabled() will handle it.
            }
        } else {
            getLogger().info("Economy features are disabled via config.yml.");
        }


        // Plugin startup logic
        this.clanManager = new ClanManager();
        // Future: Call clanManager.loadClans() here

        // Register commands
        if (this.getCommand("clan") != null) {
            this.getCommand("clan").setExecutor(new ClanCommand(this));
        } else {
            getLogger().severe("Command 'clan' not found in plugin.yml! Cannot register ClanCommand.");
        }


        // PlaceholderAPI Hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholders().register();
            getLogger().info("Successfully hooked into PlaceholderAPI and registered placeholders.");
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholders will not be available.");
        }

        getLogger().info("TClan plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // Future: Call clanManager.saveClans() here
        getLogger().info("TClan plugin has been disabled!");
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public static TClan getInstance() {
        return instance;
    }

    public static String getMessage(String path, String... replacements) {
        String message = getInstance().getConfig().getString("messages." + path);

        if (message == null || message.isEmpty()) {
            return ColorUtils.translateHexColorCodes(ChatColor.RED + "Missing message for " + path + " in config.yml");
        }

        if (replacements.length % 2 != 0) {
            // Log an error, but still try to process the message without replacements
            getInstance().getLogger().warning("Invalid number of replacements for message '" + path + "'. Replacements must be key-value pairs.");
        } else {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = "{" + replacements[i] + "}";
                String value = replacements[i + 1];
                if (value == null) value = "null"; // Prevent NPE from .replace if a replacement value is null
                message = message.replace(placeholder, value);
            }
        }

        return ColorUtils.translateHexColorCodes(message);
    }
}

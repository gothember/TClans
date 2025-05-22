package com.example.tclan;

import com.example.tclan.commands.ClanCommand;
import com.example.tclan.managers.ClanManager;
import com.example.tclan.placeholders.ClanPlaceholders;
import com.example.tclan.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class TClan extends JavaPlugin {

    private static TClan instance;
    private ClanManager clanManager;

    @Override
    public void onEnable() {
        instance = this;

        // Configuration
        saveDefaultConfig(); // Copies config.yml from JAR if not present
        getConfig().options().copyDefaults(true);
        saveConfig(); // Saves the config file, ensuring defaults are written if it was newly created

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
                message = message.replace(placeholder, value);
            }
        }

        return ColorUtils.translateHexColorCodes(message);
    }
}

package com.example.tclan.economy;

import com.example.tclan.TClan;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer; // Changed from Player for broader compatibility
import org.bukkit.entity.Player; // Still needed for some methods

public class EconomyManager {

    private static Economy econ = null;
    private static TClan plugin;

    public EconomyManager(TClan pluginInstance) {
        plugin = pluginInstance;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault plugin not found.");
            return false;
        }
        net.milkbowl.vault.chat.RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().info("No economy provider found (RSP is null).");
            return false;
        }
        econ = rsp.getProvider();
        if (econ == null) {
            plugin.getLogger().info("Economy provider found, but econ is null.");
        }
        return econ != null;
    }

    public boolean isEconomyEnabled() {
        // Also check the config flag from TClan's main config
        boolean configEnabled = TClan.getInstance().getConfig().getBoolean("economy.enabled", true);
        return econ != null && configEnabled;
    }

    public double getBalance(OfflinePlayer player) { // Changed to OfflinePlayer
        if (!isEconomyEnabled()) return 0.0;
        return econ.getBalance(player);
    }

    public boolean hasEnough(OfflinePlayer player, double amount) { // Changed to OfflinePlayer
        if (!isEconomyEnabled()) {
            // If economy is not enabled, but the feature is used,
            // assume the player "has enough" to not block features that might have a zero cost.
            // For actual costs, the command logic should check isEconomyEnabled() first.
            return true;
        }
        return econ.has(player, amount);
    }

    public boolean withdrawMoney(OfflinePlayer player, double amount) { // Changed to OfflinePlayer
        if (!isEconomyEnabled() || amount <= 0) return false;
        EconomyResponse r = econ.withdrawPlayer(player, amount);
        return r.transactionSuccess();
    }

    public boolean depositMoney(OfflinePlayer player, double amount) { // Changed to OfflinePlayer
        if (!isEconomyEnabled() || amount <= 0) return false;
        EconomyResponse r = econ.depositPlayer(player, amount);
        return r.transactionSuccess();
    }

    // Optional: Get currency symbol/name if needed for messages
    public String getCurrencySymbol() {
        if (!isEconomyEnabled()) return "";
        // Vault's API for currency name is a bit inconsistent (singular vs plural)
        // econ.currencyNameSingular() or econ.currencyNamePlural()
        // For simplicity, returning empty or a configured default.
        // Example: return econ.currencyNameSingular() != null ? econ.currencyNameSingular() : "$";
        return TClan.getInstance().getConfig().getString("economy.currency_symbol", "$"); // Default to $
    }
}

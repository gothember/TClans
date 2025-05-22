package com.example.tclan.placeholders;

import com.example.tclan.TClan;
import com.example.tclan.managers.ClanManager;
import com.example.tclan.models.Clan;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ClanPlaceholders extends PlaceholderExpansion {

    // No constructor needed for now, but if we needed TClan instance, we'd pass it here.
    // However, PlaceholderExpansion instances are typically instantiated by PAPI itself.

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tclan";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Jules (AI Assistant)";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // Allow PAPI to handle updates if eCloud is used
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return ""; // Or a default value if appropriate for all placeholders
        }

        ClanManager clanManager = TClan.getInstance().getClanManager();
        if (clanManager == null) { // Should not happen if plugin is enabled correctly
            return "Error: ClanManager not found";
        }

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());

        if (clan == null) {
            switch (params.toLowerCase()) {
                case "clan_leader":
                    return "No Clan";
                case "clan_balance":
                    return "0.00";
                case "clan_members":
                    return "0";
                default:
                    return null; // Let PAPI handle unknown placeholders for players not in a clan
            }
        }

        switch (params.toLowerCase()) {
            case "clan_leader":
                UUID leaderUuid = clan.getLeader();
                OfflinePlayer leader = Bukkit.getOfflinePlayer(leaderUuid);
                return leader.getName() != null ? leader.getName() : "Unknown";

            case "clan_balance":
                return String.format("%.2f", clan.getBalance());

            case "clan_members":
                return String.valueOf(clan.getMembers().size());

            default:
                return null; // Unknown placeholder
        }
    }
}

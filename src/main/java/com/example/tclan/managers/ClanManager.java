package com.example.tclan.managers;

import com.example.tclan.models.Clan;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClanManager {
    private final Map<String, Clan> clans = new HashMap<>();

    // Future persistence methods:
    // public void loadClans() { /* Logic to load clans from a file */ }
    // public void saveClans() { /* Logic to save clans to a file */ }

    public Clan createClan(String name, Player leader) {
        if (clans.containsKey(name.toLowerCase())) {
            return null; // Clan name already exists
        }
        Clan newClan = new Clan(name, leader.getUniqueId());
        clans.put(name.toLowerCase(), newClan);
        // Future: Call saveClans() here or manage saving periodically
        return newClan;
    }

    public Clan getClan(String name) {
        return clans.get(name.toLowerCase());
    }

    public Clan getClanByPlayer(UUID playerUuid) {
        for (Clan clan : clans.values()) {
            if (clan.isMember(playerUuid)) {
                return clan;
            }
        }
        return null;
    }

    /**
     * Removes a clan from the manager.
     * Note: Authorization (e.g., checking if the player is the leader)
     * should ideally be handled by the command executor before calling this method.
     *
     * @param name The name of the clan to disband.
     * @return true if the clan was found and removed, false otherwise.
     */
    public boolean disbandClan(String name) {
        Clan removedClan = clans.remove(name.toLowerCase());
        // Future: Call saveClans() here or manage saving periodically
        return removedClan != null;
    }

    // Method to get all clans, could be useful for admin commands or persistence
    public Map<String, Clan> getAllClans() {
        return new HashMap<>(clans); // Return a copy to prevent direct modification
    }
}

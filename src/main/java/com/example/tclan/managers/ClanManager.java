package com.example.tclan.managers;

import com.example.tclan.models.Clan;
import com.example.tclan.models.Invitation;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClanManager {
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, Invitation> pendingInvites = new ConcurrentHashMap<>(); // invitedPlayerUUID -> Invitation

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

    // --- Invitation Management ---

    /**
     * Adds an invitation to the pending invites.
     * @param invitedPlayerUUID The UUID of the player being invited.
     * @param clanName The name of the clan sending the invite.
     */
    public void addInvite(UUID invitedPlayerUUID, String clanName) {
        Invitation invitation = new Invitation(clanName, System.currentTimeMillis());
        pendingInvites.put(invitedPlayerUUID, invitation);
    }

    /**
     * Retrieves the pending invitation for a player.
     * @param invitedPlayerUUID The UUID of the player.
     * @return The Invitation object, or null if no pending invite exists.
     */
    public Invitation getInvite(UUID invitedPlayerUUID) {
        return pendingInvites.get(invitedPlayerUUID);
    }

    /**
     * Removes a pending invitation for a player.
     * @param invitedPlayerUUID The UUID of the player whose invite is to be removed.
     */
    public void removeInvite(UUID invitedPlayerUUID) {
        pendingInvites.remove(invitedPlayerUUID);
    }

    /**
     * (Optional) Cleans up expired invites.
     * This method is designed to be called periodically if needed.
     * For this implementation, timeout is checked ad-hoc in /clan accept.
     * @param timeoutMillis The duration after which an invite is considered expired.
     */
    public void cleanupExpiredInvites(long timeoutMillis) {
        long currentTime = System.currentTimeMillis();
        pendingInvites.entrySet().removeIf(entry -> {
            boolean expired = (currentTime - entry.getValue().timestamp()) > timeoutMillis;
            // if (expired) { System.out.println("Removing expired invite for " + entry.getKey()); } // For debugging
            return expired;
        });
    }
}

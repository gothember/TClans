package com.example.tclan.models;

import org.bukkit.Location;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Clan {
    private final String name;
    private UUID leader;
    private Set<UUID> members;
    private double balance;
    private Location homeLocation;

    public Clan(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
        this.balance = 0.0;
        this.homeLocation = null;
    }

    public String getName() {
        return name;
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Location getHomeLocation() {
        return homeLocation;
    }

    public void setHomeLocation(Location homeLocation) {
        this.homeLocation = homeLocation;
    }

    public void addMember(UUID memberUuid) {
        this.members.add(memberUuid);
    }

    public void removeMember(UUID memberUuid) {
        this.members.remove(memberUuid);
    }

    public boolean isMember(UUID memberUuid) {
        return this.members.contains(memberUuid);
    }

    public boolean isLeader(UUID memberUuid) {
        return this.leader.equals(memberUuid);
    }

    // Setter for leader - useful if leadership changes
    public void setLeader(UUID leader) {
        this.leader = leader;
    }
}

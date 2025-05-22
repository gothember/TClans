package com.example.tclan.models;

public record Invitation(String clanName, long timestamp) {
    // clanName: The name of the clan that sent the invitation.
    // timestamp: The time the invitation was created (System.currentTimeMillis()).
}

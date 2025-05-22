package com.example.tclan.commands;

import com.example.tclan.TClan;
import com.example.tclan.economy.EconomyManager;
import com.example.tclan.managers.ClanManager;
import com.example.tclan.models.Clan;
import com.example.tclan.models.Invitation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor {

    private final ClanManager clanManager;
    private final EconomyManager economyManager;
    private static final long DEFAULT_INVITE_TIMEOUT_SECONDS = 60;

    public ClanCommand(TClan plugin) {
        this.clanManager = plugin.getClanManager();
        this.economyManager = plugin.getEconomyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TClan.getMessage("command_only_by_player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String currencySymbol = economyManager.getCurrencySymbol(); // Get symbol once

        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args, currencySymbol);
                break;
            case "sethome":
                handleSetHomeCommand(player);
                break;
            case "home":
                handleHomeCommand(player);
                break;
            case "members":
                handleMembersCommand(player);
                break;
            case "invest":
                handleInvestCommand(player, args, currencySymbol);
                break;
            case "withdraw":
                handleWithdrawCommand(player, args, currencySymbol);
                break;
            case "balance":
                handleBalanceCommand(player, currencySymbol);
                break;
            case "invite":
                handleInviteCommand(player, args);
                break;
            case "accept":
                handleAcceptCommand(player, args);
                break;
            case "decline":
                handleDeclineCommand(player, args);
                break;
            default:
                sendUsage(player);
                break;
        }
        return true;
    }

    private void handleCreateCommand(Player player, String[] args, String currencySymbol) {
        if (args.length < 2) {
            player.sendMessage(TClan.getMessage("usage_generic_create"));
            return;
        }

        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(TClan.getMessage("error_already_in_clan"));
            return;
        }

        double creationCost = TClan.getInstance().getConfig().getDouble("clan_creation_cost", 1000.0);
        if (economyManager.isEconomyEnabled() && creationCost > 0) {
            if (!economyManager.hasEnough(player, creationCost)) {
                player.sendMessage(TClan.getMessage("error_insufficient_funds_player", "cost", String.format("%.2f", creationCost), "currency_symbol", currencySymbol));
                return;
            }
            if (!economyManager.withdrawMoney(player, creationCost)) {
                player.sendMessage(TClan.getMessage("error_economy_transaction_failed"));
                return;
            }
            player.sendMessage(TClan.getMessage("money_deducted", "amount", String.format("%.2f", creationCost), "currency_symbol", currencySymbol));
        } else if (creationCost > 0) { // Economy not enabled in plugin, but cost is set
             player.sendMessage(TClan.getMessage("error_economy_not_implemented_cost", "cost", String.format("%.2f", creationCost), "currency_symbol", currencySymbol));
        }


        String clanName = args[1];
        Clan newClan = clanManager.createClan(clanName, player);

        if (newClan != null) {
            player.sendMessage(TClan.getMessage("clan_created", "clan_name", clanName));
        } else {
            player.sendMessage(TClan.getMessage("error_clan_name_taken", "clan_name", clanName));
        }
    }

    private void handleSetHomeCommand(Player player) {
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(TClan.getMessage("error_must_be_leader_sethome"));
            return;
        }
        clan.setHomeLocation(player.getLocation());
        player.sendMessage(TClan.getMessage("clan_home_set"));
    }

    private void handleHomeCommand(Player player) {
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }
        Location homeLocation = clan.getHomeLocation();
        if (homeLocation == null) {
            player.sendMessage(TClan.getMessage("error_no_clan_home_set"));
            return;
        }
        player.teleport(homeLocation);
        player.sendMessage(TClan.getMessage("teleported_to_clan_home"));
    }

    private void handleMembersCommand(Player player) {
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }
        player.sendMessage(TClan.getMessage("members_header", "clan_name", clan.getName()));
        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
        String leaderName = leader.getName() != null ? leader.getName() : clan.getLeader().toString();
        player.sendMessage(TClan.getMessage("members_leader", "leader_name", leaderName));
        String membersListString = clan.getMembers().stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .filter(name -> name != null)
                .collect(Collectors.joining(", "));
        player.sendMessage(TClan.getMessage("members_list", "members_list", membersListString));
    }

    private void handleInvestCommand(Player player, String[] args, String currencySymbol) {
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(TClan.getMessage("usage_generic_invest"));
            return;
        }
        double value;
        try {
            value = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(TClan.getMessage("error_invalid_amount"));
            return;
        }
        if (value <= 0) {
            player.sendMessage(TClan.getMessage("error_amount_must_be_positive"));
            return;
        }

        if (economyManager.isEconomyEnabled()) {
            if (!economyManager.hasEnough(player, value)) {
                player.sendMessage(TClan.getMessage("error_insufficient_funds_player", "amount", String.format("%.2f", value), "currency_symbol", currencySymbol));
                return;
            }
            if (!economyManager.withdrawMoney(player, value)) {
                player.sendMessage(TClan.getMessage("error_economy_transaction_failed"));
                return;
            }
            // player.sendMessage(TClan.getMessage("money_deducted", "amount", String.format("%.2f", value), "currency_symbol", currencySymbol)); // Optional, invest_success might be enough
        } else {
            // If economy is not enabled, we might still allow investment if it's a conceptual thing without real money
            // For now, let's assume if economy is off, this command shouldn't really work with amounts
            player.sendMessage(TClan.getMessage("error_generic", "details", "Economy features are currently disabled."));
            return;
        }

        clan.setBalance(clan.getBalance() + value);
        player.sendMessage(TClan.getMessage("invest_success", "amount", String.format("%.2f", value), "balance", String.format("%.2f", clan.getBalance()), "currency_symbol", currencySymbol));
    }

    private void handleWithdrawCommand(Player player, String[] args, String currencySymbol) {
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(TClan.getMessage("error_must_be_leader_withdraw"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(TClan.getMessage("usage_generic_withdraw"));
            return;
        }
        double value;
        try {
            value = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(TClan.getMessage("error_invalid_amount"));
            return;
        }
        if (value <= 0) {
            player.sendMessage(TClan.getMessage("error_amount_must_be_positive"));
            return;
        }
        if (clan.getBalance() < value) {
            player.sendMessage(TClan.getMessage("error_insufficient_funds_clan", "balance", String.format("%.2f", clan.getBalance()), "currency_symbol", currencySymbol));
            return;
        }

        // Attempt to give money to player first if economy is enabled
        if (economyManager.isEconomyEnabled()) {
            if (!economyManager.depositMoney(player, value)) {
                player.sendMessage(TClan.getMessage("error_economy_transaction_failed"));
                // Do not proceed to alter clan balance if player deposit fails
                return;
            }
        } else {
            player.sendMessage(TClan.getMessage("error_generic", "details", "Economy features are currently disabled."));
            return;
        }

        clan.setBalance(clan.getBalance() - value);
        player.sendMessage(TClan.getMessage("withdraw_success", "amount", String.format("%.2f", value), "balance", String.format("%.2f", clan.getBalance()), "currency_symbol", currencySymbol));
    }

    private void handleBalanceCommand(Player player, String currencySymbol) {
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }
        player.sendMessage(TClan.getMessage("clan_balance", "balance", String.format("%.2f", clan.getBalance()), "currency_symbol", currencySymbol));
    }

    private void handleInviteCommand(Player player, String[] args) {
        Clan inviterClan = clanManager.getClanByPlayer(player.getUniqueId());
        if (inviterClan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }
        if (!inviterClan.isLeader(player.getUniqueId())) {
            player.sendMessage(TClan.getMessage("error_must_be_leader_invite"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(TClan.getMessage("usage_generic_invite"));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(TClan.getMessage("error_player_not_online", "player_name", args[1]));
            return;
        }
        if (player.getUniqueId().equals(targetPlayer.getUniqueId())) {
            player.sendMessage(TClan.getMessage("invite_self"));
            return;
        }
        if (clanManager.getClanByPlayer(targetPlayer.getUniqueId()) != null) {
            player.sendMessage(TClan.getMessage("error_player_already_in_clan", "player_name", targetPlayer.getName()));
            return;
        }
        if (clanManager.getInvite(targetPlayer.getUniqueId()) != null) {
            player.sendMessage(TClan.getMessage("error_player_has_pending_invite", "player_name", targetPlayer.getName()));
            return;
        }

        clanManager.addInvite(targetPlayer.getUniqueId(), inviterClan.getName());
        player.sendMessage(TClan.getMessage("invite_sent", "player_name", targetPlayer.getName()));
        targetPlayer.sendMessage(TClan.getMessage("invite_received", "clan_name", inviterClan.getName()));
    }

    private void handleAcceptCommand(Player player, String[] args) {
        Invitation invitation = clanManager.getInvite(player.getUniqueId());

        if (invitation == null) {
            player.sendMessage(TClan.getMessage("error_no_pending_invite"));
            return;
        }

        long inviteTimeoutMillis = TClan.getInstance().getConfig().getLong("invite_timeout_seconds", DEFAULT_INVITE_TIMEOUT_SECONDS) * 1000;
        if ((System.currentTimeMillis() - invitation.timestamp()) > inviteTimeoutMillis) {
            clanManager.removeInvite(player.getUniqueId());
            player.sendMessage(TClan.getMessage("error_invite_expired"));
            return;
        }

        String invitedClanName = invitation.clanName();
        if (args.length > 1 && !args[1].equalsIgnoreCase(invitedClanName)) {
            player.sendMessage(TClan.getMessage("error_invite_clan_mismatch", "clan_name", args[1]));
            return;
        }
        
        Clan clanToJoin = clanManager.getClan(invitedClanName);
        if (clanToJoin == null) {
            clanManager.removeInvite(player.getUniqueId());
            player.sendMessage(TClan.getMessage("error_generic", "details", "The clan that invited you no longer exists."));
            return;
        }
        
        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(TClan.getMessage("error_already_in_clan"));
            clanManager.removeInvite(player.getUniqueId());
            return;
        }

        clanToJoin.addMember(player.getUniqueId());
        clanManager.removeInvite(player.getUniqueId());

        player.sendMessage(TClan.getMessage("invite_accepted", "clan_name", clanToJoin.getName()));

        OfflinePlayer leader = Bukkit.getOfflinePlayer(clanToJoin.getLeader());
        if (leader.isOnline() && leader.getPlayer() != null) {
            leader.getPlayer().sendMessage(TClan.getMessage("player_joined_clan", "player_name", player.getName()));
        }
    }

    private void handleDeclineCommand(Player player, String[] args) {
        Invitation invitation = clanManager.getInvite(player.getUniqueId());

        if (invitation == null) {
            player.sendMessage(TClan.getMessage("error_no_pending_invite"));
            return;
        }
        
        String invitedClanName = invitation.clanName();
        // Optional: Check args[1] against invitedClanName if strict decline is needed

        clanManager.removeInvite(player.getUniqueId());
        player.sendMessage(TClan.getMessage("invite_declined", "clan_name", invitedClanName));

        Clan clanThatInvited = clanManager.getClan(invitedClanName);
        if (clanThatInvited != null) {
            OfflinePlayer leader = Bukkit.getOfflinePlayer(clanThatInvited.getLeader());
            if (leader.isOnline() && leader.getPlayer() != null) {
                // Consider adding TClan.getMessage("player_declined_your_invite", "player_name", player.getName())
            }
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(TClan.getMessage("usage_header"));
        player.sendMessage(TClan.getMessage("usage_create"));
        player.sendMessage(TClan.getMessage("usage_sethome"));
        player.sendMessage(TClan.getMessage("usage_home"));
        player.sendMessage(TClan.getMessage("usage_members"));
        player.sendMessage(TClan.getMessage("usage_balance"));
        player.sendMessage(TClan.getMessage("usage_invest"));
        player.sendMessage(TClan.getMessage("usage_withdraw"));
        player.sendMessage(TClan.getMessage("usage_invite"));
        player.sendMessage(TClan.getMessage("usage_accept_decline"));
    }
}

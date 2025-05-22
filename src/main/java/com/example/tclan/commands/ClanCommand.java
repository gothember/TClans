package com.example.tclan.commands;

import com.example.tclan.TClan;
import com.example.tclan.managers.ClanManager;
import com.example.tclan.models.Clan;
import com.example.tclan.utils.ColorUtils;
import org.bukkit.Bukkit;
// import org.bukkit.ChatColor; // No longer needed directly for messages
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor {

    // private final TClan plugin; // Not strictly needed if using TClan.getInstance()
    private final ClanManager clanManager;

    public ClanCommand(TClan plugin) {
        // this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
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

        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
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
                handleInvestCommand(player, args);
                break;
            case "withdraw":
                handleWithdrawCommand(player, args);
                break;
            case "balance":
                handleBalanceCommand(player);
                break;
            case "invite":
                handleInviteCommand(player, args);
                break;
            default:
                sendUsage(player);
                break;
        }
        return true;
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(TClan.getMessage("usage_generic_create"));
            return;
        }

        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(TClan.getMessage("error_already_in_clan"));
            return;
        }

        double creationCost = TClan.getInstance().getConfig().getDouble("clan_creation_cost", 1000.0);
        if (creationCost > 0) {
            // Simulate economy check for now
            // In a real scenario, you'd check player's balance via Vault API
            // For this step, we just inform them of the cost
            player.sendMessage(TClan.getMessage("error_economy_not_implemented_cost", "cost", String.format("%.2f", creationCost)));
            // If you wanted to actually block creation without real economy:
            // player.sendMessage(TClan.getMessage("error_insufficient_funds_player", "cost", String.format("%.2f", creationCost)));
            // return;
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

        Location playerLocation = player.getLocation();
        clan.setHomeLocation(playerLocation);
        // Future: clanManager.saveClans(); // or save specific clan
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

        // Consider adding a cooldown or cost for teleporting
        player.teleport(homeLocation);
        player.sendMessage(TClan.getMessage("teleported_to_clan_home"));
        // Consider adding safety checks for teleportation if desired
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
                .map(uuid -> {
                    OfflinePlayer member = Bukkit.getOfflinePlayer(uuid);
                    return member.getName() != null ? member.getName() : uuid.toString();
                })
                .collect(Collectors.joining(", "));
        player.sendMessage(TClan.getMessage("members_list", "members_list", membersListString));
    }

    private void handleInvestCommand(Player player, String[] args) {
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

        // TODO: Implement economy integration (e.g., Vault) to check player balance and withdraw.
        // For now, simulate player having enough money.

        clan.setBalance(clan.getBalance() + value);
        // Future: clanManager.saveClans(); // or save specific clan
        player.sendMessage(TClan.getMessage("invest_success", "amount", String.format("%.2f", value), "balance", String.format("%.2f", clan.getBalance())));
    }

    private void handleWithdrawCommand(Player player, String[] args) {
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
            player.sendMessage(TClan.getMessage("error_insufficient_funds_clan", "balance", String.format("%.2f", clan.getBalance())));
            return;
        }

        // TODO: Implement economy integration (e.g., Vault) to deposit money to player.
        // For now, simulate player receiving money.

        clan.setBalance(clan.getBalance() - value);
        // Future: clanManager.saveClans(); // or save specific clan
        player.sendMessage(TClan.getMessage("withdraw_success", "amount", String.format("%.2f", value), "balance", String.format("%.2f", clan.getBalance())));
    }

    private void handleBalanceCommand(Player player) {
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }
        player.sendMessage(TClan.getMessage("clan_balance", "balance", String.format("%.2f", clan.getBalance())));
    }

    private void handleInviteCommand(Player player, String[] args) {
        Clan inviterClan = clanManager.getClanByPlayer(player.getUniqueId());

        if (inviterClan == null) {
            player.sendMessage(TClan.getMessage("error_not_in_clan"));
            return;
        }

        if (!inviterClan.isLeader(player.getUniqueId())) {
            // For now, only leader can invite. Could be expanded with a permission system.
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

        // TODO: Implement a proper invitation system (e.g., /clan accept, /clan deny, temporary invite storage).
        // For now, directly add the player to the clan.
        inviterClan.addMember(targetPlayer.getUniqueId());
        // Future: clanManager.saveClans(); // or save specific clan

        player.sendMessage(TClan.getMessage("player_invited", "player_name", targetPlayer.getName()));
        targetPlayer.sendMessage(TClan.getMessage("you_were_invited", "clan_name", inviterClan.getName()));
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
    }
}

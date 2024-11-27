package com.gsoldera.gAuction.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.gsoldera.gAuction.GAuctionPlugin;
import com.gsoldera.gAuction.auction.AuctionItem;
import com.gsoldera.gAuction.gui.AuctionConfirmGUI;
import com.gsoldera.gAuction.messages.MessageManager;

/**
 * Handles player auction commands
 * Commands:
 * - /auction - Opens auction menu
 * - /auction create [minBid] [step] - Creates new auction
 * - /auction bid <amount> - Places bid on current auction
 * - /auction info - Shows current auction info
 */
public final class AuctionCommand implements CommandExecutor {
    private final GAuctionPlugin plugin;
    private final MessageManager messageManager;

    public AuctionCommand(GAuctionPlugin plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendRawMessage(sender, "&cThis command can only be used by players");
            return true;
        }

        if (args.length == 0) {
            plugin.getAuctionManager().openMainGUI(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "create", "criar" -> handleCreateAuction(player, args);
            case "bid", "lance" -> handlePlaceBid(player, args);
            case "info" -> handleAuctionInfo(player);
            default -> showHelp(player);
        };
    }

    /**
     * Handles auction creation command
     */
    private boolean handleCreateAuction(Player player, String[] args) {
        ItemStack itemToAuction = player.getInventory().getItemInMainHand();
        
        if (itemToAuction.getType() == Material.AIR) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.player.auction.no_item", placeholders);
            return true;
        }

        if (plugin.getConfigManager().getBannedItems().contains(itemToAuction.getType().name())) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.player.auction.banned_item", placeholders);
            return true;
        }

        double minBid = 1.0;
        double stepValue = plugin.getConfigManager().isStepEnabled() ? 
            plugin.getConfigManager().getStepPercentage() : 5.0;

        if (args.length >= 2) {
            try {
                minBid = Double.parseDouble(args[1]);
                stepValue = minBid * (stepValue / 100.0);
            } catch (NumberFormatException e) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(player, "messages.player.auction.invalid_min_bid", placeholders);
                return true;
            }
        }

        if (args.length >= 3) {
            try {
                double stepArg = Double.parseDouble(args[2]);
                if (stepArg >= minBid * 0.05 && stepArg <= minBid * 0.5) {
                    stepValue = stepArg;
                }
            } catch (NumberFormatException e) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(player, "messages.player.auction.invalid_step", placeholders);
                return true;
            }
        }

        AuctionConfirmGUI confirmGUI = new AuctionConfirmGUI(plugin, player, itemToAuction, minBid, stepValue);
        player.openInventory(confirmGUI.getInventory());
        return true;
    }

    /**
     * Handles bid placement command
     */
    private boolean handlePlaceBid(Player player, String[] args) {
        AuctionItem currentAuction = plugin.getAuctionManager().getCurrentAuction();

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.player.bids.usage", placeholders);
            return true;
        }

        if (currentAuction == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("reason", "No active auction");
            messageManager.sendMessage(player, "messages.player.bids.failed", placeholders);
            return true;
        }

        try {
            double bidAmount = Double.parseDouble(args[1]);
            if (bidAmount < currentAuction.getMinimumNextBid()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("min_bid", String.valueOf(currentAuction.getMinimumNextBid()));
                messageManager.sendMessage(player, "messages.player.bids.minimum_bid", placeholders);
                return true;
            }

            return plugin.getAuctionManager().placeBid(player, bidAmount);
        } catch (NumberFormatException e) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("reason", "Invalid bid amount");
            messageManager.sendMessage(player, "messages.player.bids.failed", placeholders);
            return true;
        }
    }

    /**
     * Handles auction info command
     */
    private boolean handleAuctionInfo(Player player) {
        AuctionItem currentAuction = plugin.getAuctionManager().getCurrentAuction();

        if (currentAuction == null) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.player.auction.info_failed", placeholders);
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", currentAuction.getItem().getType().name());
        placeholders.put("seller", currentAuction.getSellerName());
        placeholders.put("current_bid", String.valueOf(currentAuction.getCurrentBid()));
        placeholders.put("min_next_bid", String.valueOf(currentAuction.getMinimumNextBid()));
        placeholders.put("time_remaining", formatTimeRemaining(currentAuction.getRemainingTime()));
        
        messageManager.sendMessage(player, "messages.player.auction.info", placeholders);
        return true;
    }

    /**
     * Shows command help
     */
    private boolean showHelp(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        messageManager.sendMessage(player, "messages.player.auction.help", placeholders);
        return true;
    }

    /**
     * Formats remaining time in milliseconds to a readable string
     */
    private String formatTimeRemaining(long remainingMs) {
        long minutes = remainingMs / (60 * 1000);
        long seconds = (remainingMs % (60 * 1000)) / 1000;
        return String.format("%d min %d sec", minutes, seconds);
    }
}

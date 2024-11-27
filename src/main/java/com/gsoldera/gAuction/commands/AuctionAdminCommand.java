package com.gsoldera.gAuction.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.gsoldera.gAuction.GAuctionPlugin;
import com.gsoldera.gAuction.auction.AuctionItem;
import com.gsoldera.gAuction.messages.MessageManager;

/**
 * Handles admin auction commands
 * Commands:
 * - /auctionadmin banitem - Ban item in hand
 * - /auctionadmin banchest - Ban items in chest
 * - /auctionadmin setduration <seconds> - Set auction duration
 * - /auctionadmin cancelauction - Cancel current auction
 * - /auctionadmin forcestart [min bid] [step] - Force start auction
 * - /auctionadmin reload - Reload config
 */
public final class AuctionAdminCommand implements CommandExecutor {
    private final GAuctionPlugin plugin;
    private final MessageManager messageManager;
    private static final int MIN_DURATION = 30;
    private static final int MAX_DURATION = 3600;

    public AuctionAdminCommand(GAuctionPlugin plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendRawMessage(sender, "&cThis command can only be used by players");
            return true;
        }

        if (!player.hasPermission("gauction.admin")) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.error.no_permission", placeholders);
            return true;
        }

        if (args.length == 0) {
            return showHelp(player);
        }

        return switch (args[0].toLowerCase()) {
            case "banitem" -> handleBanItem(player);
            case "banchest" -> handleBanChest(player);
            case "setduration" -> handleSetDuration(player, args);
            case "cancelauction" -> handleCancelAuction(player);
            case "forcestart" -> handleForceStart(player, args);
            case "reload" -> handleReload(player);
            default -> showHelp(player);
        };
    }

    private boolean handleBanItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.ban_item.no_item", placeholders);
            return true;
        }

        String itemId = item.getType().getKey().toString();
        if (plugin.getConfigManager().getBannedItems().contains(itemId)) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.ban_item.already_banned", placeholders);
            return true;
        }

        plugin.getConfigManager().addBannedItem(itemId);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", item.getType().name());
        messageManager.sendMessage(player, "messages.admin.ban_item.success", placeholders);
        return true;
    }

    private boolean handleBanChest(Player player) {
        Block targetBlock = player.getTargetBlock(null, 5);
        if (!(targetBlock.getState() instanceof Chest chest)) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.ban_chest.no_chest", placeholders);
            return true;
        }

        int bannedCount = 0;
        ItemStack[] contents = chest.getInventory().getContents();
        if (contents != null) {
            for (ItemStack item : contents) {
                if (item != null && item.getType() != Material.AIR) {
                    String itemId = item.getType().getKey().toString();
                    if (!plugin.getConfigManager().getBannedItems().contains(itemId)) {
                        plugin.getConfigManager().addBannedItem(itemId);
                        bannedCount++;
                    }
                }
            }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(bannedCount));
        messageManager.sendMessage(player, "messages.admin.ban_chest.success", placeholders);
        return true;
    }

    private boolean handleSetDuration(Player player, String[] args) {
        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.duration.usage", placeholders);
            return true;
        }

        try {
            int duration = Integer.parseInt(args[1]);
            if (duration < MIN_DURATION || duration > MAX_DURATION) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(player, "messages.admin.duration.invalid_range", placeholders);
                return true;
            }

            plugin.getConfigManager().setAuctionDuration(duration);
            plugin.getConfigManager().saveConfig();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("duration", String.valueOf(duration));
            messageManager.sendMessage(player, "messages.admin.duration.success", placeholders);
            return true;
        } catch (NumberFormatException e) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.duration.invalid_number", placeholders);
            return true;
        }
    }

    private boolean handleCancelAuction(Player player) {
        AuctionItem currentAuction = plugin.getAuctionManager().getCurrentAuction();
        if (currentAuction == null) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.cancel.no_auction", placeholders);
            return true;
        }

        plugin.getAuctionManager().setCurrentAuction(null);
        
        Map<String, String> placeholders = new HashMap<>();
        messageManager.sendMessage(player, "messages.admin.cancel.success", placeholders);
        return true;
    }

    private boolean handleForceStart(Player player, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.force_start.no_item", placeholders);
            return true;
        }

        if (plugin.getConfigManager().getBannedItems().contains(item.getType().getKey().toString())) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.force_start.banned_item", placeholders);
            return true;
        }

        double minBid = 10.0;
        double stepValue = 0;

        if (args.length > 1) {
            try {
                minBid = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(player, "messages.admin.force_start.invalid_min_bid", placeholders);
                return true;
            }

            if (args.length > 2) {
                try {
                    stepValue = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    Map<String, String> placeholders = new HashMap<>();
                    messageManager.sendMessage(player, "messages.admin.force_start.invalid_step", placeholders);
                    return true;
                }
            }
        }

        AuctionItem auction = new AuctionItem(player, item, minBid, stepValue, 
            plugin.getConfigManager().getAuctionDuration() * 1000L);
        plugin.getAuctionManager().setCurrentAuction(auction);
        return true;
    }

    private boolean handleReload(Player player) {
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getAuctionManager().reloadFromConfig();
            
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.reload.success", placeholders);
        } catch (Exception e) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.admin.reload.error", placeholders);
            plugin.getPluginLogger().error("Error reloading config", e);
        }
        return true;
    }

    private boolean showHelp(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        messageManager.sendMessage(player, "messages.admin.help", placeholders);
        return true;
    }
}
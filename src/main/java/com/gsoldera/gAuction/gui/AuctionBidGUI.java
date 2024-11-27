package com.gsoldera.gAuction.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.gsoldera.gAuction.GAuctionPlugin;
import com.gsoldera.gAuction.auction.AuctionItem;
import com.gsoldera.gAuction.messages.MessageManager;

/**
 * GUI for placing bids on current auction
 */
public final class AuctionBidGUI implements InventoryHolder {
    private final GAuctionPlugin plugin;
    private final Player player;
    private final MessageManager messageManager;
    private final AuctionItem currentAuction;
    private Inventory inventory;
    private boolean hasPendingBid;

    private static final int CURRENT_ITEM_SLOT = 13;
    private static final int STEP_BID_SLOT = 11;
    private static final int CUSTOM_BID_SLOT = 15;

    public AuctionBidGUI(GAuctionPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.messageManager = plugin.getMessageManager();
        this.currentAuction = plugin.getAuctionManager().getCurrentAuction();
        this.hasPendingBid = false;

        Map<String, String> placeholders = new HashMap<>();
        String title = messageManager.getPlainMessage("gui.bid.title", placeholders);
        createInventory(title);
        initializeItems();
    }

    @SuppressWarnings("deprecation")
    private void createInventory(String title) {
        this.inventory = Bukkit.createInventory(this, 27, title);
    }

    private void initializeItems() {
        inventory.clear();
        
        fillBorders();

        if (currentAuction == null) {
            inventory.setItem(CURRENT_ITEM_SLOT, createNoAuctionItem());
            return;
        }

        inventory.setItem(CURRENT_ITEM_SLOT, createCurrentAuctionItem());

        if (plugin.getConfigManager().isStepEnabled()) {
            inventory.setItem(STEP_BID_SLOT, createStepBidButton());
        }
        inventory.setItem(CUSTOM_BID_SLOT, createCustomBidButton());
    }

    @SuppressWarnings("deprecation")
    private ItemStack createNoAuctionItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.bid.no_auction.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.bid.no_auction.description", placeholders));
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createCurrentAuctionItem() {
        ItemStack displayItem = currentAuction.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            
            List<String> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.getLore());
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min_bid", plugin.getEconomyManager().formatMoney(currentAuction.getMinimumNextBid()));
            placeholders.put("current_bid", plugin.getEconomyManager().formatMoney(currentAuction.getCurrentBid()));
            placeholders.put("seller", currentAuction.getSellerName());
            placeholders.put("time", formatTimeRemaining(currentAuction.getRemainingTime()));

            lore.add("");
            lore.add(messageManager.getPlainMessage("gui.main.current_auction.status", placeholders));
            lore.add(messageManager.getPlainMessage("gui.main.current_auction.min_bid", placeholders));
            lore.add(messageManager.getPlainMessage("gui.main.current_auction.current_bid", placeholders));
            lore.add(messageManager.getPlainMessage("gui.main.current_auction.seller", placeholders));
            lore.add(messageManager.getPlainMessage("gui.main.current_auction.time", placeholders));
            
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createStepBidButton() {
        ItemStack button = new ItemStack(Material.EMERALD);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", plugin.getEconomyManager().formatMoney(currentAuction.getMinimumNextBid()));
            meta.setDisplayName(messageManager.getPlainMessage("gui.bid.step_button.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.bid.step_button.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createCustomBidButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min_bid", plugin.getEconomyManager().formatMoney(currentAuction.getMinimumNextBid()));
            meta.setDisplayName(messageManager.getPlainMessage("gui.bid.custom_button.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.bid.custom_button.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    @SuppressWarnings("deprecation")
    private void fillBorders() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            border.setItemMeta(meta);
        }

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border.clone());
            inventory.setItem(18 + i, border.clone());
        }

        for (int i = 9; i < 18; i++) {
            if (i == 9 || i == 17) {
                inventory.setItem(i, border.clone());
            }
        }
    }

    private String formatTimeRemaining(long remainingMs) {
        long minutes = remainingMs / (60 * 1000);
        long seconds = (remainingMs % (60 * 1000)) / 1000;
        return String.format("%d min %d sec", minutes, seconds);
    }

    public void handleInventoryClick(Player player, int slot) {
        if (currentAuction == null) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.player.bids.failed.no_auction", placeholders);
            player.closeInventory();
            return;
        }

        if (slot == STEP_BID_SLOT) {
            handleStepBid(player);
        } else if (slot == CUSTOM_BID_SLOT) {
            handleCustomBid(player);
        }
    }

    public void open() {
        if (currentAuction == null) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.player.bids.failed.no_auction", placeholders);
            return;
        }
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public boolean hasPendingBid() {
        return hasPendingBid;
    }

    private void handleStepBid(Player player) {
        if (currentAuction == null) return;
        
        double nextBid = currentAuction.getMinimumNextBid();
        
        if (!plugin.getEconomyManager().hasBalance(player, nextBid)) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "gui.bid.messages.no_money", placeholders);
            return;
        }
        
        if (plugin.getAuctionManager().placeBid(player, nextBid)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("bid", plugin.getEconomyManager().formatMoney(nextBid));
            placeholders.put("item", currentAuction.getItem().getType().name());
            messageManager.sendMessage(player, "gui.bid.messages.success", placeholders);
            player.closeInventory();
        }
    }

    private void handleCustomBid(Player player) {
        if (currentAuction == null) return;
        
        hasPendingBid = true;
        player.closeInventory();
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("min_bid", plugin.getEconomyManager().formatMoney(currentAuction.getMinimumNextBid()));
        messageManager.sendMessage(player, "gui.bid.custom.prompt", placeholders);
        messageManager.sendMessage(player, "gui.bid.custom.min_bid", placeholders);
        
        Listener chatListener = new Listener() {
            @SuppressWarnings("deprecation")
            @EventHandler
            public void onChat(AsyncPlayerChatEvent event) {
                if (!event.getPlayer().equals(player)) return;
                event.setCancelled(true);
                
                if (event.getMessage().equalsIgnoreCase("cancel")) {
                    Map<String, String> placeholders = new HashMap<>();
                    messageManager.sendMessage(player, "gui.bid.custom.cancelled", placeholders);
                    hasPendingBid = false;
                    HandlerList.unregisterAll(this);
                    return;
                }
                
                try {
                    double bidAmount = Double.parseDouble(event.getMessage());
                    
                    if (bidAmount < currentAuction.getMinimumNextBid()) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("min_bid", plugin.getEconomyManager().formatMoney(currentAuction.getMinimumNextBid()));
                        messageManager.sendMessage(player, "gui.bid.messages.minimum_bid", placeholders);
                        return;
                    }
                    
                    if (!plugin.getEconomyManager().hasBalance(player, bidAmount)) {
                        Map<String, String> placeholders = new HashMap<>();
                        messageManager.sendMessage(player, "gui.bid.messages.no_money", placeholders);
                        return;
                    }
                    
                    if (plugin.getAuctionManager().placeBid(player, bidAmount)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("bid", plugin.getEconomyManager().formatMoney(bidAmount));
                        placeholders.put("item", currentAuction.getItem().getType().name());
                        messageManager.sendMessage(player, "gui.bid.messages.success", placeholders);
                    }
                } catch (NumberFormatException e) {
                    Map<String, String> placeholders = new HashMap<>();
                    messageManager.sendMessage(player, "gui.bid.custom.invalid_amount", placeholders);
                } finally {
                    hasPendingBid = false;
                    HandlerList.unregisterAll(this);
                }
            }
        };
        
        Bukkit.getPluginManager().registerEvents(chatListener, plugin);
    }
}
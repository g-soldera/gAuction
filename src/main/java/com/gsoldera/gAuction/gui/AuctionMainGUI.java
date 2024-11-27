package com.gsoldera.gAuction.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.gsoldera.gAuction.GAuctionPlugin;
import com.gsoldera.gAuction.auction.AuctionItem;
import com.gsoldera.gAuction.messages.MessageManager;

/**
 * Main auction GUI
 * Shows current auction and navigation buttons
 */
public final class AuctionMainGUI implements InventoryHolder {
    private final GAuctionPlugin plugin;
    private final Player player;
    private final MessageManager messageManager;
    private Inventory inventory;

    private static final int CURRENT_AUCTION_SLOT = 22;
    private static final int BID_BUTTON_SLOT = 29;
    private static final int WAREHOUSE_BUTTON_SLOT = 33;
    private static final int HISTORY_BUTTON_SLOT = 40;

    public AuctionMainGUI(GAuctionPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.messageManager = plugin.getMessageManager();
        
        Map<String, String> placeholders = new HashMap<>();
        String title = messageManager.getPlainMessage("gui.main.title", placeholders);
        createInventory(title);
        initializeItems();
    }

    @SuppressWarnings("deprecation")
    private void createInventory(String title) {
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    private void initializeItems() {
        inventory.clear();
        
        fillBorders();
        
        inventory.setItem(CURRENT_AUCTION_SLOT, createCurrentAuctionItem());
        inventory.setItem(BID_BUTTON_SLOT, createBidButton());
        inventory.setItem(WAREHOUSE_BUTTON_SLOT, createWarehouseButton());
        inventory.setItem(HISTORY_BUTTON_SLOT, createHistoryButton());
    }

    @SuppressWarnings("deprecation")
    private ItemStack createCurrentAuctionItem() {
        AuctionItem currentAuction = plugin.getAuctionManager().getCurrentAuction();

        if (currentAuction == null) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                Map<String, String> placeholders = new HashMap<>();
                meta.setDisplayName(messageManager.getPlainMessage("gui.main.no_auction.title", placeholders));
                
                List<String> lore = new ArrayList<>();
                lore.add(messageManager.getPlainMessage("gui.main.no_auction.description", placeholders));
                meta.setLore(lore);
                
                item.setItemMeta(meta);
            }
            return item;
        }

        ItemStack displayItem = currentAuction.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min_bid", plugin.getEconomyManager().formatMoney(currentAuction.getMinBid()));
            placeholders.put("current_bid", plugin.getEconomyManager().formatMoney(currentAuction.getCurrentBid()));
            placeholders.put("seller", currentAuction.getSellerName());
            placeholders.put("time", formatTimeRemaining(currentAuction.getRemainingTime()));

            List<String> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.getLore());
            }
            
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
    private ItemStack createBidButton() {
        ItemStack button = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.main.bid_button.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.main.bid_button.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createWarehouseButton() {
        ItemStack button = new ItemStack(Material.CHEST);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.main.warehouse_button.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.main.warehouse_button.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createHistoryButton() {
        ItemStack button = new ItemStack(Material.BOOK);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.main.history_button.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.main.history_button.description", placeholders));
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

        for (int i = 0; i < inventory.getSize(); i++) {
            if (isSlotOnBorder(i)) {
                inventory.setItem(i, border.clone());
            }
        }
    }

    private boolean isSlotOnBorder(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }

    private String formatTimeRemaining(long remainingMs) {
        long minutes = remainingMs / (60 * 1000);
        long seconds = (remainingMs % (60 * 1000)) / 1000;
        return String.format("%d min %d sec", minutes, seconds);
    }

    public void handleInventoryClick(Player player, int slot) {
        switch (slot) {
            case BID_BUTTON_SLOT -> {
                player.closeInventory();
                new AuctionBidGUI(plugin, player).open();
            }
            case WAREHOUSE_BUTTON_SLOT -> {
                player.closeInventory();
                new AuctionWarehouseGUI(plugin, player).open();
            }
            case HISTORY_BUTTON_SLOT -> {
                player.closeInventory();
                new AuctionHistoryGUI(plugin, player).open();
            }
        }
    }

    public void refresh() {
        initializeItems();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

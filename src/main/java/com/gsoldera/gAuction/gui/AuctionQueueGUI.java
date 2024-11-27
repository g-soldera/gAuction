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

public final class AuctionQueueGUI implements InventoryHolder {
    private final GAuctionPlugin plugin;
    private final Player player;
    private final MessageManager messageManager;
    private Inventory inventory;
    private static final int ROWS = 3;

    public AuctionQueueGUI(GAuctionPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.messageManager = plugin.getMessageManager();
        
        Map<String, String> placeholders = new HashMap<>();
        String title = messageManager.getPlainMessage("gui.queue.title", placeholders);
        createInventory(title);
        initializeItems();
    }

    @SuppressWarnings("deprecation")
    private void createInventory(String title) {
        this.inventory = Bukkit.createInventory(this, ROWS * 9, title);
    }

    private void initializeItems() {
        inventory.clear();
        fillBorders();
        
        List<AuctionItem> queueItems = plugin.getAuctionManager().getQueuePreview(7);
        int slot = 10;
        
        for (AuctionItem auction : queueItems) {
            inventory.setItem(slot++, createQueueItemDisplay(auction));
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createQueueItemDisplay(AuctionItem auction) {
        ItemStack displayItem = auction.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            Map<String, String> placeholders = new HashMap<>();
            
            placeholders.put("seller", auction.getSellerName());
            placeholders.put("min_bid", plugin.getEconomyManager().formatMoney(auction.getMinBid()));
            placeholders.put("step", plugin.getEconomyManager().formatMoney(auction.getStepValue()));
            
            lore.add(messageManager.getPlainMessage("gui.queue.item.seller", placeholders));
            lore.add(messageManager.getPlainMessage("gui.queue.item.min_bid", placeholders));
            lore.add(messageManager.getPlainMessage("gui.queue.item.step", placeholders));
            
            if (player.hasPermission("gauction.admin")) {
                lore.add("");
                lore.add(messageManager.getPlainMessage("gui.queue.item.admin_remove", placeholders));
            }
            
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    public void handleInventoryClick(Player player, int slot, boolean isRightClick) {
        if (!player.hasPermission("gauction.admin") || !isRightClick) return;
        
        AuctionItem clickedAuction = plugin.getAuctionManager().getQueueItemAt(slot - 10);
        if (clickedAuction != null) {
            plugin.getAuctionManager().removeFromQueue(clickedAuction);
            refresh();
        }
    }

    public void refresh() {
        initializeItems();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
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
        inventory.setItem(9, border.clone());
        inventory.setItem(17, border.clone());
    }

    public void open() {
        player.openInventory(inventory);
    }
} 
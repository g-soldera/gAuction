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
    private static final int CLEAR_QUEUE_BUTTON_SLOT = 4;
    private static final int BACK_BUTTON_SLOT = 22;

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

        inventory.setItem(BACK_BUTTON_SLOT, createBackButton());
        
        if (player.hasPermission("gauction.admin")) {
            inventory.setItem(CLEAR_QUEUE_BUTTON_SLOT, createClearQueueButton());
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

    @SuppressWarnings("deprecation")
    private ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.queue.buttons.back.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.queue.buttons.back.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createClearQueueButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.queue.buttons.clear.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.queue.buttons.clear.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    public void handleInventoryClick(Player player, int slot, boolean isRightClick) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (slot == BACK_BUTTON_SLOT) {
            player.closeInventory();
            new AuctionMainGUI(plugin, player).open();
            return;
        }

        if (slot == CLEAR_QUEUE_BUTTON_SLOT && player.hasPermission("gauction.admin")) {
            clearQueue();
            return;
        }

        if (isValidQueueSlot(slot) && player.hasPermission("gauction.admin") && isRightClick) {
            int index = getQueueIndex(slot);
            if (index == 0) {
                plugin.getAuctionManager().cancelCurrentAuction();
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(player, "gui.queue.messages.current_cancelled", placeholders);
            } else {
                AuctionItem clickedAuction = plugin.getAuctionManager().getQueueItemAt(index);
                if (clickedAuction != null) {
                    plugin.getAuctionManager().cancelQueuedAuction(clickedAuction);
                    Map<String, String> placeholders = new HashMap<>();
                    messageManager.sendMessage(player, "gui.queue.messages.removed", placeholders);
                }
            }
            refresh();
        }
    }

    private boolean isValidQueueSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        
        return row >= 1 && row <= 3 && col >= 1 && col <= 9;
    }

    private int getQueueIndex(int slot) {
        int row = slot / 9 - 1;
        int col = slot % 9 - 1;
        return row * 7 + col;
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

    private void clearQueue() {
        plugin.getAuctionManager().clearAllAuctions();
        Map<String, String> placeholders = new HashMap<>();
        messageManager.sendMessage(player, "gui.queue.messages.queue_cleared", placeholders);
        refresh();
    }
} 
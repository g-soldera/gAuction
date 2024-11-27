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
import com.gsoldera.gAuction.messages.MessageManager;

/**
 * GUI for confirming auction creation
 * Shows item to be auctioned and confirm/cancel buttons
 */
public final class AuctionConfirmGUI implements InventoryHolder {
    private final GAuctionPlugin plugin;
    private final MessageManager messageManager;
    private final ItemStack itemToAuction;
    private final double minBid;
    private final double stepValue;
    private Inventory inventory;
    private boolean confirmed;

    private static final int CONFIRM_START_SLOT = 10;
    private static final int CONFIRM_END_SLOT = 12;
    private static final int ITEM_SLOT = 13;
    private static final int CANCEL_START_SLOT = 14;
    private static final int CANCEL_END_SLOT = 16;

    public AuctionConfirmGUI(GAuctionPlugin plugin, Player player, ItemStack itemToAuction, 
                            double minBid, double stepValue) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.itemToAuction = itemToAuction;
        this.minBid = minBid;
        this.stepValue = stepValue;
        this.confirmed = false;

        Map<String, String> placeholders = new HashMap<>();
        String title = messageManager.getPlainMessage("gui.confirm.title", placeholders);
        createInventory(title);
        initializeItems();
    }

    @SuppressWarnings("deprecation")
    private void createInventory(String title) {
        this.inventory = Bukkit.createInventory(this, 27, title);
    }

    private void initializeItems() {
        // Limpa o inventári
        inventory.clear();
        
        fillBorders();

        ItemStack confirmButton = createConfirmButton();
        for (int i = CONFIRM_START_SLOT; i <= CONFIRM_END_SLOT; i++) {
            inventory.setItem(i, confirmButton);
        }

        ItemStack cancelButton = createCancelButton();
        for (int i = CANCEL_START_SLOT; i <= CANCEL_END_SLOT; i++) {
            inventory.setItem(i, cancelButton);
        }

        inventory.setItem(ITEM_SLOT, itemToAuction.clone());
    }

    @SuppressWarnings("deprecation")
    private ItemStack createConfirmButton() {
        ItemStack button = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min_bid", plugin.getEconomyManager().formatMoney(minBid));
            placeholders.put("step", plugin.getEconomyManager().formatMoney(stepValue));
            
            meta.setDisplayName(messageManager.getPlainMessage("gui.confirm.confirm_button.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.confirm.confirm_button.description", placeholders));
            lore.add("");
            lore.add(messageManager.getPlainMessage("gui.confirm.confirm_button.min_bid", placeholders));
            lore.add(messageManager.getPlainMessage("gui.confirm.confirm_button.step", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createCancelButton() {
        ItemStack button = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.confirm.cancel_button.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.confirm.cancel_button.description", placeholders));
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

    public void handleInventoryClick(Player player, int slot) {
        if (slot >= CONFIRM_START_SLOT && slot <= CONFIRM_END_SLOT) {
            confirmed = true;
            player.closeInventory();
            
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.isSimilar(itemToAuction)) {
                ItemStack auctionItem = itemToAuction.clone();
                
                itemInHand.setAmount(itemInHand.getAmount() - itemToAuction.getAmount());
                player.getInventory().setItemInMainHand(itemInHand.getAmount() > 0 ? itemInHand : null);
                
                plugin.getAuctionManager().queueAuction(player, auctionItem, minBid, stepValue);
            }
        } 
        else if (slot >= CANCEL_START_SLOT && slot <= CANCEL_END_SLOT) {
            player.closeInventory();
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "gui.confirm.cancelled", placeholders);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ItemStack getAuctionItem() {
        return itemToAuction;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
} 
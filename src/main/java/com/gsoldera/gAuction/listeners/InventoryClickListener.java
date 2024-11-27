package com.gsoldera.gAuction.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.gsoldera.gAuction.gui.AuctionBidGUI;
import com.gsoldera.gAuction.gui.AuctionConfirmGUI;
import com.gsoldera.gAuction.gui.AuctionHistoryGUI;
import com.gsoldera.gAuction.gui.AuctionMainGUI;
import com.gsoldera.gAuction.gui.AuctionQueueGUI;
import com.gsoldera.gAuction.gui.AuctionWarehouseGUI;
/**
 * Handles inventory click events for auction GUIs
 * Delegates click handling to appropriate GUI classes
 */
public final class InventoryClickListener implements Listener {
    
    /**
     * Handles inventory click events
     * Cancels event and delegates to appropriate GUI handler
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        // Cancel event and delegate to appropriate GUI handler
        if (holder instanceof AuctionConfirmGUI gui) {
            event.setCancelled(true);
            gui.handleInventoryClick(player, event.getRawSlot());
        } 
        else if (holder instanceof AuctionHistoryGUI gui) {
            event.setCancelled(true);
            gui.handleInventoryClick(player, event.getRawSlot());
        } 
        else if (holder instanceof AuctionMainGUI gui) {
            event.setCancelled(true);
            gui.handleInventoryClick(player, event.getRawSlot());
        } 
        else if (holder instanceof AuctionBidGUI gui) {
            event.setCancelled(true);
            gui.handleInventoryClick(player, event.getRawSlot());
        } 
        else if (holder instanceof AuctionWarehouseGUI gui) {
            event.setCancelled(true);
            gui.handleInventoryClick(player, event.getRawSlot());
        }
        else if (holder instanceof AuctionQueueGUI gui) {
            event.setCancelled(true);
            gui.handleInventoryClick(player, event.getRawSlot(), event.isRightClick());
        }
    }
} 
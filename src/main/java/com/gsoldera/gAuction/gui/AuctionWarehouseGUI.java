package com.gsoldera.gAuction.gui;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import com.gsoldera.gAuction.utils.ItemSerializer;

/**
 * GUI for managing items in player's warehouse
 * Shows unclaimed items from expired or successful auctions
 */
public final class AuctionWarehouseGUI implements InventoryHolder {
    private final GAuctionPlugin plugin;
    private final Player player;
    private final MessageManager messageManager;
    private Inventory inventory;
    private final List<WarehouseItem> warehouseItems;
    private int currentPage;

    private static final int ROWS = 6;
    private static final int PAGE_SIZE = 28;
    private static final int FIRST_SLOT = 10;
    private static final int LAST_SLOT = 43;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int COLLECT_ALL_SLOT = 49;
    private static final int BACK_BUTTON_SLOT = 45;

    private record WarehouseItem(
        long id,
        ItemStack item,
        String sellerName,
        String buyerName,
        double finalBid,
        long endTime,
        String status
    ) {}

    public AuctionWarehouseGUI(GAuctionPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.messageManager = plugin.getMessageManager();
        this.warehouseItems = new ArrayList<>();
        this.currentPage = 0;

        Map<String, String> placeholders = new HashMap<>();
        String title = messageManager.getPlainMessage("gui.warehouse.title", placeholders);
        createInventory(title);
        loadWarehouseItems();
        initializeItems();
    }

    @SuppressWarnings("deprecation")
    private void createInventory(String title) {
        this.inventory = Bukkit.createInventory(this, ROWS * 9, title);
    }

    private void loadWarehouseItems() {
        try (var conn = plugin.getDatabaseManager().getDatabaseConnection().getConnection();
             var stmt = conn.prepareStatement(
                "SELECT id, item_serialized, seller_name, buyer_name, final_bid, end_time, status " +
                "FROM auction_history " +
                "WHERE (seller_uuid = ? AND status in ('EXPIRED', 'CANCELLED')) " +
                "OR (buyer_uuid = ? AND status = 'SOLD') " +
                "ORDER BY end_time DESC"
             )) {
            
            String playerUUID = player.getUniqueId().toString();
            stmt.setString(1, playerUUID);
            stmt.setString(2, playerUUID);

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ItemStack item = ItemSerializer.deserializeItemStack(rs.getString("item_serialized"));
                    if (item != null) {
                        warehouseItems.add(new WarehouseItem(
                            rs.getLong("id"),
                            item,
                            rs.getString("seller_name"),
                            rs.getString("buyer_name"),
                            rs.getDouble("final_bid"),
                            rs.getLong("end_time"),
                            rs.getString("status")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().error("Error loading warehouse items", e);
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "messages.player.error.warehouse_load_failed", placeholders);
        }
    }

    private void initializeItems() {
        clearInventory();
        fillBorders();
        
        inventory.setItem(BACK_BUTTON_SLOT, createBackButton());

        if (warehouseItems.isEmpty()) {
            inventory.setItem(22, createEmptyWarehouseItem());
        } else {
            displayCurrentPage();
            updateNavigationButtons();
            inventory.setItem(COLLECT_ALL_SLOT, createCollectAllButton());
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createCollectAllButton() {
        ItemStack button = new ItemStack(Material.HOPPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.warehouse.buttons.collect_all.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.warehouse.buttons.collect_all.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createEmptyWarehouseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.warehouse.empty.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.warehouse.empty.description", placeholders));
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.warehouse.buttons.back.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.warehouse.buttons.back.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    private void clearInventory() {
        inventory.clear();
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void displayCurrentPage() {
        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, warehouseItems.size());
        int slot = FIRST_SLOT;

        for (int i = startIndex; i < endIndex; i++) {
            if (slot > LAST_SLOT) break;
            if (slot % 9 == 8) {
                slot += 2;
                continue;
            }
            
            WarehouseItem entry = warehouseItems.get(i);
            inventory.setItem(slot++, createWarehouseItem(entry));
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createWarehouseItem(WarehouseItem entry) {
        ItemStack displayItem = entry.item().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            
            List<String> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.getLore());
            }
            
            lore.add("");
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("seller", entry.sellerName());
            placeholders.put("buyer", entry.buyerName() != null ? entry.buyerName() : "Nenhum");
            placeholders.put("final_bid", plugin.getEconomyManager().formatMoney(entry.finalBid()));
            placeholders.put("date", formatTime(entry.endTime()));
            placeholders.put("status_color", getStatusColor(entry.status()));
            placeholders.put("status", entry.status());

            lore.add(messageManager.getPlainMessage("gui.warehouse.item.seller", placeholders));
            lore.add(messageManager.getPlainMessage("gui.warehouse.item.buyer", placeholders));
            lore.add(messageManager.getPlainMessage("gui.warehouse.item.final_bid", placeholders));
            lore.add(messageManager.getPlainMessage("gui.warehouse.item.date", placeholders));
            lore.add(messageManager.getPlainMessage("gui.warehouse.item.status", placeholders));
            lore.add("");
            lore.add(messageManager.getPlainMessage("gui.warehouse.item.collect", placeholders));

            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "SOLD" -> "§a";
            case "EXPIRED" -> "§c";
            case "CANCELLED" -> "§4";
            case "COLLECTED" -> "§b";
            default -> "§7";
        };
    }

    private String formatTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public void handleInventoryClick(Player player, int slot) {
        if (warehouseItems.isEmpty()) {
            if (slot == BACK_BUTTON_SLOT) {
                player.closeInventory();
                new AuctionMainGUI(plugin, player).open();
            }
            return;
        }

        if (slot == COLLECT_ALL_SLOT) {
            collectAllItems();
        } 
        else if (slot == BACK_BUTTON_SLOT) {
            player.closeInventory();
            new AuctionMainGUI(plugin, player).open();
        }
        else if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            refreshInventory();
        }
        else if (slot == NEXT_PAGE_SLOT) {
            int maxPages = (int) Math.ceil(warehouseItems.size() / (double) PAGE_SIZE);
            if (currentPage < maxPages - 1) {
                currentPage++;
                refreshInventory();
            }
        }
        else if (isItemSlot(slot)) {
            collectItem(getItemIndexFromSlot(slot));
        }
    }

    private boolean isItemSlot(int slot) {
        if (slot < FIRST_SLOT || slot > LAST_SLOT) return false;
        int col = slot % 9;
        return col != 0 && col != 8;
    }

    private int getItemIndexFromSlot(int slot) {
        int row = (slot - FIRST_SLOT) / 9;
        int col = (slot % 9) - 1;
        return currentPage * PAGE_SIZE + row * 7 + col;
    }

    private void collectItem(int index) {
        if (index < 0 || index >= warehouseItems.size()) return;

        WarehouseItem item = warehouseItems.get(index);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.item().clone());
        
        if (leftover.isEmpty()) {
            markItemAsCollected(item.id());
            warehouseItems.remove(index);
            
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "gui.warehouse.messages.collected", placeholders);
            
            refreshInventory();
        } else {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "gui.warehouse.messages.inventory_full", placeholders);
        }
    }

    private void collectAllItems() {
        boolean anyCollected = false;
        List<WarehouseItem> remaining = new ArrayList<>();

        for (WarehouseItem item : warehouseItems) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.item().clone());
            if (leftover.isEmpty()) {
                markItemAsCollected(item.id());
                anyCollected = true;
            } else {
                remaining.add(item);
            }
        }

        warehouseItems.clear();
        warehouseItems.addAll(remaining);

        Map<String, String> placeholders = new HashMap<>();
        if (anyCollected) {
            placeholders.put("count", String.valueOf(remaining.size()));
            messageManager.sendMessage(player, "gui.warehouse.messages.items_collected", placeholders);
        } else {
            messageManager.sendMessage(player, "gui.warehouse.messages.inventory_full", placeholders);
        }

        refreshInventory();
    }

    private void markItemAsCollected(long itemId) {
        try (var conn = plugin.getDatabaseManager().getDatabaseConnection().getConnection();
             var stmt = conn.prepareStatement(
                "UPDATE auction_history SET status = 'COLLECTED' WHERE id = ?"
             )) {
            stmt.setLong(1, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getPluginLogger().error("Error marking item as collected", e);
        }
    }

    private void updateNavigationButtons() {
        if (currentPage > 0) {
            inventory.setItem(PREV_PAGE_SLOT, createNavigationButton(true));
        }

        int maxPages = (int) Math.ceil(warehouseItems.size() / (double) PAGE_SIZE);
        if (currentPage < maxPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavigationButton(false));
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createNavigationButton(boolean isPrevious) {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            String key = isPrevious ? "gui.warehouse.prev_page" : "gui.warehouse.next_page";
            meta.setDisplayName(messageManager.getPlainMessage(key, placeholders));
            button.setItemMeta(meta);
        }
        return button;
    }

    private void refreshInventory() {
        clearInventory();
        fillBorders();
        inventory.setItem(BACK_BUTTON_SLOT, createBackButton());
        
        if (warehouseItems.isEmpty()) {
            inventory.setItem(22, createEmptyWarehouseItem());
        } else {
            displayCurrentPage();
            updateNavigationButtons();
            inventory.setItem(COLLECT_ALL_SLOT, createCollectAllButton());
        }
    }
}
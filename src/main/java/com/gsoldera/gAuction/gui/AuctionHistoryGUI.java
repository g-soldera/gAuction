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
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.gsoldera.gAuction.GAuctionPlugin;
import com.gsoldera.gAuction.messages.MessageManager;
import com.gsoldera.gAuction.utils.ItemSerializer;

/**
 * GUI for viewing auction history
 * Shows past auctions and their outcomes
 */
public final class AuctionHistoryGUI implements InventoryHolder {
    private final GAuctionPlugin plugin;
    private final Player player;
    private final MessageManager messageManager;
    private Inventory inventory;
    private int currentPage;
    private boolean showingPersonalOnly = false;
    private List<HistoryEntry> allHistoryEntries;
    private List<HistoryEntry> filteredHistoryEntries;

    private static final int ROWS = 6;
    private static final int PAGE_SIZE = 28;
    private static final int FIRST_SLOT = 10;
    private static final int LAST_SLOT = 43;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int BACK_BUTTON_SLOT = 49;
    private static final int FILTER_BUTTON_SLOT = 51;

    private record HistoryEntry(
        ItemStack item,
        String sellerName,
        String buyerName,
        double finalBid,
        long endTime,
        String status
    ) {}

    public AuctionHistoryGUI(GAuctionPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.messageManager = plugin.getMessageManager();
        this.currentPage = 0;

        Map<String, String> placeholders = new HashMap<>();
        String title = messageManager.getPlainMessage("gui.history.title", placeholders);
        createInventory(title);
        loadHistoryEntries();
        initializeItems();
    }

    @SuppressWarnings("deprecation")
    private void createInventory(String title) {
        this.inventory = Bukkit.createInventory(this, ROWS * 9, title);
    }

    private void loadHistoryEntries() {
        allHistoryEntries = new ArrayList<>();
        try (var conn = plugin.getDatabaseManager().getDatabaseConnection().getConnection();
             var stmt = conn.prepareStatement(
                "SELECT item_serialized, seller_name, buyer_name, final_bid, end_time, status " +
                "FROM auction_history " +
                "ORDER BY end_time DESC"
             )) {
            
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ItemStack item = ItemSerializer.deserializeItemStack(rs.getString("item_serialized"));
                    if (item != null) {
                        allHistoryEntries.add(new HistoryEntry(
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
            updateFilteredEntries();
        } catch (SQLException e) {
            plugin.getPluginLogger().error("Error loading auction history", e);
        }
    }

    private void updateFilteredEntries() {
        if (showingPersonalOnly) {
            filteredHistoryEntries = allHistoryEntries.stream()
                .filter(entry -> player.getName().equals(entry.sellerName()) || 
                               player.getName().equals(entry.buyerName()))
                .collect(Collectors.toList());
        } else {
            filteredHistoryEntries = new ArrayList<>(allHistoryEntries);
        }
        currentPage = 0;
    }

    private void initializeItems() {
        clearInventory();
        fillBorders();
        displayCurrentPage();
        updateNavigationButtons();
        inventory.setItem(BACK_BUTTON_SLOT, createBackButton());
        inventory.setItem(FILTER_BUTTON_SLOT, createFilterButton());
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

    private void displayCurrentPage() {
        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredHistoryEntries.size());
        int slot = FIRST_SLOT;
        int itemsInCurrentRow = 0;

        for (int i = startIndex; i < endIndex; i++) {
            if (slot > LAST_SLOT) break;
            if (itemsInCurrentRow == 7) {
                slot += 2;
                itemsInCurrentRow = 0;
            }
            
            HistoryEntry entry = filteredHistoryEntries.get(i);
            inventory.setItem(slot++, createHistoryItem(entry));
            itemsInCurrentRow++;
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createHistoryItem(HistoryEntry entry) {
        ItemStack displayItem = entry.item().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            Map<String, String> placeholders = new HashMap<>();
            
            placeholders.put("seller", entry.sellerName() != null ? entry.sellerName() : "Desconhecido");
            placeholders.put("buyer", entry.buyerName() != null ? entry.buyerName() : "Nenhum");
            placeholders.put("final_bid", plugin.getEconomyManager().formatMoney(entry.finalBid()));
            placeholders.put("date", formatTime(entry.endTime()));
            placeholders.put("status", entry.status() != null ? entry.status() : "DESCONHECIDO");
            placeholders.put("status_color", getStatusColor(entry.status()));

            lore.add(messageManager.getPlainMessage("gui.history.item.seller", placeholders));
            lore.add(messageManager.getPlainMessage("gui.history.item.buyer", placeholders));
            lore.add(messageManager.getPlainMessage("gui.history.item.final_bid", placeholders));
            lore.add(messageManager.getPlainMessage("gui.history.item.date", placeholders));
            lore.add(messageManager.getPlainMessage("gui.history.item.status", placeholders));

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

    private void updateNavigationButtons() {
        inventory.setItem(BACK_BUTTON_SLOT, createBackButton());

        if (currentPage > 0) {
            inventory.setItem(PREV_PAGE_SLOT, createNavigationButton(true));
        }

        int maxPages = (int) Math.ceil(filteredHistoryEntries.size() / (double) PAGE_SIZE);
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
            String key = isPrevious ? "gui.history.buttons.previous_page" : "gui.history.buttons.next_page";
            meta.setDisplayName(messageManager.getPlainMessage(key, placeholders));
            button.setItemMeta(meta);
        }
        return button;
    }

    private String formatTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public void handleInventoryClick(Player player, int slot) {
        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            refreshInventory();
        } 
        else if (slot == NEXT_PAGE_SLOT) {
            int maxPages = (int) Math.ceil(filteredHistoryEntries.size() / (double) PAGE_SIZE);
            if (currentPage < maxPages - 1) {
                currentPage++;
                refreshInventory();
            }
        }
        else if (slot == BACK_BUTTON_SLOT) {
            player.closeInventory();
            new AuctionMainGUI(plugin, player).open();
        }
        else if (slot == FILTER_BUTTON_SLOT) {
            showingPersonalOnly = !showingPersonalOnly;
            updateFilteredEntries();
            refreshInventory();
        }
    }

    private void refreshInventory() {
        clearInventory();
        fillBorders();
        displayCurrentPage();
        updateNavigationButtons();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        if (filteredHistoryEntries.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "gui.history.empty.description", placeholders);
            return;
        }
        player.openInventory(inventory);
    }

    @SuppressWarnings("deprecation")
    private ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            meta.setDisplayName(messageManager.getPlainMessage("gui.history.buttons.back.title", placeholders));
            
            List<String> lore = new ArrayList<>();
            lore.add(messageManager.getPlainMessage("gui.history.buttons.back.description", placeholders));
            meta.setLore(lore);
            
            button.setItemMeta(meta);
        }
        return button;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createFilterButton() {
        ItemStack button;
        if (showingPersonalOnly) {
            button = new ItemStack(Material.PLAYER_HEAD);
            if (button.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(player);
                
                Map<String, String> placeholders = new HashMap<>();
                meta.setDisplayName(messageManager.getPlainMessage("gui.history.buttons.filter.personal.title", placeholders));
                
                List<String> lore = new ArrayList<>();
                lore.add(messageManager.getPlainMessage("gui.history.buttons.filter.personal.description", placeholders));
                meta.setLore(lore);
                
                button.setItemMeta(meta);
            }
        } else {
            button = new ItemStack(Material.PLAYER_HEAD);
            if (button.getItemMeta() instanceof SkullMeta meta) {
                Map<String, String> placeholders = new HashMap<>();
                meta.setDisplayName(messageManager.getPlainMessage("gui.history.buttons.filter.all.title", placeholders));
                
                List<String> lore = new ArrayList<>();
                lore.add(messageManager.getPlainMessage("gui.history.buttons.filter.all.description", placeholders));
                meta.setLore(lore);
                
                button.setItemMeta(meta);
            }
        }
        return button;
    }
}
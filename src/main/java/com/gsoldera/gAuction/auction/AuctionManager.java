package com.gsoldera.gAuction.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;

import com.gsoldera.gAuction.GAuctionPlugin;
import com.gsoldera.gAuction.config.ConfigManager;
import com.gsoldera.gAuction.database.DatabaseManager;
import com.gsoldera.gAuction.economy.EconomyManager;
import com.gsoldera.gAuction.gui.AuctionMainGUI;
import com.gsoldera.gAuction.messages.MessageManager;
import com.gsoldera.gAuction.utils.ItemSerializer;

/**
 * Manages auction operations and lifecycle
 * Handles auction creation, bidding, and completion
 */
public final class AuctionManager {
    private final GAuctionPlugin plugin;
    private final Logger logger;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private final MessageManager messageManager;
    
    // Auction state
    private AuctionItem currentAuction;
    private final Queue<AuctionItem> auctionQueue;
    private boolean isProcessingAuction;
    
    // Concurrency control
    private final ReentrantLock auctionLock;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> auctionTimer;
    private ScheduledFuture<?> countdownTimer;
    private ScheduledFuture<?> autoCheckTimer;
    
    // Auction settings
    private final int maxQueueSize;
    private final double publicationFee;
    private final double bidFee;

    public AuctionManager(GAuctionPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPluginLogger();
        this.configManager = plugin.getConfigManager();
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
        this.messageManager = plugin.getMessageManager();
        
        // Initialize state
        this.auctionQueue = new ConcurrentLinkedQueue<>();
        this.isProcessingAuction = false;
        
        // Initialize concurrency controls
        this.auctionLock = new ReentrantLock();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "GAuction-Scheduler")
        );
        
        // Load settings
        this.maxQueueSize = configManager.getMaxQueueSize();
        this.publicationFee = configManager.getPublicationFee();
        this.bidFee = configManager.getBidFee();

        // Add auto-check every minute
        autoCheckTimer = scheduler.scheduleAtFixedRate(
            () -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!auctionLock.tryLock()) return;
                try {
                    if (!isProcessingAuction && (currentAuction == null || currentAuction.hasExpired())) {
                        scheduleNextAuction();
                    }
                } finally {
                    auctionLock.unlock();
                }
            }),
            60, 60, TimeUnit.SECONDS
        );
    }

    private void broadcastTimeCheckpoint(TimeCheckpoint checkpoint) {
        if (currentAuction == null) return;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", currentAuction.getItem().getType().name());
        placeholders.put("bid", String.valueOf(currentAuction.getCurrentBid()));
        
        String messageKey = switch (checkpoint) {
            case HALF_TIME -> "messages.broadcasts.auction.half_time";
            case QUARTER_TIME -> "messages.broadcasts.auction.quarter_time";
            case TENTH_TIME -> "messages.broadcasts.auction.tenth_time";
            case TEN_SECONDS -> "messages.broadcasts.auction.countdown.ten_seconds";
            case THREE_SECONDS -> "messages.broadcasts.auction.countdown.three";
            case TWO_SECONDS -> "messages.broadcasts.auction.countdown.two";
            case ONE_SECOND -> "messages.broadcasts.auction.countdown.one";
        };

        messageManager.broadcast(messageKey, placeholders);
    }

    /**
     * Schedules the next auction in queue
     */
    private void scheduleNextAuction() {
        try {
            auctionLock.lock();
            isProcessingAuction = true;

            if (currentAuction != null) {
                finalizeCurrentAuction(false);
            }

            currentAuction = auctionQueue.poll();

            if (currentAuction != null) {
                currentAuction.start();
                scheduleAuctionTimers(currentAuction);
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("seller", currentAuction.getSellerName());
                placeholders.put("item", currentAuction.getItem().getType().name());
                messageManager.broadcast("messages.broadcasts.auction.start", placeholders);
                
                refreshAllGUIs();
            }
        } finally {
            isProcessingAuction = false;
            auctionLock.unlock();
        }
    }

    /**
     * Finalizes the current auction
     */
    private void finalizeCurrentAuction(boolean canceled) {
        try {
            auctionLock.lock();

            if (currentAuction == null) return;

            cancelAllTimers();

            if (!canceled) {
                if (currentAuction.getCurrentBidderUUID() != null) {
                    handleAuctionWinner();
                } else {
                    saveToWarehouse(currentAuction, AuctionStatus.EXPIRED);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("item", currentAuction.getItem().getType().name());
                    messageManager.broadcast("messages.broadcasts.auction.expired", placeholders);
                }
            } else {
                saveToWarehouse(currentAuction, AuctionStatus.CANCELLED);
            }

            if (currentAuction.getCurrentBidderUUID() != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("item", currentAuction.getItem().getType().name());
                placeholders.put("winner", currentAuction.getCurrentBidderName());
                placeholders.put("bid", String.valueOf(currentAuction.getCurrentBid()));
                messageManager.broadcast("messages.broadcasts.auction.countdown.end", placeholders);
            }

            currentAuction = null;
            refreshAllGUIs();
            
            if (!auctionQueue.isEmpty()) {
                scheduleNextAuction();
            }
        } finally {
            auctionLock.unlock();
        }
    }

    /**
     * Handles auction winner processing
     */
    private void handleAuctionWinner() {
        if (currentAuction == null) return;

        UUID winnerUUID = currentAuction.getCurrentBidderUUID();
        if (winnerUUID == null) return;

        Player winner = Bukkit.getPlayer(winnerUUID);
        if (winner == null) {
            saveToWarehouse(currentAuction, AuctionStatus.SOLD);
            return;
        }

        saveToWarehouse(currentAuction, AuctionStatus.SOLD);

        Player seller = Bukkit.getPlayer(currentAuction.getSellerUUID());
        if (seller != null) {
            double finalAmount = calculateSellerAmount(currentAuction.getCurrentBid());
            economyManager.depositPlayer(seller, finalAmount);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("winner", winner.getName());
            placeholders.put("bid", economyManager.formatMoney(finalAmount));
            messageManager.sendMessage(seller, "messages.player.auction.sold", placeholders);
        }
    }

    /**
     * Calculates final amount seller receives after fees
     */
    private double calculateSellerAmount(double bidAmount) {
        if (bidFee <= 0) return bidAmount;
        
        double feeAmount = bidAmount * (bidFee / 100.0);
        return bidAmount - feeAmount;
    }

    /**
     * Saves auction item to warehouse with specific status
     */
    private void saveToWarehouse(AuctionItem auction, AuctionStatus status) {
        try (Connection conn = databaseManager.getDatabaseConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO auction_history (item_serialized, seller_uuid, seller_name, " +
                "buyer_uuid, buyer_name, start_time, end_time, min_bid, final_bid, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
             )) {
            
            stmt.setString(1, ItemSerializer.serializeItemStack(auction.getItem()));
            stmt.setString(2, auction.getSellerUUID().toString());
            stmt.setString(3, auction.getSellerName());
            stmt.setString(4, auction.getCurrentBidderUUID() != null ? auction.getCurrentBidderUUID().toString() : null);
            stmt.setString(5, auction.getCurrentBidderName());
            stmt.setLong(6, auction.getStartTime());
            stmt.setLong(7, System.currentTimeMillis());
            stmt.setDouble(8, auction.getMinBid());
            stmt.setDouble(9, auction.getCurrentBid());
            stmt.setString(10, status.name());
            
            stmt.executeUpdate();
            logger.info("Saved auction to warehouse: {} with status {}", 
                auction.getItem().getType().name(), status);
        } catch (SQLException e) {
            logger.error("Error saving auction to warehouse", e);
        }
    }

    /**
     * Checks if a player has any items in warehouse
     */
    public boolean hasWarehouseItems(Player player) {
        try (Connection conn = databaseManager.getDatabaseConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM auction_history " +
                "WHERE (seller_uuid = ? AND status = 'EXPIRED') " +
                "OR (buyer_uuid = ? AND status = 'SOLD')"
             )) {
            
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getUniqueId().toString());
            
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking warehouse items", e);
        }
        return false;
    }

    /**
     * Refreshes all open auction GUIs
     */
    private void refreshAllGUIs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof AuctionMainGUI gui) {
                gui.refresh();
            }
        }
    }

    public void shutdown() {
        try {
            auctionLock.lock();
            
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }

            cancelAllTimers();
            
            if (currentAuction != null) {
                cancelAuction(currentAuction);
            }

            while (!auctionQueue.isEmpty()) {
                AuctionItem queuedAuction = auctionQueue.poll();
                cancelAuction(queuedAuction);
            }

            clearAuctionQueue();
        } finally {
            auctionLock.unlock();
        }
    }

    private void cancelAllTimers() {
        if (auctionTimer != null) {
            auctionTimer.cancel(false);
            auctionTimer = null;
        }
        if (countdownTimer != null) {
            countdownTimer.cancel(false);
            countdownTimer = null;
        }
        if (autoCheckTimer != null) {
            autoCheckTimer.cancel(false);
            autoCheckTimer = null;
        }
    }

    /**
     * Saves auction to database queue
     * @throws SQLException if database operation fails
     */
    private void saveAuctionToDatabase(AuctionItem auctionItem) throws SQLException {
        try (Connection conn = databaseManager.getDatabaseConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO auction_queue (item_serialized, seller_uuid, seller_name, min_bid, step_value, start_time) " +
                             "VALUES (?, ?, ?, ?, ?, ?)"
             )) {
            stmt.setString(1, ItemSerializer.serializeItemStack(auctionItem.getItem()));
            stmt.setString(2, auctionItem.getSellerUUID().toString());
            stmt.setString(3, auctionItem.getSellerName());
            stmt.setDouble(4, auctionItem.getMinBid());
            stmt.setDouble(5, auctionItem.getStepValue());
            stmt.setLong(6, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    /**
     * Clears all auctions from queue table
     */
    private void clearAuctionQueue() {
        try (Connection conn = databaseManager.getDatabaseConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM auction_queue")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error clearing auction queue", e);
        }
    }

    /**
     * Loads pending auctions from database
     */
    public void loadPendingAuctions() {
        try (Connection conn = databaseManager.getDatabaseConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM auction_queue ORDER BY start_time ASC"
             )) {
            
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ItemStack item = ItemSerializer.deserializeItemStack(
                        rs.getString("item_serialized")
                    );
                    
                    if (item != null) {
                        Player seller = Bukkit.getPlayer(UUID.fromString(rs.getString("seller_uuid")));
                        if (seller != null) {
                            AuctionItem auction = new AuctionItem(
                                seller,
                                item,
                                rs.getDouble("min_bid"),
                                rs.getDouble("step_value"),
                                configManager.getAuctionDuration() * 1000L
                            );
                            auctionQueue.offer(auction);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading pending auctions", e);
        }
    }

    /**
     * Cancels an auction and refunds the current bidder
     */
    public boolean cancelAuction(AuctionItem auction) {
        if (auction == null) return false;

        if (auction.getCurrentBidderUUID() != null) {
            Player currentBidder = Bukkit.getPlayer(auction.getCurrentBidderUUID());
            if (currentBidder != null) {
                economyManager.depositPlayer(currentBidder, auction.getCurrentBid());
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("bid", String.valueOf(auction.getCurrentBid()));
                messageManager.sendMessage(currentBidder, "messages.player.bids.refunded", placeholders);
            }
        }

        Player seller = Bukkit.getPlayer(auction.getSellerUUID());
        if (seller != null) {
            HashMap<Integer, ItemStack> leftover = seller.getInventory().addItem(auction.getItem());
            if (!leftover.isEmpty()) {
                saveToWarehouse(auction, AuctionStatus.CANCELLED);
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(seller, "messages.player.auction.inventory_full", placeholders);
            } else {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(seller, "messages.player.auction.cancelled", placeholders);
            }
        } else {
            saveToWarehouse(auction, AuctionStatus.CANCELLED);
        }

        return true;
    }

    /**
     * Gets the current active auction
     * @return The current auction or null if none is active
     */
    public AuctionItem getCurrentAuction() {
        return currentAuction;
    }

    /**
     * Places a bid on the current auction
     * @param bidder The player placing the bid
     * @param amount The bid amount
     * @return true if bid was successful, false otherwise
     */
    public boolean placeBid(Player bidder, double amount) {
        try {
            auctionLock.lock();

            if (currentAuction == null) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(bidder, "messages.player.bids.failed.no_auction", placeholders);
                return false;
            }

            // if (currentAuction.getSellerUUID().equals(bidder.getUniqueId())) {
            //     Map<String, String> placeholders = new HashMap<>();
            //     messageManager.sendMessage(bidder, "messages.player.bids.failed.seller", placeholders);
            //     return false;
            // }

            if (!economyManager.hasBalance(bidder, amount)) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(bidder, "messages.player.bids.failed.balance", placeholders);
                return false;
            }

            UUID previousBidderUUID = currentAuction.getCurrentBidderUUID();
            double previousBidAmount = currentAuction.getCurrentBid();

            if (!economyManager.withdrawPlayer(bidder, amount)) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(bidder, "messages.player.bids.failed.balance", placeholders);
                return false;
            }

            if (currentAuction.placeBid(bidder, amount)) {
                // If there was a previous bid, refund the money to the previous bidder
                if (previousBidderUUID != null) {
                    Player previousBidder = Bukkit.getPlayer(previousBidderUUID);
                    if (previousBidder != null) {
                        economyManager.depositPlayer(previousBidder, previousBidAmount);
                        
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("bid", economyManager.formatMoney(previousBidAmount));
                        messageManager.sendMessage(previousBidder, "messages.player.bids.outbid", placeholders);
                    }
                }

                // Send success message
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("bidder", bidder.getName());
                placeholders.put("bid", economyManager.formatMoney(amount));
                placeholders.put("item", currentAuction.getItem().getType().name());
                messageManager.broadcast("messages.broadcasts.bids.new_bid", placeholders);

                refreshAllGUIs();
                return true;
            }

            return false;
        } finally {
            auctionLock.unlock();
        }
    }

    /**
     * Opens the main auction GUI for a player
     * @param player The player to show the GUI to
     */
    public void openMainGUI(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            new AuctionMainGUI(plugin, player).open();
        });
    }

    /**
     * Sets the current auction directly (admin only)
     * @param auction The auction to set as current
     */
    public void setCurrentAuction(AuctionItem auction) {
        try {
            auctionLock.lock();
            
            if (currentAuction != null) {
                cancelAuction(currentAuction);
            }
            
            currentAuction = auction;
            if (currentAuction != null) {
                currentAuction.start();
                scheduleAuctionTimers(currentAuction);
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("seller", currentAuction.getSellerName());
                placeholders.put("item", currentAuction.getItem().getType().name());
                messageManager.broadcast("messages.admin.force_start", placeholders);
                
                refreshAllGUIs();
            }
        } finally {
            auctionLock.unlock();
        }
    }

    /**
     * Reloads auction settings from config
     */
    public void reloadFromConfig() {
        try {
            auctionLock.lock();
            cancelAllTimers();
            
            if (currentAuction != null) {
                currentAuction.start(); 
                scheduleAuctionTimers(currentAuction);
            }
        } finally {
            auctionLock.unlock();
        }
    }

    /**
     * Time checkpoints for auction broadcasts
     */
    private enum TimeCheckpoint {
        HALF_TIME,
        QUARTER_TIME,
        TENTH_TIME,
        TEN_SECONDS,
        THREE_SECONDS,
        TWO_SECONDS,
        ONE_SECOND
    }

    /**
     * Queues a new auction
     * @return true if auction was queued successfully
     */
    public boolean queueAuction(Player seller, ItemStack item, double minBid, double stepValue) {
        try {
            auctionLock.lock();

            if (ItemSerializer.isItemBanned(item)) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(seller, "messages.player.auction.banned_item", placeholders);
                return false;
            }

            if (auctionQueue.size() >= maxQueueSize) {
                Map<String, String> placeholders = new HashMap<>();
                messageManager.sendMessage(seller, "messages.player.auction.queue_full", placeholders);
                return false;
            }

            if (publicationFee > 0 && !economyManager.withdrawPlayer(seller, publicationFee)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", "Insufficient funds for publication fee");
                messageManager.sendMessage(seller, "messages.player.auction.start_failed", placeholders);
                return false;
            }

            AuctionItem auction = new AuctionItem(seller, item, minBid, stepValue, 
                configManager.getAuctionDuration() * 1000L);
            
            if (auctionQueue.offer(auction)) {
                try {
                    saveAuctionToDatabase(auction);

                    if (currentAuction == null && !isProcessingAuction) {
                        scheduleNextAuction();
                    }
                    return true;
                } catch (SQLException e) {
                    logger.error("Failed to save auction to database", e);
                    auctionQueue.remove(auction);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("reason", "Database error");
                    messageManager.sendMessage(seller, "messages.player.auction.start_failed", placeholders);
                    return false;
                }
            }

            return false;
        } finally {
            auctionLock.unlock();
        }
    }

    /**
     * Schedules timers for auction events
     */
    private void scheduleAuctionTimers(AuctionItem auction) {
        cancelAllTimers();
        
        long duration = auction.getDuration();
        long halfTime = duration / 2;
        long quarterTime = duration / 4;
        long tenthTime = duration / 10;
        
        // Schedule main auction timer
        auctionTimer = scheduler.schedule(
            () -> Bukkit.getScheduler().runTask(plugin, this::scheduleNextAuction),
            duration,
            TimeUnit.MILLISECONDS
        );

        // Schedule time checkpoints
        scheduler.schedule(
            () -> broadcastTimeCheckpoint(TimeCheckpoint.HALF_TIME),
            halfTime,
            TimeUnit.MILLISECONDS
        );

        scheduler.schedule(
            () -> broadcastTimeCheckpoint(TimeCheckpoint.QUARTER_TIME),
            duration - quarterTime,
            TimeUnit.MILLISECONDS
        );

        scheduler.schedule(
            () -> broadcastTimeCheckpoint(TimeCheckpoint.TENTH_TIME),
            duration - tenthTime,
            TimeUnit.MILLISECONDS
        );

        // Schedule countdown if enabled
        if (messageManager.isCountdownEnabled()) {
            scheduleCountdown();
        }
    }

    /**
     * Schedules countdown messages
     */
    private void scheduleCountdown() {
        countdownTimer = scheduler.scheduleAtFixedRate(() -> {
            if (currentAuction == null) {
                cancelAllTimers();
                return;
            }

            long remaining = currentAuction.getRemainingTime();
            if (remaining <= 10000) { // 10 seconds
                TimeCheckpoint checkpoint = switch ((int) (remaining / 1000)) {
                    case 10 -> TimeCheckpoint.TEN_SECONDS;
                    case 3 -> TimeCheckpoint.THREE_SECONDS;
                    case 2 -> TimeCheckpoint.TWO_SECONDS;
                    case 1 -> TimeCheckpoint.ONE_SECOND;
                    default -> null;
                };
                
                if (checkpoint != null) {
                    broadcastTimeCheckpoint(checkpoint);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Enum for auction status
     */
    public enum AuctionStatus {
        ACTIVE,
        SOLD,
        EXPIRED,
        CANCELLED,
        COLLECTED
    }

    public List<AuctionItem> getQueuePreview(int limit) {
        List<AuctionItem> preview = new ArrayList<>();
        if (currentAuction != null) {
            preview.add(currentAuction);
        }
        preview.addAll(auctionQueue.stream().limit(limit - 1).toList());
        return preview;
    }

    public AuctionItem getQueueItemAt(int index) {
        if (index < 0) return null;
        if (index == 0) return currentAuction;
        
        List<AuctionItem> queueList = new ArrayList<>(auctionQueue);
        index--;
        return index < queueList.size() ? queueList.get(index) : null;
    }

    public void removeFromQueue(AuctionItem auction) {
        if (auction == currentAuction) {
            setCurrentAuction(null);
        } else {
            auctionQueue.remove(auction);
        }
    }
}

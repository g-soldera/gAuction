package com.gsoldera.gAuction.auction;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonObject;
import com.gsoldera.gAuction.GAuctionPlugin;
import com.gsoldera.gAuction.economy.EconomyManager;
import com.gsoldera.gAuction.utils.ItemSerializer;

/**
 * Represents an item being auctioned
 * Manages auction state, bidding, and timing
 */
public class AuctionItem {
    private final UUID id;
    private final UUID sellerUUID;
    private final String sellerName;

    private final ItemStack item;
    private final JsonObject itemDetails;

    private final long duration;
    private final double minBid;
    private final double stepValue;
    
    private double currentBid;
    private UUID currentBidderUUID;
    private String currentBidderName;

    private long startTime;
    private long endTime;
    private AuctionStatus status;

    /**
     * Represents the possible states of an auction
     */
    public enum AuctionStatus {
        /** Auction is currently active and accepting bids */
        ACTIVE,
        /** Auction ended with a successful sale */
        SOLD,
        /** Auction ended without any bids */
        EXPIRED,
        /** Auction was cancelled by admin or system */
        CANCELLED,
        /** Auction items have been collected by winner/seller */
        COLLECTED
    }

    /**
     * Creates a new auction item
     * @param seller The player creating the auction
     * @param item The item being auctioned
     * @param minBid Minimum starting bid
     * @param stepValue Minimum increment between bids
     * @param duration Duration of auction in milliseconds
     */
    public AuctionItem(Player seller, ItemStack item, double minBid, double stepValue, long duration) {
        this.id = UUID.randomUUID();
        this.sellerUUID = seller.getUniqueId();
        this.sellerName = seller.getName();

        this.item = item.clone();
        this.itemDetails = ItemSerializer.createItemDetailsJson(item);

        this.minBid = minBid;
        this.stepValue = stepValue;
        this.currentBid = minBid;

        this.duration = duration;
        this.status = AuctionStatus.ACTIVE;
    }

    /**
     * Places a bid on this auction
     * @param bidder The player placing the bid
     * @param bidAmount The amount being bid
     * @return true if bid was successful, false otherwise
     */
    public boolean placeBid(Player bidder, double bidAmount) {
        if (status != AuctionStatus.ACTIVE) {
            return false;
        }

        if (bidAmount < getMinimumNextBid()) {
            return false;
        }

        if (!hasEnoughMoney(bidder, bidAmount)) {
            return false;
        }

        this.currentBid = bidAmount;
        this.currentBidderUUID = bidder.getUniqueId();
        this.currentBidderName = bidder.getName();

        return true;
    }

    /**
     * Starts or restarts the auction timer
     */
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + duration;
    }

    /**
     * Gets the minimum amount required for the next bid
     * @return The minimum next bid amount
     */
    public double getMinimumNextBid() {
        if (GAuctionPlugin.getInstance().getConfigManager().isStepEnabled()) {
            return currentBid + stepValue;
        }
        return currentBid + 0.01;
    }

    private boolean hasEnoughMoney(Player bidder, double amount) {
        EconomyManager economyManager = GAuctionPlugin.getInstance().getEconomyManager();
        return economyManager != null && economyManager.hasBalance(bidder, amount);
    }

    /**
     * Finalizes the auction with the given result
     * @param sold true if auction ended with a sale, false if expired
     */
    public void finalize(boolean sold) {
        this.status = sold ? AuctionStatus.SOLD : AuctionStatus.EXPIRED;
    }

    /**
     * Checks if the auction has expired
     * @return true if auction time has elapsed, false otherwise
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() >= endTime;
    }

    /**
     * Gets the remaining time in milliseconds
     * @return Remaining time or 0 if expired
     */
    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getSellerUUID() { return sellerUUID; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item; }
    public JsonObject getItemDetails() { return itemDetails; }
    public double getMinBid() { return minBid; }
    public double getCurrentBid() { return currentBid; }
    public UUID getCurrentBidderUUID() { return currentBidderUUID; }
    public String getCurrentBidderName() { return currentBidderName; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public double getStepValue() { return stepValue; }
    public long getDuration() { return duration; }

    @Override
    public String toString() {
        return "AuctionItem{" +
                "id=" + id +
                ", seller=" + sellerName +
                ", item=" + item.getType() +
                ", currentBid=" + currentBid +
                ", status=" + status +
                '}';
    }
}

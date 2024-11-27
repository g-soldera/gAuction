package com.gsoldera.gAuction.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.slf4j.Logger;

import com.gsoldera.gAuction.GAuctionPlugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

/**
 * Manages economy operations using Vault API
 * Handles all money transactions for auctions
 */
public final class EconomyManager {
    private final Logger logger;
    private Economy economyProvider;
    private boolean economyEnabled;

    /**
     * Creates a new economy manager
     * @param plugin Plugin instance for logging
     */
    public EconomyManager(GAuctionPlugin plugin) {
        this.logger = plugin.getPluginLogger();
        this.economyEnabled = false;
        initializeEconomySupport();
    }

    /**
     * Initializes economy support through Vault
     */
    private void initializeEconomySupport() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.warn("Vault not found. Economy features will be limited.");
            return;
        }

        try {
            RegisteredServiceProvider<Economy> registration = 
                Bukkit.getServicesManager().getRegistration(Economy.class);

            if (registration != null) {
                this.economyProvider = registration.getProvider();
                this.economyEnabled = true;
                logger.info("Economy support initialized with: {}", 
                    registration.getProvider().getName());
            } else {
                logger.warn("No economy provider found.");
            }
        } catch (Exception e) {
            logger.error("Error setting up economy provider", e);
        }
    }

    /**
     * Checks if economy support is enabled
     * @return true if economy is enabled and provider is available
     */
    public boolean isEconomyEnabled() {
        return economyEnabled && economyProvider != null;
    }

    /**
     * Checks if a player has enough balance
     * @param player Player to check
     * @param amount Amount to check for
     * @return true if player has enough balance
     */
    public boolean hasBalance(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return false;
        }

        return economyProvider.has(player, amount);
    }

    /**
     * Withdraws money from player's account
     * @param player Player to withdraw from
     * @param amount Amount to withdraw
     * @return true if withdrawal was successful
     */
    public boolean withdrawPlayer(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return true; // Allow transactions when economy is disabled
        }

        if (!hasBalance(player, amount)) {
            return false;
        }

        EconomyResponse response = economyProvider.withdrawPlayer(player, amount);

        if (!response.transactionSuccess()) {
            logger.warn("Transaction failed: {}", response.errorMessage);
            return false;
        }

        return true;
    }

    /**
     * Deposits money into player's account
     * @param player Player to deposit to
     * @param amount Amount to deposit
     * @return true if deposit was successful
     */
    public boolean depositPlayer(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return true; // Allow transactions when economy is disabled
        }

        EconomyResponse response = economyProvider.depositPlayer(player, amount);

        if (!response.transactionSuccess()) {
            logger.warn("Deposit failed: {}", response.errorMessage);
            return false;
        }

        return true;
    }

    /**
     * Gets player's current balance
     * @param player Player to check
     * @return Player's balance or 0 if economy is disabled
     */
    public double getBalance(Player player) {
        if (!isEconomyEnabled()) {
            return 0.0;
        }

        return economyProvider.getBalance(player);
    }

    /**
     * Formats a monetary value according to economy provider
     * @param amount Amount to format
     * @return Formatted string representation of amount
     */
    public String formatMoney(double amount) {
        if (!isEconomyEnabled()) {
            return String.format("%.2f", amount);
        }

        return economyProvider.format(amount);
    }

    /**
     * Gets the currency name (singular)
     * @return Currency name or "coin" if economy is disabled
     */
    public String getCurrencyName() {
        if (!isEconomyEnabled()) {
            return "coin";
        }
        return economyProvider.currencyNameSingular();
    }

    /**
     * Gets the currency name (plural)
     * @return Currency name or "coins" if economy is disabled
     */
    public String getCurrencyNamePlural() {
        if (!isEconomyEnabled()) {
            return "coins";
        }
        return economyProvider.currencyNamePlural();
    }
}

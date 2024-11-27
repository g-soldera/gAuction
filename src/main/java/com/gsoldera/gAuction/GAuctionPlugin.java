package com.gsoldera.gAuction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import com.gsoldera.gAuction.auction.AuctionManager;
import com.gsoldera.gAuction.commands.AuctionAdminCommand;
import com.gsoldera.gAuction.commands.AuctionCommand;
import com.gsoldera.gAuction.config.ConfigManager;
import com.gsoldera.gAuction.database.DatabaseManager;
import com.gsoldera.gAuction.economy.EconomyManager;
import com.gsoldera.gAuction.gui.AuctionBidGUI;
import com.gsoldera.gAuction.gui.AuctionConfirmGUI;
import com.gsoldera.gAuction.gui.AuctionHistoryGUI;
import com.gsoldera.gAuction.gui.AuctionMainGUI;
import com.gsoldera.gAuction.gui.AuctionWarehouseGUI;
import com.gsoldera.gAuction.listeners.InventoryClickListener;
import com.gsoldera.gAuction.messages.MessageManager;
import com.gsoldera.gAuction.utils.ItemSerializer;

/**
 * Main plugin class for GAuction
 * Handles initialization and shutdown of all plugin components
 */
public final class GAuctionPlugin extends JavaPlugin {
    private static GAuctionPlugin instance;

    // Core components
    private Logger logger;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private AuctionManager auctionManager;
    private MessageManager messageManager;
    private ItemSerializer itemSerializer;

    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        instance = this;
        logger = getSLF4JLogger();

        try {
            logger.info("Starting GAuction v{}", getDescription().getVersion());
            initializeComponents();
            registerCommands();
            registerListeners();
            logger.info("GAuction initialized successfully!");
        } catch (Exception e) {
            logger.error("Error during plugin initialization", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initializes all plugin components in correct order
     */
    private void initializeComponents() {
        // Config must be first
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Message system depends on config
        messageManager = new MessageManager(this);

        // Economy can work without other components
        economyManager = new EconomyManager(this);
        if (!economyManager.isEconomyEnabled()) {
            logger.warn("No economy provider found. Economy features will be limited.");
        }

        // Database depends on config
        databaseManager = new DatabaseManager(this, configManager);
        databaseManager.initialize();

        // Auction system depends on everything else
        auctionManager = new AuctionManager(this);
        auctionManager.loadPendingAuctions();
    }

    private void registerCommands() {
        var auctionCommand = getCommand("auction");
        var adminCommand = getCommand("auctionadmin");
        
        if (auctionCommand != null) {
            auctionCommand.setExecutor(new AuctionCommand(this));
        } else {
            logger.error("Command 'auction' not found in plugin.yml");
        }
        
        if (adminCommand != null) {
            adminCommand.setExecutor(new AuctionAdminCommand(this));
        } else {
            logger.error("Command 'auctionadmin' not found in plugin.yml");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
    }

    @Override
    public void onDisable() {
        try {
            // Close all open GUIs
            for (Player player : getServer().getOnlinePlayers()) {
                InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
                if (holder instanceof AuctionMainGUI || 
                    holder instanceof AuctionHistoryGUI || 
                    holder instanceof AuctionWarehouseGUI || 
                    holder instanceof AuctionBidGUI || 
                    holder instanceof AuctionConfirmGUI) {
                    player.closeInventory();
                }
            }

            // Shutdown components in reverse order
            if (auctionManager != null) {
                auctionManager.shutdown();
            }

            if (databaseManager != null) {
                databaseManager.shutdown();
            }

            logger.info("GAuction disabled successfully");
        } catch (Exception e) {
            logger.error("Error during plugin shutdown", e);
        } finally {
            instance = null;
        }
    }

    /**
     * Gets the plugin instance
     * @return The plugin instance
     * @throws IllegalStateException if plugin is not enabled
     */
    public static GAuctionPlugin getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Plugin is not enabled");
        }
        return instance;
    }

    // Getters
    public Logger getPluginLogger() { return logger; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public ItemSerializer getItemSerializer() { return itemSerializer; }
}

package com.gsoldera.gAuction.messages;

import java.io.File;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.gsoldera.gAuction.GAuctionPlugin;

import net.md_5.bungee.api.ChatColor;

/**
 * Manages all plugin messages and broadcasts
 * Uses BungeeCord Chat API for rich text formatting
 */
public final class MessageManager {
    private final GAuctionPlugin plugin;
    private final Logger logger;
    private FileConfiguration messages;
    
    // Message settings
    private boolean messagesEnabled;
    private boolean broadcastsEnabled;
    private boolean countdownEnabled;
    private boolean bidBroadcastsEnabled;

    private String currentLanguage;

    /**
     * Creates a new message manager
     * @param plugin Plugin instance
     */
    public MessageManager(GAuctionPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPluginLogger();
        loadMessages();
    }

    /**
     * Loads all messages from the appropriate language file
     */
    public void loadMessages() {
        // Get language from config
        currentLanguage = plugin.getConfig().getString("language", "en-US");
        
        // Load language file
        File messagesFile = new File(plugin.getDataFolder(), "messages_" + currentLanguage + ".yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages_" + currentLanguage + ".yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load message settings
        messagesEnabled = messages.getBoolean("messages.enabled", true);
        broadcastsEnabled = messages.getBoolean("messages.broadcasts.enabled", true);
        countdownEnabled = messages.getBoolean("messages.broadcasts.countdown.enabled", true);
        bidBroadcastsEnabled = messages.getBoolean("messages.broadcasts.bids.enabled", true);
        
        logger.info("Messages loaded successfully for language: {}", currentLanguage);
    }

    /**
     * Sends a message to a player with rich text formatting
     */
    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        if (!messagesEnabled || player == null) return;
        player.sendMessage(getPlainMessage(key, placeholders));
    }

    /**
     * Broadcasts a message with rich text formatting
     */
    public void broadcast(String key, Map<String, String> placeholders) {
        if (!messagesEnabled || !broadcastsEnabled) {
            logger.debug("Broadcast disabled: messagesEnabled={}, broadcastsEnabled={}", 
                messagesEnabled, broadcastsEnabled);
            return;
        }
        
        String message = messages.getString(key);
        if (message == null || message.isEmpty()) {
            logger.warn("No message found for broadcast key: {}", key);
            return;
        }
        
        String prefix = messages.getString("messages.prefix", "&6[Leilão]&r ");
        message = message.replace("{prefix}", prefix);
        
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue();
                if (value != null) {
                    message = message.replace("{" + entry.getKey() + "}", value);
                }
            }
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Gets a plain text message with placeholders replaced
     * @param key Message key
     * @param placeholders Placeholder map
     * @return Formatted string or empty string if message not found
     */
    public String getPlainMessage(String key, Map<String, String> placeholders) {
        String message = messages.getString(key);
        if (message == null) {
            logger.warn("No message found for key: {}", key);
            return "Message not found: " + key;
        }

        String prefix = messages.getString("messages.prefix", "");
        message = message.replace("{prefix}", prefix);

        message = ChatColor.translateAlternateColorCodes('&', message);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue();
                if (value != null) {
                    message = message.replace("{" + entry.getKey() + "}", value);
                }
            }
        }
        return message;
    }

    /**
     * Reloads all messages from config
     */
    public void reload() {
        loadMessages();
    }

    /**
     * Sends a raw message to any command sender
     */
    public void sendRawMessage(CommandSender sender, String message) {
        if (!messagesEnabled || sender == null || message == null) return;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    // Getters
    public boolean isMessagesEnabled() { return messagesEnabled; }
    public boolean isBroadcastsEnabled() { return broadcastsEnabled; }
    public boolean isCountdownEnabled() { return countdownEnabled; }
    public boolean isBidBroadcastsEnabled() { return bidBroadcastsEnabled; }

    /**
     * Gets the current language code
     * @return Current language code (e.g. "en-US")
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
} 
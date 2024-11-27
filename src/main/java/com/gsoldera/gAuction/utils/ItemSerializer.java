package com.gsoldera.gAuction.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.gsoldera.gAuction.GAuctionPlugin;

/**
 * Handles ItemStack serialization and deserialization
 * Provides methods for converting items to/from various formats
 */
public final class ItemSerializer {
    private static final Logger logger = GAuctionPlugin.getInstance().getPluginLogger();
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ItemSerializer() {
        // Prevent instantiation
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Serializes an ItemStack to Base64 string
     * @param item ItemStack to serialize
     * @return Base64 encoded string or null if serialization fails
     */
    public static String serializeItemStack(ItemStack item) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            logger.error("Error serializing ItemStack", e);
            return null;
        }
    }

    /**
     * Deserializes a Base64 string to ItemStack
     * @param serializedItem Base64 encoded string
     * @return ItemStack or null if deserialization fails
     */
    public static ItemStack deserializeItemStack(String serializedItem) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(serializedItem));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            return (ItemStack) dataInput.readObject();
        } catch (Exception e) {
            logger.error("Error deserializing ItemStack", e);
            return null;
        }
    }

    /**
     * Creates a detailed JSON representation of an item
     * @param item ItemStack to convert
     * @return JsonObject with item details or null if conversion fails
     */
    @SuppressWarnings("deprecation")
    public static JsonObject createItemDetailsJson(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        JsonObject itemDetails = new JsonObject();

        try {
            // Basic information
            itemDetails.addProperty("material", item.getType().getKey().toString());
            itemDetails.addProperty("amount", item.getAmount());

            // Durability
            if (item.getDurability() > 0) {
                itemDetails.addProperty("durability", item.getDurability());
            }

            // Custom metadata
            if (item.hasItemMeta()) {
                JsonObject metaDetails = new JsonObject();
                var meta = item.getItemMeta();

                // Display name
                if (meta.hasDisplayName()) {
                    metaDetails.addProperty("display_name", meta.getDisplayName());
                }

                // Lore
                if (meta.hasLore()) {
                    metaDetails.add("lore", gson.toJsonTree(meta.getLore()));
                }

                // Enchantments
                if (meta.hasEnchants()) {
                    JsonObject enchantments = new JsonObject();
                    meta.getEnchants().forEach((enchant, level) ->
                            enchantments.addProperty(enchant.getKey().getKey(), level)
                    );
                    metaDetails.add("enchantments", enchantments);
                }

                itemDetails.add("meta", metaDetails);
            }

            return itemDetails;
        } catch (Exception e) {
            logger.error("Error creating item JSON details", e);
            return null;
        }
    }

    /**
     * Checks if an item is in the banned items list
     * @param item ItemStack to check
     * @return true if item is banned, false otherwise
     */
    public static boolean isItemBanned(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return GAuctionPlugin.getInstance()
                .getConfigManager()
                .getBannedItems()
                .contains(item.getType().getKey().toString());
    }

    /**
     * Compares two items considering material, amount, and metadata
     * @param item1 First ItemStack
     * @param item2 Second ItemStack
     * @return true if items are considered equal
     */
    public static boolean compareItems(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;

        // Compare material and amount
        if (item1.getType() != item2.getType() || 
            item1.getAmount() != item2.getAmount()) {
            return false;
        }

        // Compare metadata if both have it
        if (!item1.hasItemMeta() && !item2.hasItemMeta()) return true;
        if (!item1.hasItemMeta() || !item2.hasItemMeta()) return false;

        return item1.getItemMeta().equals(item2.getItemMeta());
    }

    /**
     * Reconstructs an ItemStack from its JSON details
     * @param itemDetails JsonObject with item details
     * @return ItemStack or null if reconstruction fails
     */
    @SuppressWarnings("deprecation")
    public static ItemStack fromItemDetailsJson(JsonObject itemDetails) {
        try {
            // Base material
            Material material = Material.matchMaterial(
                itemDetails.get("material").getAsString()
            );

            if (material == null) {
                logger.warn("Material not found: {}", 
                    itemDetails.get("material").getAsString());
                return null;
            }

            // Create ItemStack
            ItemStack item = new ItemStack(material,
                itemDetails.has("amount") ? 
                    itemDetails.get("amount").getAsInt() : 1
            );

            // Add metadata if exists
            if (itemDetails.has("meta")) {
                var meta = item.getItemMeta();
                JsonObject metaDetails = itemDetails.getAsJsonObject("meta");

                // Display name
                if (metaDetails.has("display_name")) {
                    meta.setDisplayName(metaDetails.get("display_name").getAsString());
                }

                // Lore
                if (metaDetails.has("lore")) {
                    meta.setLore(gson.fromJson(
                        metaDetails.get("lore"), 
                        new com.google.gson.reflect.TypeToken<java.util.List<String>>(){}.getType()
                    ));
                }

                item.setItemMeta(meta);
            }

            return item;
        } catch (JsonSyntaxException e) {
            logger.error("Error reconstructing item from JSON", e);
            return null;
        }
    }
}

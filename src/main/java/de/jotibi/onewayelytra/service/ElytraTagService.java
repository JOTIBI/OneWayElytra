package de.jotibi.onewayelytra.service;

import de.jotibi.onewayelytra.OneWayElytraPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ElytraTagService {

    private final OneWayElytraPlugin plugin;
    private final NamespacedKey oneWayKey;

    public ElytraTagService(OneWayElytraPlugin plugin, NamespacedKey oneWayKey) {
        this.plugin = plugin;
        this.oneWayKey = oneWayKey;
    }

    public boolean isOneWayElytra(ItemStack item) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] ElytraTagService.isOneWayElytra - Item: %s", 
                item != null ? item.getType().toString() : "null"));
        }
        
        if (item == null || item.getType() != Material.ELYTRA) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info(String.format("[DEBUG] ElytraTagService.isOneWayElytra - Item ist null oder kein Elytra: %s", 
                    item == null ? "null" : item.getType().toString()));
            }
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[DEBUG] ElytraTagService.isOneWayElytra - ItemMeta ist null");
            }
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean hasTag = pdc.has(oneWayKey, PersistentDataType.INTEGER);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] ElytraTagService.isOneWayElytra - Hat PDC-Tag: %s", hasTag));
        }
        
        return hasTag;
    }

    public ItemStack tagElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(oneWayKey, PersistentDataType.INTEGER, 1);

        meta.displayName(Component.text("OneWay Elytra", NamedTextColor.GOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Spezielle Elytra mit Einschränkungen", NamedTextColor.GRAY));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack untagElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(oneWayKey);

        meta.displayName(null);
        meta.lore(null);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createOneWayElytra(int amount) {
        ItemStack elytra = new ItemStack(Material.ELYTRA, amount);
        return tagElytra(elytra);
    }
}

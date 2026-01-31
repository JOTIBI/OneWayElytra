package de.jotibi.onewayelytra.config;

import de.jotibi.onewayelytra.OneWayElytraPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final OneWayElytraPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(OneWayElytraPlugin plugin) {
        this.plugin = plugin;
    }
    
    public OneWayElytraPlugin getPlugin() {
        return plugin;
    }

    public void loadConfig() {
        java.io.File configFile = new java.io.File(plugin.getDataFolder(), "config.yml");
        boolean configExisted = configFile.exists();
        
        // WICHTIG: saveDefaultConfig() NUR aufrufen, wenn Config NICHT existiert
        // Dies verhindert, dass die Config beim Reload überschrieben wird
        if (!configExisted) {
            plugin.saveDefaultConfig();
            plugin.getLogger().info("Config.yml wurde erstellt (existierte nicht)");
        } else {
            plugin.getLogger().info("Config.yml existiert bereits, wird geladen (nicht überschrieben)");
        }
        
        // Reload lädt die existierende Config, überschreibt sie aber nicht
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        boolean debug = config.getBoolean("debug", false);
        plugin.getLogger().info("Config geladen - Debug-Modus: " + (debug ? "AN" : "AUS"));
        
        if (debug) {
            plugin.getLogger().info("[DEBUG] ===== DEBUG-MODUS AKTIVIERT =====");
            plugin.getLogger().info("[DEBUG] Config-Datei existierte vorher: " + configExisted);
            plugin.getLogger().info("[DEBUG] Config-Datei existiert jetzt: " + configFile.exists());
            plugin.getLogger().info("[DEBUG] Config-Datei Pfad: " + configFile.getAbsolutePath());
        }
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public Location getSpawnLocation() {
        if (config == null) {
            plugin.getLogger().warning("Config nicht geladen! Versuche erneut zu laden...");
            loadConfig();
            if (config == null) {
                return null;
            }
        }
        
        String worldName = config.getString("spawn.world", "world");
        double x = config.getDouble("spawn.x", 0.0);
        double y = config.getDouble("spawn.y", 64.0);
        double z = config.getDouble("spawn.z", 0.0);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            List<World> worlds = Bukkit.getWorlds();
            if (worlds.isEmpty()) {
                plugin.getLogger().severe("Keine Welten geladen! Verwende Standard-Welt.");
                return null;
            }
            world = worlds.get(0);
        }

        return new Location(world, x, y, z);
    }

    public void setSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Ungültige Location beim Setzen des Spawn-Punkts!");
            return;
        }
        if (config == null) {
            loadConfig();
            if (config == null) {
                plugin.getLogger().severe("Config konnte nicht geladen werden!");
                return;
            }
        }
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        saveConfig();
    }

    public int getRadius() {
        if (config == null) {
            loadConfig();
            if (config == null) {
                return 100;
            }
        }
        return config.getInt("radius", 100);
    }

    public void setRadius(int radius) {
        if (config == null) {
            loadConfig();
            if (config == null) {
                plugin.getLogger().severe("Config konnte nicht geladen werden!");
                return;
            }
        }
        config.set("radius", radius);
        saveConfig();
    }

    public RemoveMode getRemoveMode() {
        if (config == null) {
            loadConfig();
            if (config == null) {
                return RemoveMode.MOVE_TO_INVENTORY;
            }
        }
        String mode = config.getString("removeMode", "MOVE_TO_INVENTORY");
        try {
            return RemoveMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RemoveMode.MOVE_TO_INVENTORY;
        }
    }

    /**
     * Returns the language code (de or en) from config.
     */
    public String getLanguage() {
        if (config == null) {
            loadConfig();
            if (config == null) {
                return "de";
            }
        }
        return config.getString("lang", "de");
    }

    public boolean isDebug() {
        if (config == null) {
            loadConfig();
            if (config == null) {
                return false;
            }
        }
        return config.getBoolean("debug", false);
    }

    /**
     * Gibt die FileConfiguration zurück (für direkten Zugriff, z.B. zum Setzen von Werten)
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public enum RemoveMode {
        MOVE_TO_INVENTORY,
        DROP
    }
}

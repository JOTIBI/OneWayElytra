package de.jotibi.onewayelytra.service;

import de.jotibi.onewayelytra.OneWayElytraPlugin;
import de.jotibi.onewayelytra.config.ConfigManager;
import org.bukkit.Location;

public class ZoneService {

    private final ConfigManager configManager;
    private final OneWayElytraPlugin plugin;

    public ZoneService(ConfigManager configManager) {
        this.configManager = configManager;
        this.plugin = configManager.getPlugin();
    }

    public boolean isWithinRadius(Location location) {
        if (configManager.isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] ZoneService.isWithinRadius aufgerufen - Location: %s", 
                location != null ? location.toString() : "null"));
        }
        
        if (location == null || location.getWorld() == null) {
            if (configManager.isDebug()) {
                plugin.getLogger().warning("[DEBUG] ZoneService.isWithinRadius - Location oder World ist null");
            }
            return false;
        }
        
        Location spawn = configManager.getSpawnLocation();
        if (spawn == null || spawn.getWorld() == null) {
            if (configManager.isDebug()) {
                plugin.getLogger().warning("[DEBUG] ZoneService.isWithinRadius - Spawn Location oder World ist null");
            }
            return false;
        }
        
        if (!location.getWorld().equals(spawn.getWorld())) {
            if (configManager.isDebug()) {
                plugin.getLogger().info(String.format("[DEBUG] ZoneService.isWithinRadius - Welten unterschiedlich: %s != %s", 
                    location.getWorld().getName(), spawn.getWorld().getName()));
            }
            return false;
        }

        double distanceSquared = getDistanceSquared(location, spawn);
        int radius = configManager.getRadius();
        boolean within = distanceSquared <= (radius * radius);
        
        if (configManager.isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] ZoneService.isWithinRadius - Distanz²: %.2f, Radius²: %d, Ergebnis: %s", 
                distanceSquared, radius * radius, within));
        }
        
        return within;
    }

    /**
     * Berechnet die horizontale Distanz² (nur X und Z, ignoriert Y/Höhe).
     * So kann der Spieler beliebig hoch fliegen und bleibt trotzdem "im Radius".
     */
    public double getDistanceSquared(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) {
            return Double.MAX_VALUE;
        }
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        // Nur horizontale Distanz (X/Z), Y wird ignoriert
        return dx * dx + dz * dz;
    }
}

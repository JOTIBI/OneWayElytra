package de.jotibi.onewayelytra;

import de.jotibi.onewayelytra.command.OneWayElytraCommand;
import de.jotibi.onewayelytra.config.ConfigManager;
import de.jotibi.onewayelytra.config.LanguageManager;
import de.jotibi.onewayelytra.listener.ElytraListener;
import de.jotibi.onewayelytra.service.ElytraTagService;
import de.jotibi.onewayelytra.service.FlightTracker;
import de.jotibi.onewayelytra.service.ZoneService;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class OneWayElytraPlugin extends JavaPlugin {

    private static OneWayElytraPlugin instance;
    private NamespacedKey oneWayKey;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private ZoneService zoneService;
    private ElytraTagService elytraTagService;
    private FlightTracker flightTracker;

    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("OneWay Elytra Plugin is loading...");
        
        oneWayKey = new NamespacedKey(this, "oneway");
        getLogger().info("NamespacedKey created: " + oneWayKey);
        
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        languageManager = new LanguageManager(this, configManager);
        languageManager.loadLanguage();

        boolean debug = configManager.isDebug();
        if (debug) {
            getLogger().info("[DEBUG] ===== DEBUG MODE ACTIVATED =====");
            Location spawn = configManager.getSpawnLocation();
            if (spawn != null) {
                getLogger().info(String.format("[DEBUG] Spawn: %s @ %.1f, %.1f, %.1f, Radius: %d", 
                    spawn.getWorld().getName(), spawn.getX(), spawn.getY(), spawn.getZ(), 
                    configManager.getRadius()));
            } else {
                getLogger().warning("[DEBUG] Spawn Location is null!");
            }
        }
        
        zoneService = new ZoneService(configManager);
        elytraTagService = new ElytraTagService(this, oneWayKey);
        flightTracker = new FlightTracker();
        
        if (debug) {
            getLogger().info("[DEBUG] Services initialized");
        }
        
        OneWayElytraCommand commandHandler = new OneWayElytraCommand(this, configManager, languageManager, elytraTagService);
        org.bukkit.command.PluginCommand command = getCommand("oe");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
            if (debug) {
                getLogger().info("[DEBUG] Command 'oe' registered");
            }
        } else {
            getLogger().severe("Command 'oe' not found in plugin.yml!");
        }
        
        getServer().getPluginManager().registerEvents(
            new ElytraListener(this, configManager, languageManager, zoneService, elytraTagService, flightTracker),
            this
        );
        
        if (debug) {
            getLogger().info("[DEBUG] Event Listener registered");
            getLogger().info("[DEBUG] ===== Plugin fully loaded =====");
        }
        
        getLogger().info("OneWay Elytra Plugin was successfully loaded!");
    }

    @Override
    public void onDisable() {
        // Config wird automatisch beim Setzen von Werten gespeichert (setSpawnLocation, setRadius, etc.)
        // Wir müssen sie NICHT beim onDisable speichern, da das die Config überschreibt
        getLogger().info("OneWay Elytra Plugin was unloaded!");
    }

    public static OneWayElytraPlugin getInstance() {
        return instance;
    }

    public NamespacedKey getOneWayKey() {
        return oneWayKey;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ZoneService getZoneService() {
        return zoneService;
    }

    public ElytraTagService getElytraTagService() {
        return elytraTagService;
    }

    public FlightTracker getFlightTracker() {
        return flightTracker;
    }
}

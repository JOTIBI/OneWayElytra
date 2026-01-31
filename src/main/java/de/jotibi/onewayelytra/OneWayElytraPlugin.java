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
        
        getLogger().info("OneWay Elytra Plugin wird geladen...");
        
        oneWayKey = new NamespacedKey(this, "oneway");
        getLogger().info("NamespacedKey erstellt: " + oneWayKey);
        
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        languageManager = new LanguageManager(this, configManager);
        languageManager.loadLanguage();

        boolean debug = configManager.isDebug();
        if (debug) {
            getLogger().info("[DEBUG] ===== DEBUG-MODUS AKTIVIERT =====");
            Location spawn = configManager.getSpawnLocation();
            if (spawn != null) {
                getLogger().info(String.format("[DEBUG] Spawn: %s @ %.1f, %.1f, %.1f, Radius: %d", 
                    spawn.getWorld().getName(), spawn.getX(), spawn.getY(), spawn.getZ(), 
                    configManager.getRadius()));
            } else {
                getLogger().warning("[DEBUG] Spawn Location ist null!");
            }
        }
        
        zoneService = new ZoneService(configManager);
        elytraTagService = new ElytraTagService(this, oneWayKey);
        flightTracker = new FlightTracker();
        
        if (debug) {
            getLogger().info("[DEBUG] Services initialisiert");
        }
        
        OneWayElytraCommand commandHandler = new OneWayElytraCommand(this, configManager, languageManager, elytraTagService);
        org.bukkit.command.PluginCommand command = getCommand("oe");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
            if (debug) {
                getLogger().info("[DEBUG] Command 'oe' registriert");
            }
        } else {
            getLogger().severe("Command 'oe' nicht in plugin.yml gefunden!");
        }
        
        getServer().getPluginManager().registerEvents(
            new ElytraListener(this, configManager, languageManager, zoneService, elytraTagService, flightTracker),
            this
        );
        
        if (debug) {
            getLogger().info("[DEBUG] Event Listener registriert");
            getLogger().info("[DEBUG] ===== Plugin vollständig geladen =====");
        }
        
        getLogger().info("OneWay Elytra Plugin wurde erfolgreich geladen!");
    }

    @Override
    public void onDisable() {
        if (configManager != null) {
            configManager.saveConfig();
        }
        getLogger().info("OneWay Elytra Plugin wurde entladen!");
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

package de.jotibi.onewayelytra.config;

import de.jotibi.onewayelytra.OneWayElytraPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Loads language files (lang/de.yml, lang/en.yml) and provides translated messages.
 */
public class LanguageManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final OneWayElytraPlugin plugin;
    private final ConfigManager configManager;
    private FileConfiguration langConfig;
    private String currentLang;

    public LanguageManager(OneWayElytraPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Loads the language file according to config (lang: de or en).
     * Saves default lang files from resources if they don't exist.
     */
    public void loadLanguage() {
        String lang = configManager.getLanguage();
        if (lang == null || lang.isEmpty()) {
            lang = "en";
        }
        lang = lang.toLowerCase();
        if (!lang.equals("de") && !lang.equals("en")) {
            plugin.getLogger().warning("Unknown language '" + lang + "', using 'en'");
            lang = "en";
        }

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Save default lang files from jar if they don't exist
        for (String l : new String[] { "de", "en" }) {
            File f = new File(langFolder, l + ".yml");
            if (!f.exists() && plugin.getResource("lang/" + l + ".yml") != null) {
                plugin.saveResource("lang/" + l + ".yml", false);
            }
        }

        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + lang + ".yml not found, using en");
            lang = "en";
            langFile = new File(langFolder, "en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
        if (langConfig == null) {
            langConfig = new YamlConfiguration();
        }
        currentLang = lang;
        plugin.getLogger().info("Language loaded: " + lang);
    }

    /**
     * Returns the raw message string for the key, or the key if missing.
     */
    public String getMessage(String key) {
        if (langConfig == null) {
            loadLanguage();
        }
        String value = langConfig.getString(key);
        return value != null ? value : key;
    }

    /**
     * Returns the message with %s replaced by the given arguments (in order).
     */
    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (String r : replacements) {
            msg = msg.replaceFirst("%s", r == null ? "" : r);
        }
        return msg;
    }

    /**
     * Returns the message as a Component (parses & color codes).
     */
    public Component getComponent(String key) {
        return LEGACY.deserialize(getMessage(key));
    }

    /**
     * Returns the message as a Component with %s replacements (parses & color codes).
     */
    public Component getComponent(String key, String... replacements) {
        return LEGACY.deserialize(getMessage(key, replacements));
    }

    public String getCurrentLanguage() {
        return currentLang;
    }
}

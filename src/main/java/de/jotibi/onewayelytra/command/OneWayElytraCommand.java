package de.jotibi.onewayelytra.command;

import de.jotibi.onewayelytra.OneWayElytraPlugin;
import de.jotibi.onewayelytra.config.ConfigManager;
import de.jotibi.onewayelytra.config.LanguageManager;
import de.jotibi.onewayelytra.service.ElytraTagService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OneWayElytraCommand implements CommandExecutor, TabCompleter {

    private final OneWayElytraPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    private final ElytraTagService elytraTagService;

    public OneWayElytraCommand(OneWayElytraPlugin plugin, ConfigManager configManager,
                               LanguageManager languageManager, ElytraTagService elytraTagService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageManager = languageManager;
        this.elytraTagService = elytraTagService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "setspawn":
                return handleSetSpawn(sender);
            case "setradius":
                return handleSetRadius(sender, args);
            case "setremovemode":
                return handleSetRemoveMode(sender, args);
            case "setlang":
            case "setlanguage":
                return handleSetLanguage(sender, args);
            case "info":
                return handleInfo(sender);
            case "reload":
                return handleReload(sender);
            case "give":
                return handleGive(sender, args);
            case "tag":
                return handleTag(sender);
            case "untag":
                return handleUntag(sender);
            case "check":
                return handleCheck(sender);
            case "debug":
                return handleDebug(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleSetSpawn(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getComponent("command.player_only"));
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            player.sendMessage(languageManager.getComponent("command.world_not_found"));
            return true;
        }
        
        configManager.setSpawnLocation(location);
        String pos = String.format("%s (%.1f, %.1f, %.1f)",
            location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
        player.sendMessage(languageManager.getComponent("command.setspawn_success", pos));

        if (configManager.isDebug()) {
            plugin.getLogger().info(String.format("Spawn gesetzt: %s @ %.1f, %.1f, %.1f",
                location.getWorld().getName(), location.getX(), location.getY(), location.getZ()));
        }

        return true;
    }

    private boolean handleSetRadius(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(languageManager.getComponent("command.setradius_usage"));
            return true;
        }

        try {
            int radius = Integer.parseInt(args[1]);
            if (radius < 0) {
                sender.sendMessage(languageManager.getComponent("command.setradius_positive"));
                return true;
            }

            configManager.setRadius(radius);
            sender.sendMessage(languageManager.getComponent("command.setradius_success", String.valueOf(radius)));

            if (configManager.isDebug()) {
                plugin.getLogger().info("Radius gesetzt: " + radius);
            }

            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(languageManager.getComponent("command.setradius_invalid", args[1]));
            return true;
        }
    }

    private boolean handleSetRemoveMode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(languageManager.getComponent("command.setremovemode_usage"));
            sender.sendMessage(languageManager.getComponent("command.setremovemode_current", configManager.getRemoveMode().name()));
            return true;
        }

        String modeStr = args[1].toUpperCase();
        try {
            ConfigManager.RemoveMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(languageManager.getComponent("command.setremovemode_invalid", args[1]));
            sender.sendMessage(languageManager.getComponent("command.setremovemode_options"));
            return true;
        }

        if (configManager.getConfig() == null) {
            configManager.loadConfig();
        }
        configManager.getConfig().set("removeMode", modeStr);
        configManager.saveConfig();
        
        sender.sendMessage(languageManager.getComponent("command.setremovemode_success", modeStr));

        if (configManager.isDebug()) {
            plugin.getLogger().info("Remove-Modus gesetzt: " + modeStr);
        }

        return true;
    }

    private boolean handleSetLanguage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(languageManager.getComponent("command.setlang_usage"));
            sender.sendMessage(languageManager.getComponent("command.setlang_current", languageManager.getCurrentLanguage()));
            return true;
        }

        String lang = args[1].toLowerCase();
        if (!lang.equals("de") && !lang.equals("en")) {
            sender.sendMessage(languageManager.getComponent("command.setlang_invalid", args[1]));
            sender.sendMessage(languageManager.getComponent("command.setlang_options"));
            return true;
        }

        if (configManager.getConfig() == null) {
            configManager.loadConfig();
        }
        configManager.getConfig().set("lang", lang);
        configManager.saveConfig();
        
        languageManager.loadLanguage();
        
        sender.sendMessage(languageManager.getComponent("command.setlang_success", lang));

        if (configManager.isDebug()) {
            plugin.getLogger().info("Language set to: " + lang);
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        Location spawn = configManager.getSpawnLocation();
        if (spawn == null || spawn.getWorld() == null) {
            sender.sendMessage(languageManager.getComponent("command.info_no_spawn"));
            return true;
        }
        
        int radius = configManager.getRadius();
        String spawnStr = String.format("%.1f, %.1f, %.1f", spawn.getX(), spawn.getY(), spawn.getZ());

        sender.sendMessage(languageManager.getComponent("command.info_header"));
        sender.sendMessage(languageManager.getComponent("command.info_world", spawn.getWorld().getName()));
        sender.sendMessage(languageManager.getComponent("command.info_spawn", spawnStr));
        sender.sendMessage(languageManager.getComponent("command.info_radius", String.valueOf(radius)));
        sender.sendMessage(languageManager.getComponent("command.info_removemode", configManager.getRemoveMode().name()));

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        configManager.loadConfig();
        languageManager.loadLanguage();
        sender.sendMessage(languageManager.getComponent("command.reload_success"));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(languageManager.getComponent("command.give_usage"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(languageManager.getComponent("command.give_player_not_found", args[1]));
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage(languageManager.getComponent("command.give_amount_range"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(languageManager.getComponent("command.give_invalid_amount", args[2]));
                return true;
            }
        }

        ItemStack elytra = elytraTagService.createOneWayElytra(amount);
        target.getInventory().addItem(elytra);

        sender.sendMessage(languageManager.getComponent("command.give_success", target.getName()));

        return true;
    }

    private boolean handleTag(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getComponent("command.player_only"));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.ELYTRA) {
            player.sendMessage(languageManager.getComponent("command.tag_hold_elytra"));
            return true;
        }

        elytraTagService.tagElytra(item);
        player.sendMessage(languageManager.getComponent("command.tag_success"));
        return true;
    }

    private boolean handleUntag(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getComponent("command.player_only"));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.ELYTRA) {
            player.sendMessage(languageManager.getComponent("command.untag_hold_elytra"));
            return true;
        }

        elytraTagService.untagElytra(item);
        player.sendMessage(languageManager.getComponent("command.untag_success"));
        return true;
    }

    private boolean handleCheck(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getComponent("command.player_only"));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.ELYTRA) {
            player.sendMessage(languageManager.getComponent("command.check_hold_elytra"));
            return true;
        }

        boolean isOneWay = elytraTagService.isOneWayElytra(item);
        if (isOneWay) {
            player.sendMessage(languageManager.getComponent("command.check_is_oneway"));
        } else {
            player.sendMessage(languageManager.getComponent("command.check_not_oneway"));
        }

        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        boolean currentDebug = configManager.isDebug();
        boolean newDebug = !currentDebug;
        
        if (configManager.getConfig() == null) {
            configManager.loadConfig();
        }
        configManager.getConfig().set("debug", newDebug);
        configManager.saveConfig();
        
        if (newDebug) {
            sender.sendMessage(languageManager.getComponent("command.debug_enabled"));
        } else {
            sender.sendMessage(languageManager.getComponent("command.debug_disabled"));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(languageManager.getComponent("command.help_header"));
        for (int i = 1; i <= 11; i++) {
            sender.sendMessage(languageManager.getComponent("command.help_" + i));
        }
    }

    private void sendNoPermission(CommandSender sender) {
        sender.sendMessage(languageManager.getComponent("command.no_permission"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("setspawn", "setradius", "setremovemode", "setlang", "info", "reload", "give", "tag", "untag", "check", "debug");
            return subCommands.stream()
                .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("setremovemode")) {
                return Arrays.asList("MOVE_TO_INVENTORY", "DROP").stream()
                    .filter(mode -> mode.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("setlang") || args[0].equalsIgnoreCase("setlanguage")) {
                return Arrays.asList("de", "en").stream()
                    .filter(lang -> lang.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}

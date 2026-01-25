package de.jotibi.onewayelytra.command;

import de.jotibi.onewayelytra.OneWayElytraPlugin;
import de.jotibi.onewayelytra.config.ConfigManager;
import de.jotibi.onewayelytra.service.ElytraTagService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final ElytraTagService elytraTagService;

    public OneWayElytraCommand(OneWayElytraPlugin plugin, ConfigManager configManager,
                               ElytraTagService elytraTagService) {
        this.plugin = plugin;
        this.configManager = configManager;
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
            sender.sendMessage(Component.text("Dieser Befehl kann nur von Spielern ausgeführt werden!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            player.sendMessage(Component.text("Fehler: Welt nicht gefunden!", NamedTextColor.RED));
            return true;
        }
        
        configManager.setSpawnLocation(location);

        player.sendMessage(Component.text("Spawn-Punkt wurde auf deine aktuelle Position gesetzt: ", NamedTextColor.GREEN)
            .append(Component.text(String.format("%s (%.1f, %.1f, %.1f)", 
                location.getWorld().getName(), location.getX(), location.getY(), location.getZ()), 
                NamedTextColor.YELLOW)));

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
            sender.sendMessage(Component.text("Verwendung: /oe setradius <blocks>", NamedTextColor.RED));
            return true;
        }

        try {
            int radius = Integer.parseInt(args[1]);
            if (radius < 0) {
                sender.sendMessage(Component.text("Der Radius muss positiv sein!", NamedTextColor.RED));
                return true;
            }

            configManager.setRadius(radius);
            sender.sendMessage(Component.text("Radius wurde auf ", NamedTextColor.GREEN)
                .append(Component.text(radius + " Blöcke", NamedTextColor.YELLOW))
                .append(Component.text(" gesetzt.")));

            if (configManager.isDebug()) {
                plugin.getLogger().info("Radius gesetzt: " + radius);
            }

            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Ungültige Zahl: " + args[1], NamedTextColor.RED));
            return true;
        }
    }

    private boolean handleSetRemoveMode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Verwendung: /oe setremovemode <MOVE_TO_INVENTORY|DROP>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Aktueller Modus: ", NamedTextColor.GRAY)
                .append(Component.text(configManager.getRemoveMode().name(), NamedTextColor.YELLOW)));
            return true;
        }

        String modeStr = args[1].toUpperCase();
        try {
            // Validiere den Modus
            ConfigManager.RemoveMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Ungültiger Modus: " + args[1], NamedTextColor.RED));
            sender.sendMessage(Component.text("Verfügbare Modi: MOVE_TO_INVENTORY, DROP", NamedTextColor.YELLOW));
            return true;
        }

        // Setze den Modus in der Config
        if (configManager.getConfig() == null) {
            configManager.loadConfig();
        }
        configManager.getConfig().set("removeMode", modeStr);
        configManager.saveConfig();
        
        sender.sendMessage(Component.text("Remove-Modus wurde auf ", NamedTextColor.GREEN)
            .append(Component.text(modeStr, NamedTextColor.YELLOW))
            .append(Component.text(" gesetzt.")));

        if (configManager.isDebug()) {
            plugin.getLogger().info("Remove-Modus gesetzt: " + modeStr);
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
            sender.sendMessage(Component.text("Fehler: Spawn-Punkt nicht konfiguriert!", NamedTextColor.RED));
            return true;
        }
        
        int radius = configManager.getRadius();

        sender.sendMessage(Component.text("=== OneWay Elytra Info ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Welt: ", NamedTextColor.GRAY)
            .append(Component.text(spawn.getWorld().getName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Spawn: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("%.1f, %.1f, %.1f", 
                spawn.getX(), spawn.getY(), spawn.getZ()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Radius: ", NamedTextColor.GRAY)
            .append(Component.text(radius + " Blöcke", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Remove-Modus: ", NamedTextColor.GRAY)
            .append(Component.text(configManager.getRemoveMode().name(), NamedTextColor.WHITE)));

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        configManager.loadConfig();
        sender.sendMessage(Component.text("Config wurde neu geladen!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Verwendung: /oe give <player> [amount]", NamedTextColor.RED));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Spieler nicht gefunden: " + args[1], NamedTextColor.RED));
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage(Component.text("Anzahl muss zwischen 1 und 64 sein!", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Ungültige Anzahl: " + args[2], NamedTextColor.RED));
                return true;
            }
        }

        ItemStack elytra = elytraTagService.createOneWayElytra(amount);
        target.getInventory().addItem(elytra);

        sender.sendMessage(Component.text("OneWay Elytra wurde an ", NamedTextColor.GREEN)
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .append(Component.text(" gegeben.")));

        return true;
    }

    private boolean handleTag(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Dieser Befehl kann nur von Spielern ausgeführt werden!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.ELYTRA) {
            player.sendMessage(Component.text("Du musst eine Elytra in der Hand halten!", NamedTextColor.RED));
            return true;
        }

        elytraTagService.tagElytra(item);
        player.sendMessage(Component.text("Elytra wurde als OneWay Elytra markiert!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleUntag(CommandSender sender) {
        if (!sender.hasPermission("onewayelytra.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Dieser Befehl kann nur von Spielern ausgeführt werden!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.ELYTRA) {
            player.sendMessage(Component.text("Du musst eine Elytra in der Hand halten!", NamedTextColor.RED));
            return true;
        }

        elytraTagService.untagElytra(item);
        player.sendMessage(Component.text("OneWay Elytra Markierung wurde entfernt!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleCheck(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Dieser Befehl kann nur von Spielern ausgeführt werden!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.ELYTRA) {
            player.sendMessage(Component.text("Du musst eine Elytra in der Hand halten!", NamedTextColor.RED));
            return true;
        }

        boolean isOneWay = elytraTagService.isOneWayElytra(item);
        if (isOneWay) {
            player.sendMessage(Component.text("Diese Elytra ist eine OneWay Elytra!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Diese Elytra ist keine OneWay Elytra.", NamedTextColor.GRAY));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== OneWay Elytra Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/oe setspawn - Setzt den Spawn-Punkt", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/oe setradius <blocks> - Setzt den Radius", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/oe setremovemode <MOVE_TO_INVENTORY|DROP> - Setzt den Remove-Modus", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/oe info - Zeigt aktuelle Einstellungen", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/oe reload - Lädt die Config neu", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/oe give <player> [amount] - Gibt OneWay Elytra", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/oe tag - Markiert Elytra in der Hand", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/oe untag - Entfernt Markierung von Elytra", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/oe check - Prüft ob Elytra getaggt ist", NamedTextColor.YELLOW));
    }

    private void sendNoPermission(CommandSender sender) {
        sender.sendMessage(Component.text("Du hast keine Berechtigung für diesen Befehl!", NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("setspawn", "setradius", "setremovemode", "info", "reload", "give", "tag", "untag", "check");
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
        }

        return new ArrayList<>();
    }
}

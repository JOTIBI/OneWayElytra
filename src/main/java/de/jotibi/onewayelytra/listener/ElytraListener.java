package de.jotibi.onewayelytra.listener;

import de.jotibi.onewayelytra.OneWayElytraPlugin;
import de.jotibi.onewayelytra.config.ConfigManager;
import de.jotibi.onewayelytra.service.ElytraTagService;
import de.jotibi.onewayelytra.service.FlightTracker;
import de.jotibi.onewayelytra.service.ZoneService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ElytraListener implements Listener {

    private final OneWayElytraPlugin plugin;
    private final ConfigManager configManager;
    private final ZoneService zoneService;
    private final ElytraTagService elytraTagService;
    private final FlightTracker flightTracker;

    public ElytraListener(OneWayElytraPlugin plugin, ConfigManager configManager,
                          ZoneService zoneService, ElytraTagService elytraTagService,
                          FlightTracker flightTracker) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.zoneService = zoneService;
        this.elytraTagService = elytraTagService;
        this.flightTracker = flightTracker;

        startFlightTrackingTask();
        startInventoryCleanupTask();
    }
    
    private void startInventoryCleanupTask() {
        if (configManager.isDebug()) {
            plugin.getLogger().info("[DEBUG] InventoryCleanupTask wird gestartet (läuft alle 20 Ticks)");
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean debug = configManager.isDebug();
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.hasPermission("onewayelytra.bypass")) {
                        continue;
                    }
                    
                    // Prüfe alle Inventar-Slots (außer Chestplate-Slot) auf OneWay Elytras
                    for (int i = 0; i < player.getInventory().getSize(); i++) {
                        // Überspringe den Chestplate-Slot (38), da dieser separat gehandhabt wird
                        if (i == 38) {
                            continue;
                        }
                        
                        ItemStack item = player.getInventory().getItem(i);
                        if (elytraTagService.isOneWayElytra(item)) {
                            if (debug) {
                                plugin.getLogger().info(String.format("[DEBUG] OneWay Elytra im Inventar von %s gefunden (Slot %d) - lösche", 
                                    player.getName(), i));
                            }
                            player.getInventory().setItem(i, null);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Läuft alle 20 Ticks (1 Sekunde)
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        boolean debug = configManager.isDebug();
        if (debug) {
            plugin.getLogger().info("[DEBUG] ===== EntityToggleGlideEvent ausgelöst =====");
            plugin.getLogger().info("[DEBUG] Entity: " + event.getEntity().getType() + ", isGliding: " + event.isGliding());
        }
        
        if (!(event.getEntity() instanceof Player)) {
            if (debug) {
                plugin.getLogger().info("[DEBUG] Entity ist kein Player, ignoriere Event");
            }
            return;
        }

        Player player = (Player) event.getEntity();
        
        if (debug) {
            plugin.getLogger().info(String.format("[DEBUG] Player: %s, isGliding: %s", 
                player.getName(), event.isGliding()));
        }

        if (player.hasPermission("onewayelytra.bypass")) {
            if (debug) {
                plugin.getLogger().info(String.format("[DEBUG] Player %s hat bypass Permission, ignoriere", 
                    player.getName()));
            }
            return;
        }

        if (!event.isGliding()) {
            // Spieler stoppt Glide
            if (debug) {
                plugin.getLogger().info(String.format("[DEBUG] Player %s stoppt Glide, setze State auf false", 
                    player.getName()));
            }
            flightTracker.setGliding(player.getUniqueId(), false);
            
            // Prüfe sofort beim Stoppen, ob Elytra entfernt werden muss
            ItemStack chestplate = player.getInventory().getChestplate();
            boolean isOneWay = elytraTagService.isOneWayElytra(chestplate);
            if (isOneWay) {
                @SuppressWarnings("deprecation")
                boolean isOnGround = player.isOnGround();
                Location playerLoc = player.getLocation();
                boolean withinRadius = zoneService.isWithinRadius(playerLoc);
                
                if (debug) {
                    plugin.getLogger().info(String.format("[DEBUG] Player %s hat Glide gestoppt - isOnGround: %s, withinRadius: %s", 
                        player.getName(), isOnGround, withinRadius));
                }
                
                if (isOnGround && !withinRadius) {
                    if (debug) {
                        plugin.getLogger().info(String.format("[DEBUG] Player %s - Glide gestoppt UND außerhalb Radius UND on ground - entferne Elytra", 
                            player.getName()));
                    }
                    removeElytra(player, chestplate);
                    String message = configManager.getRemovedAfterLandingMessage();
                    // Entferne Color-Codes und verwende NamedTextColor stattdessen
                    String cleanMessage = message.replace("&c", "").replace("&r", "").replace("&", "");
                    player.sendMessage(Component.text(cleanMessage, NamedTextColor.RED));
                }
            }
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (debug) {
            plugin.getLogger().info(String.format("[DEBUG] Player %s Chestplate: %s (null: %s)", 
                player.getName(), 
                chestplate != null ? chestplate.getType().toString() : "null",
                chestplate == null));
        }
        
        boolean isOneWay = elytraTagService.isOneWayElytra(chestplate);
        if (debug) {
            plugin.getLogger().info(String.format("[DEBUG] Player %s - Ist OneWay Elytra: %s", 
                player.getName(), isOneWay));
        }
        
        if (!isOneWay) {
            if (debug) {
                plugin.getLogger().info(String.format("[DEBUG] Player %s trägt keine OneWay Elytra, ignoriere", 
                    player.getName()));
            }
            return;
        }

        Location playerLoc = player.getLocation();
        boolean withinRadius = zoneService.isWithinRadius(playerLoc);
        
        if (debug) {
            Location spawn = configManager.getSpawnLocation();
            plugin.getLogger().info(String.format("[DEBUG] Player %s Location: %s @ %.1f, %.1f, %.1f", 
                player.getName(), 
                playerLoc.getWorld() != null ? playerLoc.getWorld().getName() : "null",
                playerLoc.getX(), playerLoc.getY(), playerLoc.getZ()));
            if (spawn != null) {
                plugin.getLogger().info(String.format("[DEBUG] Spawn Location: %s @ %.1f, %.1f, %.1f, Radius: %d", 
                    spawn.getWorld() != null ? spawn.getWorld().getName() : "null",
                    spawn.getX(), spawn.getY(), spawn.getZ(), configManager.getRadius()));
            } else {
                plugin.getLogger().warning("[DEBUG] Spawn Location ist null!");
            }
            plugin.getLogger().info(String.format("[DEBUG] Player %s innerhalb Radius: %s", 
                player.getName(), withinRadius));
        }

        if (!withinRadius) {
            event.setCancelled(true);
            
            String message = configManager.getDenyGlideMessage();
            // Entferne Color-Codes und verwende NamedTextColor stattdessen
            String cleanMessage = message.replace("&c", "").replace("&r", "").replace("&", "");
            player.sendActionBar(Component.text(cleanMessage, NamedTextColor.RED));

            if (debug) {
                plugin.getLogger().info(String.format("[DEBUG] Glide-Start blockiert für %s (außerhalb Radius)", 
                    player.getName()));
            }
            return;
        }

        flightTracker.setGliding(player.getUniqueId(), true);

        if (debug) {
            plugin.getLogger().info(String.format("[DEBUG] Glide gestartet für %s (innerhalb Radius), State gesetzt", 
                player.getName()));
        }
    }

    private void startFlightTrackingTask() {
        if (configManager.isDebug()) {
            plugin.getLogger().info("[DEBUG] FlightTrackingTask wird gestartet (läuft alle 2 Ticks)");
        }
        
        new BukkitRunnable() {
            private int tickCount = 0;
            
            @Override
            public void run() {
                tickCount++;
                boolean debug = configManager.isDebug();
                int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
                
                // Nur alle 100 Ticks (5 Sekunden) loggen, um Spam zu vermeiden
                if (debug && tickCount % 100 == 0 && onlinePlayers > 0) {
                    plugin.getLogger().info(String.format("[DEBUG] FlightTrackingTask läuft (Tick %d) - Online Spieler: %d", 
                        tickCount, onlinePlayers));
                }
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    
                    // Prüfe automatische Elytra-Vergabe/Entfernung
                    checkAutoElytraManagement(player, debug, tickCount);
                    
                    boolean wasGliding = flightTracker.wasGliding(uuid);
                    // Nur loggen wenn wasGliding true ist, um Spam zu vermeiden
                    if (debug && wasGliding) {
                        plugin.getLogger().info(String.format("[DEBUG] Player %s - wasGliding: %s", 
                            player.getName(), wasGliding));
                    }
                    
                    if (!wasGliding) {
                        continue;
                    }

                    boolean isGliding = player.isGliding();
                    @SuppressWarnings("deprecation")
                    boolean isOnGround = player.isOnGround();

                    if (configManager.isDebug()) {
                        plugin.getLogger().info(String.format("[DEBUG] Player %s - isGliding: %s, isOnGround: %s", 
                            player.getName(), isGliding, isOnGround));
                    }

                    if (!isGliding && isOnGround) {
                        if (configManager.isDebug()) {
                            plugin.getLogger().info(String.format("[DEBUG] Player %s hat gelandet (nicht mehr gliding, on ground)", 
                                player.getName()));
                        }
                        
                        ItemStack chestplate = player.getInventory().getChestplate();
                        boolean isOneWay = elytraTagService.isOneWayElytra(chestplate);
                        Location playerLoc = player.getLocation();
                        boolean withinRadius = zoneService.isWithinRadius(playerLoc);
                        
                        if (configManager.isDebug()) {
                            plugin.getLogger().info(String.format("[DEBUG] Player %s nach Landung - Chestplate: %s, isOneWay: %s, withinRadius: %s", 
                                player.getName(),
                                chestplate != null ? chestplate.getType().toString() : "null",
                                isOneWay,
                                withinRadius));
                            if (playerLoc != null) {
                                plugin.getLogger().info(String.format("[DEBUG] Player %s Location: %s @ %.1f, %.1f, %.1f", 
                                    player.getName(),
                                    playerLoc.getWorld() != null ? playerLoc.getWorld().getName() : "null",
                                    playerLoc.getX(), playerLoc.getY(), playerLoc.getZ()));
                            }
                        }
                        
                        if (isOneWay && !withinRadius) {
                            if (configManager.isDebug()) {
                                plugin.getLogger().info(String.format("[DEBUG] Player %s - Bedingung erfüllt: OneWay Elytra getragen UND außerhalb Radius - entferne Elytra", 
                                    player.getName()));
                            }
                            
                            removeElytra(player, chestplate);
                            
                            String message = configManager.getRemovedAfterLandingMessage();
                            player.sendMessage(Component.text(message.replace("&", "§"), NamedTextColor.RED));

                            if (configManager.isDebug()) {
                                plugin.getLogger().info(String.format("[DEBUG] OneWay Elytra entfernt von %s (außerhalb Radius nach Landung)", 
                                    player.getName()));
                            }
                        } else {
                            if (configManager.isDebug()) {
                                plugin.getLogger().info(String.format("[DEBUG] Player %s - Elytra wird NICHT entfernt (isOneWay: %s, withinRadius: %s)", 
                                    player.getName(), isOneWay, withinRadius));
                            }
                        }

                        flightTracker.setGliding(uuid, false);
                        if (configManager.isDebug()) {
                            plugin.getLogger().info(String.format("[DEBUG] Player %s - State auf false gesetzt", 
                                player.getName()));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void removeElytra(Player player, ItemStack elytra) {
        if (configManager.isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] removeElytra aufgerufen für %s, elytra: %s", 
                player != null ? player.getName() : "null",
                elytra != null ? elytra.getType().toString() : "null"));
        }
        
        if (elytra == null || player == null) {
            if (configManager.isDebug()) {
                plugin.getLogger().warning(String.format("[DEBUG] removeElytra abgebrochen - elytra null: %s, player null: %s", 
                    elytra == null, player == null));
            }
            return;
        }
        
        if (player.getWorld() == null) {
            if (configManager.isDebug()) {
                plugin.getLogger().warning(String.format("[DEBUG] removeElytra abgebrochen - player.getWorld() ist null für %s", 
                    player.getName()));
            }
            return;
        }
        
        if (configManager.isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] Entferne Chestplate von %s", player.getName()));
        }
        player.getInventory().setChestplate(null);

        ConfigManager.RemoveMode removeMode = configManager.getRemoveMode();
        
        if (configManager.isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] RemoveMode: %s", removeMode));
        }
        
        if (removeMode == ConfigManager.RemoveMode.MOVE_TO_INVENTORY) {
            if (configManager.isDebug()) {
                plugin.getLogger().info(String.format("[DEBUG] Versuche Elytra ins Inventar von %s zu geben", player.getName()));
            }
            java.util.Map<Integer, ItemStack> remainingItems = player.getInventory().addItem(elytra);
            if (!remainingItems.isEmpty()) {
                ItemStack remaining = remainingItems.values().iterator().next();
                if (remaining != null && !remaining.getType().equals(Material.AIR)) {
                    if (configManager.isDebug()) {
                        plugin.getLogger().info(String.format("[DEBUG] Inventar voll, droppe und lösche Elytra bei %s", player.getName()));
                    }
                    // Elytra wird gedroppt, aber sofort gelöscht (nicht aufsammelbar)
                    org.bukkit.entity.Item droppedItem = player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                    if (droppedItem != null) {
                        droppedItem.remove();
                        if (configManager.isDebug()) {
                            plugin.getLogger().info(String.format("[DEBUG] Gedroppte Elytra wurde sofort gelöscht bei %s", player.getName()));
                        }
                    }
                }
            } else {
                if (configManager.isDebug()) {
                    plugin.getLogger().info(String.format("[DEBUG] Elytra erfolgreich ins Inventar von %s gegeben", player.getName()));
                }
            }
        } else {
            if (configManager.isDebug()) {
                plugin.getLogger().info(String.format("[DEBUG] Droppe und lösche Elytra bei %s (DROP Mode)", player.getName()));
            }
            // Elytra wird gedroppt, aber sofort gelöscht (nicht aufsammelbar)
            org.bukkit.entity.Item droppedItem = player.getWorld().dropItemNaturally(player.getLocation(), elytra);
            if (droppedItem != null) {
                droppedItem.remove();
                if (configManager.isDebug()) {
                    plugin.getLogger().info(String.format("[DEBUG] Gedroppte Elytra wurde sofort gelöscht bei %s", player.getName()));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (configManager.isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] Player %s verlässt Server, lösche State", 
                event.getPlayer().getName()));
        }
        flightTracker.clearState(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (configManager.isDebug()) {
            plugin.getLogger().info(String.format("[DEBUG] Player %s wechselt Welt (%s -> %s), lösche State", 
                event.getPlayer().getName(),
                event.getFrom().getName(),
                event.getPlayer().getWorld().getName()));
        }
        flightTracker.clearState(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        
        if (player.hasPermission("onewayelytra.bypass")) {
            return;
        }

        boolean debug = configManager.isDebug();
        
        // Prüfe, ob eine OneWay Elytra aus dem Chestplate-Slot entfernt wird
        // Slot 38 ist der Chestplate-Slot im Player Inventory
        if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 38) {
            ItemStack clickedItem = event.getCurrentItem();
            if (elytraTagService.isOneWayElytra(clickedItem)) {
                // OneWay Elytra wird aus dem Chestplate-Slot entfernt - lösche sie sofort
                if (debug) {
                    plugin.getLogger().info(String.format("[DEBUG] Player %s entfernt OneWay Elytra aus Chestplate-Slot - lösche sofort", 
                        player.getName()));
                }
                
                // Lösche die Elytra sofort - setze den Slot auf null
                event.setCurrentItem(null);
                
                // Verhindere, dass die Elytra auf den Cursor oder ins Inventar verschoben wird
                if (event.getClick().isShiftClick()) {
                    // Shift-Click würde die Elytra ins Inventar verschieben - verhindere das
                    event.setCancelled(true);
                } else if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
                    // Normales Klicken würde die Elytra auf den Cursor legen - verhindere das
                    event.setCancelled(true);
                }
                
                // Prüfe nach dem Event, ob die Elytra trotzdem ins Inventar gelangt ist und lösche sie
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Prüfe alle Inventar-Slots auf OneWay Elytras und lösche sie
                        for (int i = 0; i < player.getInventory().getSize(); i++) {
                            ItemStack item = player.getInventory().getItem(i);
                            if (elytraTagService.isOneWayElytra(item)) {
                                if (debug) {
                                    plugin.getLogger().info(String.format("[DEBUG] OneWay Elytra im Inventar von %s gefunden (Slot %d) - lösche", 
                                        player.getName(), i));
                                }
                                player.getInventory().setItem(i, null);
                            }
                        }
                    }
                }.runTaskLater(plugin, 1L);
                
                if (debug) {
                    plugin.getLogger().info(String.format("[DEBUG] OneWay Elytra von %s wurde gelöscht (aus Slot entfernt)", 
                        player.getName()));
                }
            }
            
            // Prüfe auch, ob eine OneWay Elytra aus dem Inventar in den Chestplate-Slot gelegt wird
            // und ob der Spieler außerhalb des Radius ist
            ItemStack cursorItem = event.getCursor();
            if (elytraTagService.isOneWayElytra(cursorItem)) {
                Location playerLoc = player.getLocation();
                boolean withinRadius = zoneService.isWithinRadius(playerLoc);
                
                if (!withinRadius) {
                    // Spieler versucht OneWay Elytra außerhalb des Radius anzuziehen - verhindere das
                    if (debug) {
                        plugin.getLogger().info(String.format("[DEBUG] Player %s versucht OneWay Elytra außerhalb Radius anzuziehen - blockiere", 
                            player.getName()));
                    }
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Du kannst die OneWay Elytra hier nicht anziehen!", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        
        if (player.hasPermission("onewayelytra.bypass")) {
            return;
        }

        boolean debug = configManager.isDebug();
        
        // Prüfe, ob eine OneWay Elytra per Drag & Drop in den Chestplate-Slot verschoben wird
        ItemStack draggedItem = event.getOldCursor();
        if (elytraTagService.isOneWayElytra(draggedItem)) {
            // Prüfe, ob einer der Slots der Chestplate-Slot (38) ist
            if (event.getRawSlots().contains(38)) {
                Location playerLoc = player.getLocation();
                boolean withinRadius = zoneService.isWithinRadius(playerLoc);
                
                if (!withinRadius) {
                    // Spieler versucht OneWay Elytra außerhalb des Radius anzuziehen - verhindere das
                    if (debug) {
                        plugin.getLogger().info(String.format("[DEBUG] Player %s versucht OneWay Elytra per Drag außerhalb Radius anzuziehen - blockiere", 
                            player.getName()));
                    }
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Du kannst die OneWay Elytra hier nicht anziehen!", NamedTextColor.RED));
                }
            }
        }
        
        // Prüfe, ob eine OneWay Elytra aus dem Chestplate-Slot per Drag entfernt wird
        // Prüfe, ob Slot 38 (Chestplate) betroffen ist und ob dort eine OneWay Elytra war
        if (event.getRawSlots().contains(38)) {
            // Prüfe im nächsten Tick, ob eine OneWay Elytra aus dem Slot entfernt wurde
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack chestplate = player.getInventory().getChestplate();
                    // Wenn keine OneWay Elytra mehr im Slot ist, aber vorher eine da war
                    // dann wurde sie entfernt - das wird durch ItemSpawnEvent/ItemDropEvent gehandhabt
                    if (!elytraTagService.isOneWayElytra(chestplate)) {
                        // Prüfe, ob die gedraggte Item eine OneWay Elytra ist
                        if (elytraTagService.isOneWayElytra(draggedItem)) {
                            if (debug) {
                                plugin.getLogger().info(String.format("[DEBUG] Player %s entfernt OneWay Elytra per Drag aus Chestplate-Slot - wird durch ItemSpawnEvent gelöscht", 
                                    player.getName()));
                            }
                        }
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission("onewayelytra.bypass")) {
            return;
        }

        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        if (elytraTagService.isOneWayElytra(droppedItem)) {
            boolean debug = configManager.isDebug();
            if (debug) {
                plugin.getLogger().info(String.format("[DEBUG] Player %s droppt OneWay Elytra - lösche sofort", 
                    player.getName()));
            }
            
            // Lösche die gedroppte Elytra sofort
            event.getItemDrop().remove();
            
            if (debug) {
                plugin.getLogger().info(String.format("[DEBUG] Gedroppte OneWay Elytra von %s wurde gelöscht", 
                    player.getName()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSpawn(ItemSpawnEvent event) {
        // Prüfe, ob ein OneWay Elytra in die Welt gespawnt wird und lösche es sofort
        ItemStack itemStack = event.getEntity().getItemStack();
        
        if (elytraTagService.isOneWayElytra(itemStack)) {
            boolean debug = configManager.isDebug();
            if (debug) {
                plugin.getLogger().info(String.format("[DEBUG] OneWay Elytra wurde in die Welt gespawnt @ %s - lösche sofort", 
                    event.getLocation().toString()));
            }
            
            // Lösche die gespawnte Elytra sofort
            event.setCancelled(true);
            event.getEntity().remove();
            
            if (debug) {
                plugin.getLogger().info(String.format("[DEBUG] Gespawnte OneWay Elytra wurde gelöscht", 
                    event.getLocation().toString()));
            }
        }
    }
    
    /**
     * Prüft automatisch, ob Spieler im Radius sind und gibt/entfernt Elytra entsprechend
     */
    private void checkAutoElytraManagement(Player player, boolean debug, int tickCount) {
        if (player.hasPermission("onewayelytra.bypass")) {
            return;
        }
        
        Location playerLoc = player.getLocation();
        boolean withinRadius = zoneService.isWithinRadius(playerLoc);
        ItemStack chestplate = player.getInventory().getChestplate();
        boolean hasOneWayElytra = elytraTagService.isOneWayElytra(chestplate);
        boolean isGliding = player.isGliding();
        
        if (debug && tickCount % 20 == 0) { // Alle 20 Ticks (1 Sekunde) loggen
            plugin.getLogger().info(String.format("[DEBUG] Auto-Management für %s - withinRadius: %s, hasOneWayElytra: %s, isGliding: %s", 
                player.getName(), withinRadius, hasOneWayElytra, isGliding));
        }
        
        if (withinRadius) {
            // Spieler ist im Radius
            if (!hasOneWayElytra && !isGliding) {
                // Spieler hat keine OneWay Elytra und gleitet nicht -> gebe Elytra
                if (debug) {
                    plugin.getLogger().info(String.format("[DEBUG] ===== Player %s ist im Radius ohne OneWay Elytra - gebe Elytra =====", 
                        player.getName()));
                }
                
                ItemStack elytra = elytraTagService.createOneWayElytra(1);
                
                // Prüfe ob Chest-Slot frei ist
                if (chestplate == null || chestplate.getType() == Material.AIR) {
                    player.getInventory().setChestplate(elytra);
                    if (debug) {
                        plugin.getLogger().info(String.format("[DEBUG] ✓ OneWay Elytra automatisch an %s gegeben (Chest-Slot)", 
                            player.getName()));
                    }
                } else {
                    // Chest-Slot ist belegt, versuche ins Inventar zu geben
                    if (debug) {
                        plugin.getLogger().info(String.format("[DEBUG] Chest-Slot belegt (%s), versuche ins Inventar zu geben", 
                            chestplate.getType().toString()));
                    }
                    java.util.Map<Integer, ItemStack> remainingItems = player.getInventory().addItem(elytra);
                    if (remainingItems.isEmpty()) {
                        if (debug) {
                            plugin.getLogger().info(String.format("[DEBUG] ✓ OneWay Elytra automatisch ins Inventar von %s gegeben", 
                                player.getName()));
                        }
                    } else {
                        // Inventar voll, droppe Elytra und lösche sie sofort (nicht aufsammelbar)
                        org.bukkit.entity.Item droppedItem = player.getWorld().dropItemNaturally(player.getLocation(), elytra);
                        if (droppedItem != null) {
                            droppedItem.remove();
                            if (debug) {
                                plugin.getLogger().info(String.format("[DEBUG] ✓ OneWay Elytra bei %s gedroppt und sofort gelöscht (Inventar voll)", 
                                    player.getName()));
                            }
                        }
                    }
                }
            } else {
                if (debug && tickCount % 20 == 0) {
                    plugin.getLogger().info(String.format("[DEBUG] Player %s im Radius - hat bereits Elytra oder gleitet (hasOneWayElytra: %s, isGliding: %s)", 
                        player.getName(), hasOneWayElytra, isGliding));
                }
            }
        } else {
            // Spieler ist außerhalb des Radius
            // WICHTIG: Nur entfernen, wenn Spieler wirklich außerhalb ist UND nicht gleitet
            // NICHT entfernen, wenn Spieler gerade die Elytra anzieht (wird durch FlightTrackingTask gehandhabt)
            if (hasOneWayElytra && !isGliding) {
                // Prüfe zusätzlich, ob der Spieler gerade am Boden ist (nicht in der Luft)
                @SuppressWarnings("deprecation")
                boolean isOnGround = player.isOnGround();
                
                // Nur entfernen, wenn Spieler wirklich am Boden ist (nicht beim Fallen/Springen)
                if (isOnGround) {
                    if (debug) {
                        plugin.getLogger().info(String.format("[DEBUG] Player %s ist außerhalb Radius mit OneWay Elytra (nicht gleitend, on ground) - entferne Elytra", 
                            player.getName()));
                    }
                    removeElytra(player, chestplate);
                } else {
                    if (debug && tickCount % 20 == 0) {
                        plugin.getLogger().info(String.format("[DEBUG] Player %s außerhalb Radius mit OneWay Elytra, aber nicht on ground - warte auf Landung", 
                            player.getName()));
                    }
                }
            }
        }
    }
}

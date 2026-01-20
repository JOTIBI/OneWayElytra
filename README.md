# OneWay Elytra

A Minecraft plugin for Paper/Spigot 1.21.1 that implements a special Elytra system with radius-based restrictions.

---

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/I3I61SOC0C)

## What does this plugin do?

This plugin creates a "OneWay Elytra" system where certain Elytras can only be used within a configurable radius around a spawn point. Normal Elytras work as usual and are not affected.

## Features

- **Radius-based Elytra usage**: OneWay Elytras can only be worn within a defined radius around the spawn point
- **Automatic Elytra management**: 
  - Players automatically receive a OneWay Elytra when entering the radius
  - The Elytra is automatically removed when players leave the radius (only when not gliding)
- **Abuse prevention**: 
  - New gliding cannot be started outside the radius
  - Players already flying can continue their flight until landing
- **Landing detection**: When a player lands outside the radius, the Elytra is automatically removed
- **Flexible removal modes**: 
  - `MOVE_TO_INVENTORY`: Elytra is moved to inventory (or dropped if full)
  - `DROP`: Elytra is immediately deleted (cannot be picked up)

## Installation

1. Download the `OneWayElytra-1.0.0.jar` file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the spawn point and radius (see below)

## Configuration

The configuration file is located at `plugins/OneWayElytra/config.yml`:

```yaml
# Spawn point (World + coordinates)
spawn:
  world: "world"
  x: 0.0
  y: 64.0
  z: 0.0

# Radius in blocks
radius: 100

# Removal mode: MOVE_TO_INVENTORY or DROP
removeMode: "MOVE_TO_INVENTORY"

# Messages
messages:
  denyGlide: "&cYou cannot glide here with the OneWay Elytra!"
  removedAfterLanding: "&cThe OneWay Elytra was removed because you landed outside the allowed area."

# Debug mode (shows additional information)
debug: false
```

## Commands

### `/oe setspawn`
Sets the spawn point to your current position.

### `/oe setradius <blocks>`
Sets the radius in blocks (e.g., `/oe setradius 50`).

### `/oe setremovemode <MOVE_TO_INVENTORY|DROP>`
Changes the removal mode:
- `MOVE_TO_INVENTORY`: Elytra is moved to inventory
- `DROP`: Elytra is deleted

### `/oe info`
Shows the current configuration (spawn point, radius, removal mode).

### `/oe reload`
Reloads the configuration file.

### `/oe give <player> [amount]`
Gives a player one or multiple OneWay Elytras.

### `/oe tag`
Marks the Elytra in your hand as a OneWay Elytra.

### `/oe untag`
Removes the mark from the Elytra in your hand.

### `/oe check`
Checks if the Elytra in your hand is a OneWay Elytra.

**Aliases**: `/onewayelytra`, `/oneway`

## Permissions

- `onewayelytra.admin` - Allows all admin commands (default: OP)
- `onewayelytra.bypass` - Ignores all OneWay Elytra rules (default: OP)

## How it works

1. **Elytra marking**: OneWay Elytras are marked via PersistentDataContainer (PDC) with a special tag
2. **Automatic distribution**: When a player enters the defined radius without a OneWay Elytra, they automatically receive one
3. **Glide protection**: When starting a glide, the plugin checks if the player is within the radius. Outside the radius, the start is blocked
4. **Landing detection**: A task checks every 2 ticks if players have landed and removes the Elytra if they're outside the radius

## Technical Details

- **API Version**: 1.21.1
- **Dependencies**: None (Paper API only)
- **Java Version**: 21
- **Marking**: PersistentDataContainer with NamespacedKey `onewayelytra:oneway`

## Troubleshooting

**Problem**: Elytra is not automatically given/removed
- Check if the spawn point is correctly set (`/oe info`)
- Check if you're within the correct radius
- Enable debug mode in the config (`debug: true`) and check the console

**Problem**: Special characters are displayed incorrectly
- Make sure the config file is saved as UTF-8
- The build file is already configured for UTF-8

## License

This plugin was developed for private use.


package de.example.chunkloader;

import de.example.chunkloader.ChunkLoaderPlugin.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ChunkCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "[ChunkLoader] ";
    private final ChunkLoaderPlugin plugin;

    public ChunkCommand(ChunkLoaderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "Dieser Befehl geht nur im Spiel.");
            return true;
        }
        if (args.length == 0) return false;

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "load", "unload" -> {
                int maxRadius = plugin.getConfig().getInt("max-radius", 2);
                int radius = 0;
                if (args.length >= 2) {
                    try {
                        radius = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(PREFIX + "Der Radius muss eine Zahl sein.");
                        return true;
                    }
                    if (radius < 0 || radius > maxRadius) {
                        player.sendMessage(PREFIX + "Radius muss zwischen 0 und " + maxRadius + " liegen.");
                        return true;
                    }
                }
                handle(player, sub.equals("load"), radius);
            }
            case "list" -> list(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handle(Player player, boolean load, int radius) {
        Chunk center = player.getLocation().getChunk();
        String world = center.getWorld().getName();
        int max = plugin.getConfig().getInt("max-chunks", 50);

        int changed = 0;
        int skipped = 0;
        boolean limitHit = false;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkKey key = new ChunkKey(world, center.getX() + dx, center.getZ() + dz);
                if (load) {
                    if (plugin.loadedChunks().contains(key)) {
                        skipped++;
                    } else if (plugin.loadedChunks().size() >= max) {
                        limitHit = true;
                    } else if (plugin.load(key)) {
                        changed++;
                    }
                } else {
                    if (plugin.unload(key)) changed++;
                    else skipped++;
                }
            }
        }
        plugin.saveData();

        if (load) {
            player.sendMessage(PREFIX + changed + " Chunk(s) werden jetzt dauerhaft geladen"
                    + (skipped > 0 ? " (" + skipped + " waren schon geladen)" : "") + ".");
            if (limitHit) player.sendMessage(PREFIX + "Limit von " + max + " Chunks erreicht.");
        } else {
            player.sendMessage(PREFIX + changed + " Chunk(s) werden nicht mehr dauerhaft geladen"
                    + (skipped > 0 ? " (" + skipped + " waren nicht dauerhaft geladen)" : "") + ".");
        }
    }

    private void list(Player player) {
        String world = player.getWorld().getName();
        List<ChunkKey> here = plugin.loadedChunks().stream()
                .filter(k -> k.world().equals(world)).toList();
        player.sendMessage(PREFIX + plugin.loadedChunks().size() + " Chunk(s) insgesamt, "
                + here.size() + " in dieser Welt:");
        for (ChunkKey k : here) {
            player.sendMessage(" - Chunk " + k.x() + ", " + k.z()
                    + " (Block " + (k.x() * 16) + ", " + (k.z() * 16) + ")");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("load", "unload", "list");
        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) return List.of("0", "1", "2");
        return List.of();
    }
}

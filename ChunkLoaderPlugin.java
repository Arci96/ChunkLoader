package de.example.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChunkLoaderPlugin extends JavaPlugin implements Listener {

    /** Ein dauerhaft geladener Chunk. */
    public record ChunkKey(String world, int x, int z) {
        String serialize() {
            return world + ";" + x + ";" + z;
        }

        static ChunkKey parse(String s) {
            String[] p = s.split(";");
            return new ChunkKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]));
        }
    }

    private final Set<ChunkKey> loaded = new LinkedHashSet<>();
    private File dataFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "data.yml");
        readData();

        ChunkCommand cmd = new ChunkCommand(this);
        getCommand("chunk").setExecutor(cmd);
        getCommand("chunk").setTabCompleter(cmd);
        getServer().getPluginManager().registerEvents(this, this);

        // Nach dem Serverstart die gespeicherten Chunks wieder laden
        getServer().getScheduler().runTask(this, () -> {
            for (World w : getServer().getWorlds()) applyWorld(w);
        });
    }

    @Override
    public void onDisable() {
        saveData();
        // Plugin-Tickets werden beim Deaktivieren automatisch entfernt.
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        applyWorld(event.getWorld());
    }

    private void applyWorld(World world) {
        int count = 0;
        for (ChunkKey key : loaded) {
            if (key.world().equals(world.getName())) {
                world.addPluginChunkTicket(key.x(), key.z(), this);
                count++;
            }
        }
        if (count > 0) getLogger().info(count + " Chunk(s) in Welt '" + world.getName() + "' geladen.");
    }

    // ---- API fuer den Befehl ----

    public Set<ChunkKey> loadedChunks() {
        return loaded;
    }

    public boolean load(ChunkKey key) {
        World w = Bukkit.getWorld(key.world());
        if (w == null || loaded.contains(key)) return false;
        w.addPluginChunkTicket(key.x(), key.z(), this);
        loaded.add(key);
        return true;
    }

    public boolean unload(ChunkKey key) {
        if (!loaded.remove(key)) return false;
        World w = Bukkit.getWorld(key.world());
        if (w != null) w.removePluginChunkTicket(key.x(), key.z(), this);
        return true;
    }

    // ---- Speichern ----

    private void readData() {
        loaded.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        for (String s : yaml.getStringList("chunks")) {
            try {
                loaded.add(ChunkKey.parse(s));
            } catch (Exception e) {
                getLogger().warning("Ungueltiger Eintrag in data.yml: " + s);
            }
        }
    }

    public void saveData() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (ChunkKey k : loaded) list.add(k.serialize());
        yaml.set("chunks", list);
        try {
            getDataFolder().mkdirs();
            yaml.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("data.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}

package ru.mitriyf.rainbreaktorch.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.service.TorchService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Getter
@Setter
public class ChunkData {
    private final Map<Integer, Map<Integer, Column>> locations = new ConcurrentHashMap<>();
    private final String fileName, worldName;
    private final TorchService torchService;
    private final BukkitScheduler scheduler;
    private final RainBreakTorch plugin;
    private final int defaultLifeTime;
    private final Logger logger;
    private final File file;
    private int lifeTime;
    private boolean changed, active;
    private AtomicInteger queue = new AtomicInteger(0);

    public ChunkData(TorchService torchService, String worldName, File file, String fileName) {
        this.torchService = torchService;
        this.file = file;
        this.fileName = fileName;
        this.worldName = worldName;
        plugin = torchService.getPlugin();
        logger = plugin.getLogger();
        scheduler = plugin.getServer().getScheduler();
        defaultLifeTime = plugin.getValues().getObjectRemove();
        if (file.exists()) {
            read();
        }
        startTask();
    }

    private void startTask() {
        lifeTime = defaultLifeTime + 1;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (active) {
                    if (lifeTime <= 0) {
                        save(true);
                        changed = false;
                        torchService.getChunkData().remove(fileName);
                        cancel();
                    }
                    lifeTime--;
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20);
    }

    public void save(boolean async) {
        synchronized (this) {
            if (!changed) {
                return;
            }
            Iterator<Map<Integer, Column>> zMapIterator = locations.values().iterator();
            while (zMapIterator.hasNext()) {
                Map<Integer, Column> zMap = zMapIterator.next();
                zMap.values().removeIf(Column::isEmpty);
                if (zMap.isEmpty()) {
                    zMapIterator.remove();
                }
            }
            if (locations.isEmpty()) {
                torchService.getWorldChunks().get(worldName).remove(fileName);
                removeFile(file);
            } else {
                Map<Integer, Map<Integer, Column>> snapshot = deepCopy(locations);
                if (async) {
                    scheduler.runTaskAsynchronously(plugin, () -> write(snapshot));
                } else {
                    write(snapshot);
                }
            }
            changed = false;
        }
    }

    private void write(Map<Integer, Map<Integer, Column>> snapshot) {
        Set<String> worldChunks = torchService.getWorldChunks().get(worldName);
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            out.writeInt(snapshot.size());
            for (Map.Entry<Integer, Map<Integer, Column>> xEntry : snapshot.entrySet()) {
                out.writeInt(xEntry.getKey());
                out.writeInt(xEntry.getValue().size());
                for (Map.Entry<Integer, Column> zEntry : xEntry.getValue().entrySet()) {
                    out.writeInt(zEntry.getKey());
                    Column col = zEntry.getValue();
                    out.writeInt(col.blocks.size());
                    for (int yBlock : col.blocks) {
                        out.writeInt(yBlock);
                    }
                    out.writeInt(col.torches.size());
                    for (Map.Entry<Integer, Boolean> torch : col.torches.entrySet()) {
                        out.writeInt(torch.getKey());
                        out.writeBoolean(torch.getValue());
                    }
                }
            }
            out.flush();
            worldChunks.add(fileName);
        } catch (Exception e) {
            worldChunks.remove(fileName);
            File world = torchService.getWorldFiles().get(worldName);
            if (!world.exists() && world.mkdirs()) {
                write(snapshot);
            } else {
                logger.warning("File " + file.getName() + " save error: " + e);
            }
        }
    }

    private void read() {
        if (file.length() == 0) {
            removeFile(file);
            return;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())))) {
            int xSize = in.readInt();
            for (int i = 0; i < xSize; i++) {
                int x = in.readInt();
                int zSize = in.readInt();
                Map<Integer, Column> zMap = new ConcurrentHashMap<>();
                for (int j = 0; j < zSize; j++) {
                    int z = in.readInt();
                    Column col = new Column();
                    int bSize = in.readInt();
                    for (int k = 0; k < bSize; k++) {
                        col.blocks.add(in.readInt());
                    }
                    int tSize = in.readInt();
                    for (int k = 0; k < tSize; k++) {
                        col.torches.put(in.readInt(), in.readBoolean());
                    }
                    zMap.put(z, col);
                }
                locations.put(x, zMap);
            }
        } catch (EOFException e) {
            logger.warning("File " + file.getName() + " is break. Reset file.");
            locations.clear();
            removeFile(file);
        } catch (Exception e) {
            logger.warning("File " + file.getName() + " load error: " + e);
        }
    }

    private Map<Integer, Map<Integer, Column>> deepCopy(Map<Integer, Map<Integer, Column>> orig) {
        Map<Integer, Map<Integer, Column>> copy = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, Map<Integer, Column>> xEntry : orig.entrySet()) {
            Map<Integer, Column> zMap = new ConcurrentHashMap<>();
            for (Map.Entry<Integer, Column> zEntry : xEntry.getValue().entrySet()) {
                zMap.put(zEntry.getKey(), zEntry.getValue().cloneColumn());
            }
            copy.put(xEntry.getKey(), zMap);
        }
        return copy;
    }

    private void removeFile(File file) {
        try {
            Files.delete(Paths.get(file.getPath()));
        } catch (Exception ignored) {
        }
    }
}
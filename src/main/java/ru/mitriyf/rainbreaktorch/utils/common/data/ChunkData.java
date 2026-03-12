package ru.mitriyf.rainbreaktorch.utils.common.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.common.TorchUtils;
import ru.mitriyf.rainbreaktorch.utils.common.data.column.Column;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Getter
@Setter
public class ChunkData {
    private final Map<Integer, Map<Integer, Column>> locations = new ConcurrentHashMap<>();
    private final RainBreakTorch plugin;
    private final TorchUtils torchUtils;
    private final int defaultLifeTime;
    private final String fileName;
    private final Logger logger;
    private final File file;
    private int lifeTime;
    private boolean changed, active;
    private AtomicInteger queue = new AtomicInteger(0);

    public ChunkData(TorchUtils torchUtils, File file, String fileName) {
        this.torchUtils = torchUtils;
        this.file = file;
        this.fileName = fileName;
        plugin = torchUtils.getPlugin();
        logger = plugin.getLogger();
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
                        torchUtils.getChunkData().remove(fileName);
                        cancel();
                    }
                    lifeTime--;
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void save(boolean async) {
        synchronized (this) {
            if (!changed) {
                return;
            }
            locations.values().forEach(zMap -> zMap.values().removeIf(Column::isEmpty));
            locations.values().removeIf(Map::isEmpty);
            if (locations.isEmpty()) {
                try {
                    Files.delete(Paths.get(file.getPath()));
                } catch (Exception ignored) {
                }
            } else {
                Map<Integer, Map<Integer, Column>> snapshot = deepCopy(locations);
                if (async) {
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(snapshot));
                } else {
                    write(snapshot);
                }
            }
            changed = false;
        }
    }

    private void write(Map<Integer, Map<Integer, Column>> snapshot) {
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
        } catch (Exception e) {
            logger.warning("File " + file.getName() + " save error: " + e);
        }
    }

    private void read() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())))) {
            int xSize = in.readInt();
            for (int i = 0; i < xSize; i++) {
                int x = in.readInt();
                int zSize = in.readInt();
                Map<Integer, Column> zMap = new HashMap<>();
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
        } catch (Exception e) {
            logger.warning("File " + file.getName() + " load error: " + e);
        }
    }

    private Map<Integer, Map<Integer, Column>> deepCopy(Map<Integer, Map<Integer, Column>> orig) {
        Map<Integer, Map<Integer, Column>> copy = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Column>> xEntry : orig.entrySet()) {
            Map<Integer, Column> zMap = new HashMap<>();
            for (Map.Entry<Integer, Column> zEntry : xEntry.getValue().entrySet()) {
                zMap.put(zEntry.getKey(), zEntry.getValue().cloneColumn());
            }
            copy.put(xEntry.getKey(), zMap);
        }
        return copy;
    }
}
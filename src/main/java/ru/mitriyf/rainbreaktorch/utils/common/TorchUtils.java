package ru.mitriyf.rainbreaktorch.utils.common;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.common.data.ChunkData;
import ru.mitriyf.rainbreaktorch.utils.common.data.column.Column;
import ru.mitriyf.rainbreaktorch.utils.tasks.DropTask;
import ru.mitriyf.rainbreaktorch.utils.tasks.data.DropData;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TorchUtils {
    @Getter
    private final Map<String, Set<String>> worldChunks = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, ChunkData> chunkData = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, File> worldFiles = new ConcurrentHashMap<>();
    private final BukkitScheduler scheduler;
    @Getter
    private final RainBreakTorch plugin;
    private final Values values;
    private final Utils utils;
    @Getter
    private final File worlds;
    private final int minValue;

    public TorchUtils(Utils utils, RainBreakTorch plugin) {
        this.utils = utils;
        this.plugin = plugin;
        values = plugin.getValues();
        minValue = Integer.MIN_VALUE;
        scheduler = plugin.getServer().getScheduler();
        worlds = values.getWorldsFile();
        for (World world : plugin.getServer().getWorlds()) {
            String worldName = world.getName();
            File worldChunksFile = new File(worlds, worldName);
            if (!worldChunksFile.exists() && !worldChunksFile.mkdirs()) {
                plugin.getLogger().warning("Error create mkdirs!");
            }
            worldFiles.put(worldName, worldChunksFile);
            Set<String> chunks = ConcurrentHashMap.newKeySet();
            File[] files = worldChunksFile.listFiles();
            if (files != null) {
                for (File fileName : files) {
                    chunks.add(fileName.getName());
                }
            }
            worldChunks.put(worldName, chunks);
            updateChunksAsync(world, world.hasStorm());
        }
    }

    private ChunkData getChunkData(String worldName, File chunkFile, String fileName) {
        ChunkData chunkDataValue = chunkData.computeIfAbsent(fileName, k -> new ChunkData(this, worldName, chunkFile, fileName));
        chunkDataValue.getQueue().incrementAndGet();
        chunkDataValue.setActive(false);
        return chunkDataValue;
    }

    public void checkTorch(World world, Chunk chunk, ChunkSnapshot snapshot, int x, int y, int z, Material material, boolean isStorm, boolean remove) {
        boolean isTorch = values.getTorchesBlocks().contains(material);
        boolean isCheckHeight = false;
        int chunkX, chunkZ;
        if (snapshot != null) {
            chunkX = snapshot.getX();
            chunkZ = snapshot.getZ();
        } else {
            chunkX = chunk.getX();
            chunkZ = chunk.getZ();
        }
        int checkHeight = 0;
        if (!remove && isStorm) {
            if (!isTorch) {
                return;
            }
            isCheckHeight = true;
            snapshot = snapshot != null ? snapshot : chunk.getChunkSnapshot(true, true, false);
            checkHeight = checkHeight(world, snapshot, null, false, x, y, z);
            if (checkHeight == y) {
                dropItems(world, chunkX * 16 + x, y, chunkZ * 16 + z);
                return;
            }
        }
        String fileName = chunkX + "_" + chunkZ + ".bin";
        String worldName = world.getName();
        File chunkFile = new File(worldFiles.get(worldName), fileName);
        if (remove && chunkData.get(fileName) == null && !worldChunks.get(worldName).contains(fileName)) {
            return;
        }
        ChunkData chunkDataValue = getChunkData(worldName, chunkFile, fileName);
        synchronized (chunkDataValue) {
            processChunkData(chunkDataValue, world, chunk, snapshot, chunkX, chunkZ, x, y, z, material, isStorm, remove, isTorch, isCheckHeight, checkHeight);
        }
    }

    private void processChunkData(ChunkData chunkDataValue, World world, Chunk chunk, ChunkSnapshot snapshot, int chunkX, int chunkZ, int x, int y, int z, Material material, boolean isStorm, boolean remove, boolean isTorch, boolean isCheckHeight, int checkHeight) {
        boolean changed, isFirst = true;
        boolean checkRemoveBlock = !isTorch && remove;
        Map<Integer, Map<Integer, Column>> locations = chunkDataValue.getLocations();
        Map<Integer, Column> xSection = getXSection(locations, x, checkRemoveBlock);
        Column zSection = xSection != null ? getZSection(xSection, z, checkRemoveBlock) : null;
        if (zSection == null) {
            exit(chunkDataValue, false);
            return;
        }
        Set<Integer> blocksSection = zSection.blocks;
        Map<Integer, Boolean> torchesSection = zSection.torches;
        if (remove) {
            if (isBadRemove(chunkDataValue, blocksSection, torchesSection, y)) {
                return;
            }
            torchesSection.remove(y);
            changed = true;
            snapshot = snapshot != null ? snapshot : chunk.getChunkSnapshot(true, true, false);
            checkHeight(world, snapshot, blocksSection, true, x, y, z);
        } else {
            changed = true;
            isFirst = false;
            if (isTorch) {
                if (torchesSection.get(y) == null) {
                    torchesSection.put(y, false);
                    if (isCheckHeight) {
                        check(blocksSection, checkHeight);
                    } else {
                        snapshot = snapshot != null ? snapshot : chunk.getChunkSnapshot(true, true, false);
                        checkHeight(world, snapshot, blocksSection, false, x, y, z);
                    }
                    isFirst = true;
                    changed = false;
                }
            } else if (values.getType().isValid(material)) {
                if (isBadSafeBlock(chunkDataValue, blocksSection, torchesSection, y)) {
                    return;
                }
            }
        }
        exit(chunkDataValue, processTorchesY(world, locations, xSection, torchesSection, blocksSection, x, z, chunkX * 16 + x, chunkZ * 16 + z, isStorm, changed, isFirst));
    }

    private boolean isBadRemove(ChunkData chunkDataValue, Set<Integer> blocksSection, Map<Integer, Boolean> torchesSection, int y) {
        if (torchesSection.isEmpty()) {
            exit(chunkDataValue, false);
            return true;
        }
        blocksSection.remove(y);
        if (torchesSection.get(y) == null && !blocksSection.isEmpty()) {
            exit(chunkDataValue, false);
            return true;
        }
        return false;
    }

    private boolean isBadSafeBlock(ChunkData chunkDataValue, Set<Integer> blocksSection, Map<Integer, Boolean> torchesSection, int y) {
        if (torchesSection.isEmpty()) {
            exit(chunkDataValue, false);
            return true;
        }
        if (blocksSection.isEmpty()) {
            check(blocksSection, y);
        } else {
            int max = getYMax(blocksSection);
            if (max < y) {
                blocksSection.remove(max);
                check(blocksSection, y);
            } else {
                exit(chunkDataValue, false);
                return true;
            }
        }
        return false;
    }

    private void updateValues(World world, ChunkData chunkDataValue, int realChunkX, int realChunkZ, boolean isStorm) {
        Map<Integer, Map<Integer, Column>> locations = chunkDataValue.getLocations();
        if (locations.isEmpty()) {
            exit(chunkDataValue, false);
            return;
        }
        boolean changed = false;
        for (Integer x : locations.keySet()) {
            Map<Integer, Column> xSection = locations.get(x);
            if (xSection == null) {
                continue;
            }
            for (Integer z : xSection.keySet()) {
                Column zSection = xSection.get(z);
                if (zSection == null) {
                    continue;
                }
                Set<Integer> blocksSection = zSection.blocks;
                Map<Integer, Boolean> torchesSection = zSection.torches;
                changed = processTorchesY(world, locations, xSection, torchesSection, blocksSection, x, z, realChunkX + x, realChunkZ + z, isStorm, changed, false);
            }
        }
        exit(chunkDataValue, changed);
    }

    private boolean processTorchesY(World world, Map<Integer, Map<Integer, Column>> locations, Map<Integer, Column> xSection, Map<Integer, Boolean> torchesSection, Set<Integer> blocksSection, int x, int z, int realX, int realZ, boolean isStorm, boolean changed, boolean isFirst) {
        Set<Integer> torchesSet = torchesSection.keySet();
        if (torchesSet.isEmpty()) {
            clearSections(locations, xSection, x, z);
            return changed || !isFirst;
        }
        int yMax = getYMax(blocksSection);
        boolean yMaxClear = true;
        for (Map.Entry<Integer, Boolean> entry : torchesSection.entrySet()) {
            int torch = entry.getKey();
            boolean hasBlock = yMax > torch;
            boolean torchBoolean = torchesSection.getOrDefault(torch, false);
            if (hasBlock) {
                if (!torchBoolean) {
                    changed = true;
                }
                torchesSection.put(torch, true);
                torchBoolean = true;
                yMaxClear = false;
            } else {
                if (torchBoolean) {
                    changed = true;
                    torchesSection.put(torch, false);
                    torchBoolean = false;
                }
            }
            if (!torchBoolean) {
                if (isStorm) {
                    if (!isFirst) {
                        changed = true;
                    }
                    torchesSection.remove(torch);
                    dropItems(world, realX, torch, realZ);
                } else {
                    changed = true;
                }
                if (torchesSet.isEmpty()) {
                    clearSections(locations, xSection, x, z);
                }
            }
        }
        if (yMaxClear) {
            blocksSection.remove(yMax);
        }
        return changed;
    }

    public void checkChunk(World world, ChunkSnapshot snapshot, int minHeight, int maxHeight, boolean isStorm, AtomicInteger foundSet) {
        int found = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    Material material = utils.getRules().getBlockType(snapshot, x, y, z);
                    if (material != Material.AIR && values.getTorchesBlocks().contains(material)) {
                        found += 1;
                        if (utils.checkRules(world, utils.getRules().getBiome(snapshot, x, y, z))) {
                            continue;
                        }
                        checkTorch(world, null, snapshot, x, y, z, material, isStorm, false);
                    }
                }
            }
        }
        if (foundSet != null && found > 0) {
            foundSet.addAndGet(found);
        }
    }

    private int getYMax(Set<Integer> blocks) {
        int yMax = minValue;
        if (blocks.isEmpty()) {
            return yMax;
        }
        for (Integer block : blocks) {
            if (block > yMax) {
                yMax = block;
            }
        }
        blocks.clear();
        if (minValue != yMax) {
            blocks.add(yMax);
        }
        return yMax;
    }

    private void exit(ChunkData chunkDataValue, boolean changed) {
        if (changed) {
            chunkDataValue.setChanged(true);
            chunkDataValue.setLifeTime(chunkDataValue.getDefaultLifeTime());
        }
        chunkDataValue.getQueue().decrementAndGet();
        if (chunkDataValue.getQueue().get() <= 0) {
            chunkDataValue.setActive(true);
        }
    }

    private void updateChunk(World world, Chunk chunk, boolean isStorm) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        String fileName = chunkX + "_" + chunkZ + ".bin";
        String worldName = world.getName();
        File chunkFile = new File(worldFiles.get(worldName), fileName);
        if (!worldChunks.get(worldName).contains(fileName) && chunkData.get(fileName) == null) {
            return;
        }
        ChunkData chunkDataValue = getChunkData(worldName, chunkFile, fileName);
        synchronized (chunkDataValue) {
            updateValues(world, chunkDataValue, chunkX * 16, chunkZ * 16, isStorm);
        }
    }

    private int checkHeight(World world, ChunkSnapshot snapshot, Set<Integer> blocksSection, boolean checkDown, int x, int y, int z) {
        int maxHeight = snapshot.getHighestBlockYAt(x, z);
        int minHeight = utils.getMinHeight().get(world);
        int hasBlock = search(snapshot, blocksSection, maxHeight, 1, x, y, z);
        if (hasBlock != y || !checkDown || y == minHeight) {
            return hasBlock;
        }
        return search(snapshot, blocksSection, minHeight, -1, x, y, z);
    }

    private int search(ChunkSnapshot snapshot, Set<Integer> blocksSection, int height, int upInt, int x, int y, int z) {
        for (int i = y + upInt; upInt > 0 ? i <= height : i >= height; i = i + upInt) {
            Material material = utils.getRules().getBlockType(snapshot, x, i, z);
            if (!values.getTorchesBlocks().contains(material) && values.getType().isValid(material)) {
                utils.getTorchUtils().check(blocksSection, i);
                return i;
            }
        }
        return y;
    }

    public void check(Set<Integer> blocksSection, int y) {
        if (blocksSection != null) {
            blocksSection.add(y);
        }
    }

    public void updateChunksAsync(World world, boolean isStorm) {
        Chunk[] chunks = world.getLoadedChunks();
        scheduler.runTaskAsynchronously(plugin, () -> {
            for (Chunk chunk : chunks) {
                updateChunk(world, chunk, isStorm);
            }
        });
    }

    private void dropItems(World world, int x, int y, int z) {
        DropData data = new DropData(world, x, y, z);
        DropTask dropTask = plugin.getDropTask();
        dropTask.getDataList().add(data);
        AtomicBoolean active = dropTask.getActive();
        if (!active.get()) {
            active.set(true);
        }
    }

    private Map<Integer, Column> getXSection(Map<Integer, Map<Integer, Column>> locations, int x, boolean checkRemoveBlock) {
        Map<Integer, Column> xSection = locations.get(x);
        if (xSection != null) {
            return xSection;
        } else if (checkRemoveBlock) {
            return null;
        }
        xSection = new ConcurrentHashMap<>();
        locations.put(x, xSection);
        return xSection;
    }

    private Column getZSection(Map<Integer, Column> xSection, int z, boolean checkRemoveBlock) {
        Column zSection = xSection.get(z);
        if (zSection != null) {
            return zSection;
        } else if (checkRemoveBlock) {
            return null;
        }
        zSection = new Column();
        xSection.put(z, zSection);
        return zSection;
    }

    private void clearSections(Map<Integer, Map<Integer, Column>> locations, Map<Integer, Column> xSection, int x, int z) {
        xSection.remove(z);
        if (xSection.isEmpty()) {
            locations.remove(x);
        }
    }

    public void updateChunkAsync(World world, Chunk chunk) {
        boolean isStorm = world.hasStorm();
        scheduler.runTaskAsynchronously(plugin, () -> updateChunk(world, chunk, isStorm));
    }
}
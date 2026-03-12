package ru.mitriyf.rainbreaktorch.utils.common;

import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.common.data.ChunkData;
import ru.mitriyf.rainbreaktorch.utils.common.data.column.Column;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class TorchUtils {
    @Getter
    private final Map<String, ChunkData> chunkData = new ConcurrentHashMap<>();
    private final BukkitScheduler scheduler;
    private final ThreadLocalRandom rnd;
    @Getter
    private final RainBreakTorch plugin;
    private final ItemStack stick;
    private final Logger logger;
    private final Values values;
    private final Utils utils;
    private final File worlds;

    public TorchUtils(Utils utils, RainBreakTorch plugin) {
        this.utils = utils;
        this.plugin = plugin;
        rnd = plugin.getRnd();
        values = plugin.getValues();
        logger = plugin.getLogger();
        stick = new ItemStack(Material.STICK);
        scheduler = plugin.getServer().getScheduler();
        worlds = values.getWorldsFile();
        for (World world : plugin.getServer().getWorlds()) {
            updateChunksAsync(world);
        }
    }

    private boolean checkRules(World world, Biome biome) {
        return values.getWorldType().notContainsWorld(world) || values.getBiomeType().notContainsBiome(biome);
    }

    private File getChunkFile(String fileName, String worldName) {
        File worldChunks = new File(worlds, worldName);
        if (!worldChunks.exists() && !worldChunks.mkdirs()) {
            logger.warning("Error create mkdirs!");
        }
        return new File(worldChunks, fileName);
    }

    private ChunkData getChunkData(File chunkFile, String fileName) {
        ChunkData chunkDataValue = chunkData.computeIfAbsent(fileName, k -> new ChunkData(this, chunkFile, fileName));
        chunkDataValue.getQueue().incrementAndGet();
        chunkDataValue.setActive(false);
        return chunkDataValue;
    }

    public void saveTorch(World world, ChunkSnapshot snapshot, int x, int y, int z, Material material, boolean remove) {
        if (checkRules(world, snapshot.getBiome(x, y, z))) {
            return;
        }
        boolean isTorch = values.getTorchesBlocks().contains(material);
        boolean isCheckHeight = false;
        int chunkX = snapshot.getX();
        int chunkZ = snapshot.getZ();
        int checkHeight = 0;
        if (!remove && world.hasStorm()) {
            if (!isTorch) {
                return;
            } else {
                isCheckHeight = true;
                checkHeight = checkHeight(world, snapshot, null, false, x, y, z);
                if (checkHeight == y) {
                    dropItems(world, chunkX * 16 + x, y, chunkZ * 16 + z);
                    return;
                }
            }
        }
        String fileName = chunkX + "_" + chunkZ + ".bin";
        File chunkFile = getChunkFile(fileName, world.getName());
        if (remove && !chunkFile.exists() && chunkData.get(fileName) == null) {
            return;
        }
        ChunkData chunkDataValue = getChunkData(chunkFile, fileName);
        synchronized (chunkDataValue) {
            boolean changed, isFirst = true;
            boolean checkRemoveBlock = !isTorch && remove;
            Map<Integer, Map<Integer, Column>> locations = chunkDataValue.getLocations();
            Map<Integer, Column> xSection = getXSection(locations, x, checkRemoveBlock);
            if (xSection == null) {
                exit(chunkDataValue, false);
                return;
            }
            Column zSection = getZSection(xSection, z, checkRemoveBlock);
            if (zSection == null) {
                exit(chunkDataValue, false);
                return;
            }
            Set<Integer> blocksSection = zSection.blocks;
            Map<Integer, Boolean> torchesSection = zSection.torches;
            if (remove) {
                if (torchesSection.isEmpty()) {
                    exit(chunkDataValue, false);
                    return;
                }
                blocksSection.remove(y);
                if (torchesSection.get(y) == null && !blocksSection.isEmpty()) {
                    exit(chunkDataValue, false);
                    return;
                }
                torchesSection.remove(y);
                changed = true;
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
                            checkHeight(world, snapshot, blocksSection, false, x, y, z);
                        }
                        isFirst = true;
                        changed = false;
                    }
                } else if (material.isOccluding()) {
                    if (torchesSection.isEmpty()) {
                        exit(chunkDataValue, false);
                        return;
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
                            return;
                        }
                    }
                }
            }
            exit(chunkDataValue, processTorchesY(world, locations, xSection, torchesSection, blocksSection, x, z, chunkX * 16 + x, chunkZ * 16 + z, changed, isFirst));
        }
    }

    private void updateValues(World world, ChunkData chunkDataValue, int realChunkX, int realChunkZ) {
        Map<Integer, Map<Integer, Column>> locations = chunkDataValue.getLocations();
        if (locations.isEmpty()) {
            exit(chunkDataValue, false);
            return;
        }
        boolean changed = false;
        for (Integer x : new ArrayList<>(locations.keySet())) {
            Map<Integer, Column> xSection = locations.get(x);
            if (xSection == null) {
                continue;
            }
            for (Integer z : new ArrayList<>(xSection.keySet())) {
                Column zSection = xSection.get(z);
                if (zSection == null) {
                    continue;
                }
                Set<Integer> blocksSection = zSection.blocks;
                Map<Integer, Boolean> torchesSection = zSection.torches;
                changed = processTorchesY(world, locations, xSection, torchesSection, blocksSection, x, z, realChunkX + x, realChunkZ + z, changed, false);
            }
        }
        exit(chunkDataValue, changed);
    }

    private boolean processTorchesY(World world, Map<Integer, Map<Integer, Column>> locations, Map<Integer, Column> xSection, Map<Integer, Boolean> torchesSection, Set<Integer> blocksSection, int x, int z, int realX, int realZ, boolean changed, boolean isFirst) {
        Set<Integer> torchesSet = torchesSection.keySet();
        if (torchesSet.isEmpty()) {
            clearSections(locations, xSection, x, z);
            return changed || !isFirst;
        }
        int yMax = getYMax(blocksSection);
        boolean yMaxClear = true;
        for (Integer torch : new ArrayList<>(torchesSet)) {
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
                if (world.hasStorm()) {
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

    private int getYMax(Set<Integer> blocks) {
        int yMax = Integer.MIN_VALUE;
        if (blocks.isEmpty()) {
            return yMax;
        }
        for (Integer block : blocks) {
            if (block > yMax) {
                yMax = block;
            }
        }
        for (Integer block : new ArrayList<>(blocks)) {
            if (block != yMax) {
                blocks.remove(block);
            }
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

    private void updateChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        String fileName = chunkX + "_" + chunkZ + ".bin";
        File chunkFile = getChunkFile(fileName, world.getName());
        if (!chunkFile.exists() && chunkData.get(fileName) == null) {
            return;
        }
        ChunkData chunkDataValue = getChunkData(chunkFile, fileName);
        updateValues(world, chunkDataValue, chunkX * 16, chunkZ * 16);
    }

    private void dropItems(World world, int x, int y, int z) {
        scheduler.runTask(plugin, () -> {
            Location location = new Location(world, x, y, z);
            Block block = location.getBlock();
            Material material = block.getType();
            if (!values.getTorchesBlocks().contains(material)) {
                return;
            }
            stick.setAmount(rnd.nextInt(2) + 1);
            location.getBlock().setType(Material.AIR);
            world.dropItem(location, stick);
        });
    }

    private int checkHeight(World world, ChunkSnapshot snapshot, Set<Integer> blocksSection, boolean checkDown, int x, int y, int z) {
        int maxHeight = snapshot.getHighestBlockYAt(x, z);
        int minHeight = utils.isTemperature() ? 0 : world.getMinHeight();
        int hasBlock = search(snapshot, blocksSection, maxHeight, 1, x, y, z);
        if (hasBlock != y || !checkDown) {
            return hasBlock;
        }
        return search(snapshot, blocksSection, minHeight, -1, x, y, z);
    }

    private int search(ChunkSnapshot snapshot, Set<Integer> blocksSection, int height, int upInt, int x, int y, int z) {
        for (int i = y + upInt; upInt > 0 ? i <= height : i >= height; i = i + upInt) {
            Material material = snapshot.getBlockType(x, i, z);
            if (!values.getTorchesBlocks().contains(material) && material.isOccluding()) {
                check(blocksSection, i);
                return i;
            }
        }
        return y;
    }

    private void check(Set<Integer> blocksSection, int y) {
        if (blocksSection != null) {
            blocksSection.add(y);
        }
    }

    public void updateChunksAsync(World world) {
        Chunk[] chunks = world.getLoadedChunks();
        scheduler.runTaskAsynchronously(plugin, () -> {
            for (Chunk chunk : chunks) {
                updateChunk(chunk);
            }
        });
    }

    private Map<Integer, Column> getXSection(Map<Integer, Map<Integer, Column>> locations, int x, boolean checkRemoveBlock) {
        Map<Integer, Column> xSection = locations.get(x);
        if (xSection != null) {
            return xSection;
        } else if (checkRemoveBlock) {
            return null;
        }
        xSection = new HashMap<>();
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

    public void updateChunkAsync(Chunk chunk) {
        scheduler.runTaskAsynchronously(plugin, () -> updateChunk(chunk));
    }
}
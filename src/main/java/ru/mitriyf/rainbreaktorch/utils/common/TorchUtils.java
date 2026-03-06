package ru.mitriyf.rainbreaktorch.utils.common;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.common.data.ChunkData;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

@SuppressWarnings("DataFlowIssue")
public class TorchUtils {
    @Getter
    private final Map<String, ChunkData> chunkData = new HashMap<>();
    private final BukkitScheduler scheduler;
    private final ThreadLocalRandom rnd;
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
        worlds = new File(plugin.getDataFolder(), "worlds");
        for (World world : plugin.getServer().getWorlds()) {
            updateChunks(world);
        }
    }

    public void saveTorch(World world, Block block, Material material, boolean remove) {
        File chunks = new File(worlds, world.getName());
        boolean isTorch = values.getTorchesBlocks().contains(material.name());
        if (!worlds.exists() && !chunks.exists() && !chunks.mkdirs()) {
            logger.warning("Error create mkdirs!");
        } else if (!remove && !isTorch && !block.getType().isSolid()) {
            return;
        }
        Chunk chunk = block.getChunk();
        String fileName = chunk.getX() + "_" + chunk.getZ() + ".yml";
        File file = new File(chunks, fileName);
        ChunkData chunkDataValue = chunkData.computeIfAbsent(fileName, k -> new ChunkData(file, logger));
        synchronized (chunkDataValue) {
            chunkData.putIfAbsent(fileName, chunkDataValue);
            YamlConfiguration dataFile = chunkDataValue.getDataFile();
            boolean changed = true;
            try {
                ConfigurationSection zSection = getZSection(dataFile, block);
                ConfigurationSection blocksSection = zSection.getConfigurationSection("blocks");
                if (blocksSection == null) {
                    blocksSection = zSection.createSection("blocks");
                }
                ConfigurationSection torchesSection = zSection.getConfigurationSection("torches");
                if (torchesSection == null) {
                    torchesSection = zSection.createSection("torches");
                }
                String yBlock = String.valueOf(block.getY());
                if (remove) {
                    if (torchesSection.getKeys(true).isEmpty()) {
                        changed = false;
                        return;
                    }
                    torchesSection.set(yBlock, null);
                    blocksSection.set(yBlock, null);
                    checkHeight(dataFile, world, block, true);
                } else {
                    ConfigurationSection blockSection = blocksSection;
                    if (isTorch) {
                        blockSection = torchesSection;
                        checkHeight(dataFile, world, block, false);
                    }
                    if (blockSection.getConfigurationSection(yBlock) == null) {
                        blockSection.createSection(yBlock);
                    }
                    if (torchesSection.getKeys(true).isEmpty()) {
                        changed = false;
                    }
                }
            } finally {
                updateValues(dataFile, fileName, world, changed);
            }
        }
    }

    private void check(YamlConfiguration dataFile, Block block) {
        ConfigurationSection zSection = getZSection(dataFile, block);
        ConfigurationSection blocksSection = zSection.getConfigurationSection("blocks");
        if (blocksSection == null) {
            blocksSection = zSection.createSection("blocks");
        }
        String yBlock = String.valueOf(block.getY());
        if (blocksSection.getConfigurationSection(yBlock) == null) {
            blocksSection.createSection(yBlock);
        }
    }

    private void updateValues(YamlConfiguration chunk, String fileName, World world, boolean changed) {
        ConfigurationSection locations = chunk.getConfigurationSection("locations");
        if (locations == null) {
            return;
        }
        for (String x : locations.getKeys(false)) {
            ConfigurationSection xSection = locations.getConfigurationSection(x);
            for (String z : xSection.getKeys(false)) {
                ConfigurationSection zCoords = xSection.getConfigurationSection(z);
                ConfigurationSection torches = zCoords.getConfigurationSection("torches");
                Set<String> torchesSet = torches.getKeys(false);
                if (torchesSet.isEmpty()) {
                    changed = true;
                    clearSections(locations, xSection, x, z);
                    break;
                }
                ConfigurationSection blocks = zCoords.getConfigurationSection("blocks");
                int yMax = Integer.MIN_VALUE;
                for (String torch : torchesSet) {
                    int yTorch = Integer.parseInt(torch);
                    boolean hasBlock = false;
                    for (String block : blocks.getKeys(false)) {
                        int yBlock = Integer.parseInt(block);
                        if (yBlock > yTorch) {
                            hasBlock = true;
                            if (yBlock > yMax) {
                                blocks.set(String.valueOf(yMax), null);
                                yMax = yBlock;
                            } else if (yBlock != yMax) {
                                blocks.set(String.valueOf(yBlock), null);
                            }
                        }
                    }
                    ConfigurationSection torchSection = torches.getConfigurationSection(torch);
                    boolean torchBoolean = torchSection.getBoolean("noBreak");
                    if (hasBlock) {
                        changed = true;
                        torchSection.set("noBreak", true);
                        torchBoolean = true;
                    } else {
                        if (torchBoolean) {
                            changed = true;
                            torchSection.set("noBreak", null);
                            torchBoolean = false;
                        }
                    }
                    if (world.hasStorm()) {
                        if (!torchBoolean) {
                            changed = true;
                            torches.set(torch, null);
                            if (torchesSet.isEmpty()) {
                                clearSections(locations, xSection, x, z);
                            }
                            dropItems(world, utils.formatInt(x), yTorch, utils.formatInt(z));
                        }
                    }
                }
            }
        }
        exit(fileName, changed);
    }

    private void exit(String fileName, boolean changed) {
        ChunkData chunkDataValue = chunkData.get(fileName);
        BukkitTask task = chunkDataValue.getTask();
        if (task != null) {
            task.cancel();
            chunkDataValue.setTask(null);
        }
        if (changed) {
            chunkDataValue.setChanged(true);
        }
        chunkDataValue.setTask(scheduler.runTaskLater(plugin, () -> {
            chunkData.get(fileName).save();
            chunkData.remove(fileName);
        }, values.getObjectRemove() * 20L));
    }

    private ConfigurationSection getZSection(YamlConfiguration dataFile, Block block) {
        ConfigurationSection locations = dataFile.getConfigurationSection("locations");
        if (locations == null) {
            locations = dataFile.createSection("locations");
        }
        String xValue = String.valueOf(block.getX());
        ConfigurationSection xSection = locations.getConfigurationSection(xValue);
        if (xSection == null) {
            xSection = locations.createSection(xValue);
        }
        String zValue = String.valueOf(block.getZ());
        ConfigurationSection zSection = xSection.getConfigurationSection(zValue);
        if (zSection == null) {
            zSection = xSection.createSection(zValue);
        }
        return zSection;
    }

    private void updateChunk(Chunk chunk) {
        World world = chunk.getWorld();
        File chunks = new File(worlds, world.getName());
        if (!worlds.exists() || !chunks.exists()) {
            return;
        }
        String fileName = chunk.getX() + "_" + chunk.getZ() + ".yml";
        File file = new File(chunks, fileName);
        if (!file.exists()) {
            return;
        }
        ChunkData chunkDataValue = chunkData.computeIfAbsent(fileName, k -> new ChunkData(file, logger));
        synchronized (chunkDataValue) {
            chunkData.putIfAbsent(fileName, chunkDataValue);
            updateValues(chunkDataValue.getDataFile(), fileName, world, false);
        }
    }

    private void dropItems(World world, int x, int y, int z) {
        scheduler.runTask(plugin, () -> {
            Location location = new Location(world, x, y, z);
            Block block = location.getBlock();
            Material material = block.getType();
            if (!values.getTorchesBlocks().contains(material.name())) {
                return;
            }
            stick.setAmount(rnd.nextInt(2) + 1);
            location.getBlock().setType(Material.AIR);
            world.dropItem(location, stick);
        });
    }

    private void checkHeight(YamlConfiguration dataFile, World world, Block block, boolean checkDown) {
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.runTask(plugin, () -> {
            if (search(dataFile, world, block, world.getMaxHeight(), 1) && checkDown) {
                search(dataFile, world, block, utils.isTemperature() ? 0 : world.getMinHeight(), -1);
            }
            latch.countDown();
        });
        try {
            latch.await();
        } catch (Exception ignored) {
        }
    }

    private boolean search(YamlConfiguration dataFile, World world, Block block, int height, int upInt) {
        for (int i = block.getY(); upInt > 0 ? i < height : i > height; i = i + upInt) {
            Block blockNew = world.getBlockAt(block.getX(), i, block.getZ());
            if (blockNew.getType().isSolid()) {
                check(dataFile, blockNew);
                return false;
            }
        }
        return true;
    }

    public void updateChunks(World world) {
        scheduler.runTaskAsynchronously(plugin, () -> {
            for (Chunk chunk : world.getLoadedChunks()) {
                updateChunk(chunk);
            }
        });
    }

    private void clearSections(ConfigurationSection locations, ConfigurationSection xSection, String x, String z) {
        xSection.set(z, null);
        if (xSection.getKeys(true).isEmpty()) {
            locations.set(x, null);
        }
    }
}
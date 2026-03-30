package ru.mitriyf.rainbreaktorch.cmd.subcommands;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkSubCommands {
    private final Utils utils;
    private final Values values;
    private final RainBreakTorch plugin;
    private final BukkitScheduler scheduler;
    @Getter
    private BukkitTask taskWorld;
    private int speedInTick = 4;

    public ChunkSubCommands(RainBreakTorch plugin, Values values, Utils utils, BukkitScheduler scheduler) {
        this.utils = utils;
        this.values = values;
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    public void checkChunkSubCommands(CommandSender sender, String arg1, String arg2) {
        switch (arg1.toLowerCase()) {
            case "cancel": {
                if (taskWorld != null) {
                    taskWorld.cancel();
                    taskWorld = null;
                    sender.sendMessage("Successfully. §eSome tasks may require more time to fully cancel. §aYou can create a new task.");
                } else {
                    sender.sendMessage("§cThe task is not set.");
                }
                return;
            }
            case "speed": {
                if (!arg2.isEmpty()) {
                    if (taskWorld == null) {
                        try {
                            final int oldSpeedInTick = speedInTick;
                            speedInTick = utils.formatInt(arg2);
                            sender.sendMessage("§aThe worlds upload speed is set to: " + speedInTick + " (old speed: " + oldSpeedInTick + ")");
                        } catch (Exception e) {
                            sendTwo(sender, "§cThe speed is not specified/incorrect.");
                        }
                    } else {
                        sender.sendMessage("§cIt is not allowed to change the speed during a task.");
                    }
                } else {
                    sender.sendMessage("Current speed: §e" + speedInTick);
                }
                return;
            }
            case "active": {
                sender.sendMessage("Successfully. Wait for it...");
                processChunks(sender, arg2, 0);
                return;
            }
            case "all": {
                sender.sendMessage("§eThe command to §cupdate all chunks§e for torches§f has been received. §ePlease wait...");
                processChunks(sender, arg2, 1);
                return;
            }
            default: {
                utils.sendMessage(sender, values.getHelp());
            }
        }
    }

    private void processChunks(CommandSender sender, String worldName, int type) {
        if (taskWorld != null) {
            sender.sendMessage("§cYou cannot create a new task until the previous one is completed or canceled.\n§e/rainbreaktorch update cancel §f- §cCanceling§f a previous task.");
            return;
        }
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage("§cSuch a world has not been discovered!");
            return;
        }
        int minHeight = utils.getMinHeight().get(world);
        int maxHeight = world.getMaxHeight();
        if (type == 0) {
            Chunk[] chunks = world.getLoadedChunks();
            AtomicInteger found = new AtomicInteger(0);
            startProcessChunks(sender, world, worldName, minHeight, maxHeight, chunks, found);
        } else {
            sendTwo(sender, "§eWe get the region files...");
            File regionFolder = new File(world.getWorldFolder(), "region");
            if (!regionFolder.isDirectory()) {
                sendTwo(sender, "§cError. This is not a directory.");
                return;
            }
            File[] files = regionFolder.listFiles();
            if (files == null || files.length == 0) {
                sendTwo(sender, "§cError. There are no files.");
                return;
            }
            ConcurrentLinkedQueue<File> fileRegions = new ConcurrentLinkedQueue<>(Arrays.asList(files));
            sendTwo(sender, "§aThe region files have been received.§e The work process has begun.");
            startProcessAllChunks(sender, world, minHeight, maxHeight, fileRegions);
        }
    }

    private void startProcessChunks(CommandSender sender, World world, String worldName, int minHeight, int maxHeight, Chunk[] chunks, AtomicInteger found) {
        taskWorld = new BukkitRunnable() {
            final AtomicInteger currentChunks = new AtomicInteger(0);
            final int length = chunks.length;
            int ticks = 0;
            int i = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks >= 200) {
                    ticks = 0;
                    String message = "Found for world " + worldName + ": " + found.get();
                    sendTwo(sender, message);
                }
                if (currentChunks.get() != i) {
                    return;
                } else if (i >= length) {
                    sendTwo(sender, "§aThe update is complete. Received torches: " + found.get());
                    cancel();
                    taskWorld = null;
                    return;
                }
                processChunk(world, chunks[i++], minHeight, maxHeight, found, currentChunks);
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private void startProcessAllChunks(CommandSender sender, World world, int minHeight, int maxHeight, ConcurrentLinkedQueue<File> fileRegions) {
        AtomicInteger chunks = new AtomicInteger();
        AtomicInteger found = new AtomicInteger();
        AtomicInteger lost = new AtomicInteger();
        taskWorld = new BukkitRunnable() {
            private File currentFile = null;
            private int startX, startZ, currentRegionChunks = 0, currentChunks = 0;

            @Override
            public void run() {
                if (currentChunks % speedInTick != 0 || currentChunks != chunks.get()) {
                    return;
                }
                for (int i = 0; i < speedInTick; i++) {
                    if (currentFile == null) {
                        currentFile = fileRegions.poll();
                        if (currentFile == null) {
                            sendTwo(sender, "§aThe process is complete! §fFound chunks: §a" + chunks.get() + " (not generated: " + lost.get() + ")§f. Found torches: §a" + found.get());
                            cancel();
                            taskWorld = null;
                            return;
                        }
                        String[] parts = currentFile.getName().split("\\.");
                        startX = Integer.parseInt(parts[1]) << 5;
                        startZ = Integer.parseInt(parts[2]) << 5;
                    }
                    int x = startX + (currentRegionChunks % 32);
                    int z = startZ + (currentRegionChunks / 32);
                    if (world.isChunkGenerated(x, z)) {
                        if (!world.isChunkLoaded(x, z)) {
                            world.loadChunk(x, z, true);
                        }
                        Chunk chunk = world.getChunkAt(x, z);
                        processChunk(world, chunk, minHeight, maxHeight, found, chunks);
                    } else {
                        chunks.addAndGet(1);
                        lost.addAndGet(1);
                    }
                    if (currentRegionChunks != 0 && currentRegionChunks % 1024 == 0) {
                        currentFile = null;
                        currentRegionChunks = 0;
                        sendTwo(sender, "§eStatus: " + chunks.get() + " chunks (not generated: " + lost.get() + ") §fand §e" + found.get() + " torches§a found.");
                    }
                    currentRegionChunks++;
                    currentChunks++;
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private void processChunk(World world, Chunk chunk, int minHeight, int maxHeight, AtomicInteger found, AtomicInteger currentChunks) {
        boolean isStorm = world.hasStorm();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, true, false);
        scheduler.runTaskAsynchronously(plugin, () -> {
            try {
                utils.getTorchUtils().checkChunk(world, snapshot, minHeight, maxHeight, isStorm, found);
            } catch (Exception e) {
                plugin.getLogger().warning("Error chunk: " + e);
            } finally {
                currentChunks.addAndGet(1);
            }
        });
    }

    private void sendTwo(CommandSender sender, String message) {
        if (sender != plugin.getServer().getConsoleSender()) {
            sender.sendMessage(message);
        }
        plugin.getLogger().info(message);
    }
}

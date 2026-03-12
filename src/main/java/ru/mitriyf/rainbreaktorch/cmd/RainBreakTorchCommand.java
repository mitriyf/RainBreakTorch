package ru.mitriyf.rainbreaktorch.cmd;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("NullableProblems")
public class RainBreakTorchCommand implements CommandExecutor {
    private final Set<String> confirmation = new HashSet<>();
    private final BukkitScheduler scheduler;
    private final RainBreakTorch plugin;
    private final Values values;
    private final Utils utils;
    private BukkitTask taskWorld;

    public RainBreakTorchCommand(RainBreakTorch plugin) {
        this.plugin = plugin;
        utils = plugin.getUtils();
        values = plugin.getValues();
        scheduler = plugin.getServer().getScheduler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        boolean permission = sender.hasPermission("rainbreaktorch.use");
        if (!permission || args.length < 1 || args.length > 3) {
            if (permission) {
                utils.sendMessage(sender, values.getHelp());
            } else {
                utils.sendMessage(sender, values.getNoperm());
            }
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "reload": {
                plugin.getValues().setup();
                sender.sendMessage("Successfully!");
                return false;
            }
            case "status": {
                sender.sendMessage("§aPlugin status:\n");
                sender.sendMessage("§aGood. §fBlocks: " + values.getTorchesBlocks());
                sender.sendMessage("§aBiomes: §ftype - " + values.getBiomeType().getClass().getSimpleName() + ", list: " + values.getBiomes());
                sender.sendMessage("§aWorlds: §ftype - " + values.getWorldType().getClass().getSimpleName() + ", list: " + values.getWorlds());
                sender.sendMessage("§aTask: " + (taskWorld != null ? "§aEnabled" : "§cDisabled"));
                return false;
            }
            case "update": {
                String name = sender.getName();
                if (!confirmation.contains(name)) {
                    sendWarning(sender);
                    confirmation.add(name);
                    scheduler.runTaskLater(plugin, () -> confirmation.remove(name), 600);
                } else {
                    switch (args[1].toLowerCase()) {
                        case "cancel": {
                            if (taskWorld != null) {
                                taskWorld.cancel();
                                taskWorld = null;
                                sender.sendMessage("Successfully. §eSome tasks may require more time to fully cancel. §aYou can create a new task.");
                            } else {
                                sender.sendMessage("§cThe task is not set.");
                            }
                            return false;
                        }
                        case "active": {
                            sender.sendMessage("Successfully. Wait for it...");
                            processActiveChunks(sender, args[2]);
                            return false;
                        }
                        case "all": {
                            sender.sendMessage("The command to §cupdate all chunks§e for torches§f has been received. Please wait...");
                            return false;
                        }
                        default: {
                            utils.sendMessage(sender, values.getHelp());
                            return false;
                        }
                    }
                }
                return false;
            }
            default: {
                utils.sendMessage(sender, values.getHelp());
                return false;
            }
        }
    }

    private void processActiveChunks(CommandSender sender, String worldName) {
        if (taskWorld != null) {
            sender.sendMessage("§cYou cannot create a new task until the previous one is completed or canceled.\n§e/rainbreaktorch update cancel §f- §cCanceling§f a previous task.");
            return;
        }
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage("§сSuch a world has not been discovered!");
            return;
        }
        int minHeight = utils.isTemperature() ? 0 : world.getMinHeight();
        int maxHeight = world.getMaxHeight();
        Chunk[] chunks = world.getLoadedChunks();
        AtomicInteger found = new AtomicInteger(0);
        BukkitTask task = scheduler.runTaskTimer(plugin, () -> {
            String message = "Found for world " + worldName + ": " + found.get();
            sendTwo(sender, message);
        }, 100, 100);
        taskWorld = new BukkitRunnable() {
            final int length = chunks.length;
            int currentChunks = 0;
            int i = 0;

            @Override
            public void run() {
                if (currentChunks != i) {
                    return;
                } else if (i >= length) {
                    task.cancel();
                    String message = "§aThe update is complete. Received torches: " + found.get();
                    sendTwo(sender, message);
                    cancel();
                    taskWorld = null;
                    return;
                }
                ChunkSnapshot snapshot = chunks[i++].getChunkSnapshot(true, true, false);
                scheduler.runTaskAsynchronously(plugin, () -> {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = minHeight; y < maxHeight; y++) {
                                Material material = snapshot.getBlockType(x, y, z);
                                if (values.getTorchesBlocks().contains(material)) {
                                    found.incrementAndGet();
                                    utils.getTorchUtils().saveTorch(world, snapshot, x, y, z, material, false);
                                }
                            }
                        }
                    }
                    currentChunks++;
                });
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private void sendTwo(CommandSender sender, String message) {
        if (sender != plugin.getServer().getConsoleSender()) {
            sender.sendMessage(message);
        }
        plugin.getLogger().info(message);
    }

    private void sendWarning(CommandSender sender) {
        sender.sendMessage("§cAttention! §eBy entering this command again, you are agreeing that all loaded chunks in the world will be used for torch checking.");
        sender.sendMessage("§cUsing this command may negatively affect the stability of the game until all loaded chunks are checked.\n");
        sender.sendMessage("§fAre you sure? §a/rainbreaktorch update world\n§eYou have 30 seconds to make a choice. Only select regular worlds (do not use nether or ender worlds)");
    }
}
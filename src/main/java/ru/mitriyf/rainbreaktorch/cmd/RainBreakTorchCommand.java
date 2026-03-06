package ru.mitriyf.rainbreaktorch.cmd;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("NullableProblems")
public class RainBreakTorchCommand implements CommandExecutor {
    private final Set<String> confirmation = new HashSet<>();
    private final Map<String, Integer> torchesFound = new ConcurrentHashMap<>();
    private final BukkitScheduler scheduler;
    private final RainBreakTorch plugin;
    private final Values values;
    private final Utils utils;

    public RainBreakTorchCommand(RainBreakTorch plugin) {
        this.plugin = plugin;
        utils = plugin.getUtils();
        values = plugin.getValues();
        scheduler = plugin.getServer().getScheduler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        boolean permission = sender.hasPermission("jambience.use");
        if (!permission || (args.length != 1 && args.length != 2)) {
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
                Integer torches = torchesFound.get(sender.getName());
                if (torches != null) {
                    sender.sendMessage("\n§cUpdate in progress. Found torches: " + torches);
                }
                return false;
            }
            case "update": {
                String name = sender.getName();
                if (!confirmation.contains(name)) {
                    sendWarning(sender);
                    confirmation.add(name);
                    scheduler.runTaskLater(plugin, () -> confirmation.remove(name), 600);
                } else {
                    sender.sendMessage("Successfully. Wait for it...");
                    processChunks(sender, name, args[1]);
                }
                return false;
            }
            default: {
                utils.sendMessage(sender, values.getHelp());
                return false;
            }
        }
    }

    private void processChunks(CommandSender sender, String senderName, String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage("§сSuch a world has not been discovered!");
            return;
        }
        int maxHeight = world.getMaxHeight();
        int minHeight = utils.isTemperature() ? 0 : world.getMinHeight();
        torchesFound.put(senderName, 0);
        BukkitTask task = scheduler.runTaskTimer(plugin, () -> {
            String message = "Found: " + torchesFound.get(senderName);
            sendTwo(sender, message);
        }, 100, 100);
        scheduler.runTaskAsynchronously(plugin, () -> {
            int i = 0;
            Chunk[] chunks = world.getLoadedChunks();
            for (Chunk chunk : chunks) {
                ChunkSnapshot snapshot = chunk.getChunkSnapshot();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minHeight; y < maxHeight; y++) {
                            Material material = snapshot.getBlockType(x, y, z);
                            if (values.getTorchesBlocks().contains(material.name())) {
                                i++;
                                int realX = snapshot.getX() * 16 + x;
                                int realZ = snapshot.getZ() * 16 + z;
                                utils.saveTorchWithAsync(world, world.getBlockAt(realX, y, realZ), material, false);
                                torchesFound.put(senderName, i);
                            }
                        }
                    }
                }
            }
            task.cancel();
            torchesFound.remove(senderName);
            String message = "§aThe update is complete. Received torches: " + i;
            sendTwo(sender, message);
        });
    }

    private void sendTwo(CommandSender sender, String message) {
        sender.sendMessage(message);
        plugin.getLogger().info(message);
    }

    private void sendWarning(CommandSender sender) {
        sender.sendMessage("§cAttention! §eBy entering this command again, you are agreeing that all loaded chunks in the world will be used for torch checking.");
        sender.sendMessage("§сUsing this command may negatively affect the stability of the game until all loaded chunks are checked.\n");
        sender.sendMessage("§fAre you sure? §a/rainbreaktorch update world\n§eYou have 30 seconds to make a choice. Only select regular worlds (do not use nether or ender worlds)");
    }
}
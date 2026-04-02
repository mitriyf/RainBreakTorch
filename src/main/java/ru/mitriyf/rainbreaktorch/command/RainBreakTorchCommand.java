package ru.mitriyf.rainbreaktorch.command;

import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.command.subcommand.ChunkSubCommands;
import ru.mitriyf.rainbreaktorch.command.subcommand.LootSubCommands;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.HashSet;
import java.util.Set;

@Getter
@SuppressWarnings("NullableProblems")
public class RainBreakTorchCommand implements CommandExecutor {
    private final Set<String> confirmation = new HashSet<>();
    private final ChunkSubCommands chunkSubCommands;
    private final LootSubCommands lootSubCommands;
    private final BukkitScheduler scheduler;
    private final RainBreakTorch plugin;
    private final Values values;
    private final Utils utils;

    public RainBreakTorchCommand(RainBreakTorch plugin) {
        this.plugin = plugin;
        utils = plugin.getUtils();
        values = plugin.getValues();
        scheduler = plugin.getServer().getScheduler();
        lootSubCommands = new LootSubCommands(plugin, values, utils);
        chunkSubCommands = new ChunkSubCommands(plugin, values, utils, scheduler);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        boolean permission = sender.hasPermission("rainbreaktorch.use");
        if (!permission || args.length < 1 || args.length > 9) {
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
                sender.sendMessage("§aTask: " + (chunkSubCommands.getTaskWorld() != null ? "§aEnabled" : "§cDisabled"));
                return false;
            }
            case "loot": {
                lootSubCommands.checkLootSubCommands(sender, args);
                return false;
            }
            case "update": {
                String name = sender.getName();
                if (!confirmation.contains(name)) {
                    sendWarning(sender);
                    confirmation.add(name);
                    scheduler.runTaskLater(plugin, () -> confirmation.remove(name), 600);
                } else {
                    String arg1 = "";
                    if (args.length >= 2) {
                        arg1 = args[1];
                    }
                    String arg2 = "";
                    if (args.length == 3) {
                        arg2 = args[2];
                    }
                    chunkSubCommands.checkChunkSubCommands(sender, arg1, arg2);
                }
                return false;
            }
            default: {
                utils.sendMessage(sender, values.getHelp());
                return false;
            }
        }
    }

    private void sendWarning(CommandSender sender) {
        sender.sendMessage("§cAttention! §eBy entering this command again, you are agreeing that all loaded chunks in the world will be used for torch checking.");
        sender.sendMessage("§cUsing this command may negatively affect the stability of the game until all loaded chunks are checked.\n");
        sender.sendMessage("§fAre you sure? §a/rainbreaktorch update type world\n§eYou have 30 seconds to make a choice. Only select regular worlds (do not use nether or ender worlds)");
    }
}
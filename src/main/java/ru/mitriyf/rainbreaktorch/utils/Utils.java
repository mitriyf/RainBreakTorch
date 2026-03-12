package ru.mitriyf.rainbreaktorch.utils;

import lombok.Getter;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.actions.Action;
import ru.mitriyf.rainbreaktorch.utils.actions.ActionType;
import ru.mitriyf.rainbreaktorch.utils.common.CommonUtils;
import ru.mitriyf.rainbreaktorch.utils.common.TorchUtils;
import ru.mitriyf.rainbreaktorch.utils.piston.PistonRetract;
import ru.mitriyf.rainbreaktorch.utils.piston.impl.RetractVersion7;
import ru.mitriyf.rainbreaktorch.utils.piston.impl.RetractVersion8;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Getter
public class Utils {
    private final Values values;
    private final Logger logger;
    private final RainBreakTorch plugin;
    private final CountDownLatch latch;
    private final TorchUtils torchUtils;
    private final CommonUtils commonUtils;
    private final BukkitScheduler scheduler;
    private PistonRetract pistonRetract;
    private boolean temperature;

    public Utils(RainBreakTorch plugin) {
        this.plugin = plugin;
        values = plugin.getValues();
        logger = plugin.getLogger();
        latch = new CountDownLatch(1);
        scheduler = plugin.getServer().getScheduler();
        torchUtils = new TorchUtils(this, plugin);
        commonUtils = new CommonUtils(this, plugin);
    }

    public void setup() {
        int version = plugin.getVersion();
        if (version < 17) {
            temperature = true;
            if (version < 8) {
                pistonRetract = new RetractVersion7(plugin);
                return;
            }
        }
        pistonRetract = new RetractVersion8(plugin);
    }

    public void sendMessage(CommandSender sender, List<Action> actions) {
        scheduler.runTaskAsynchronously(plugin, () -> {
            for (Action action : actions) {
                sendSender(sender, action);
            }
        });
    }

    private void sendSender(CommandSender sender, Action action) {
        ActionType type = action.getType();
        String context = action.getContext();
        switch (type) {
            case CONSOLE:
                commonUtils.dispatchConsole(context);
                break;
            case BROADCAST:
                commonUtils.broadcast(context);
                break;
            case LOG:
                log(context);
                break;
            case DELAY:
                try {
                    if (latch.await(formatInt(context) * 50L, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (Exception ignored) {
                }
                break;
            default:
                sendMessage(sender, context);
                break;
        }
    }

    private void sendMessage(CommandSender sender, String text) {
        sender.sendMessage(formatString(text));
    }

    public String formatString(String s) {
        return values.getColorizer().colorize(s);
    }

    public int formatInt(String text) {
        return Integer.parseInt(text);
    }

    private void log(String log) {
        logger.info(log);
    }

    public void saveTorch(Block block, boolean remove) {
        ChunkSnapshot snapshot = block.getChunk().getChunkSnapshot(true, true, false);
        int x = block.getX() & 15;
        int z = block.getZ() & 15;
        scheduler.runTaskAsynchronously(plugin, () -> torchUtils.saveTorch(block.getWorld(), snapshot, x, block.getY(), z, block.getType(), remove));
    }
}

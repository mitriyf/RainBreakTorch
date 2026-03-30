package ru.mitriyf.rainbreaktorch.utils;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.actions.Action;
import ru.mitriyf.rainbreaktorch.utils.actions.ActionType;
import ru.mitriyf.rainbreaktorch.utils.block.SetType;
import ru.mitriyf.rainbreaktorch.utils.block.impl.SetType7;
import ru.mitriyf.rainbreaktorch.utils.block.impl.SetType8;
import ru.mitriyf.rainbreaktorch.utils.common.CommonUtils;
import ru.mitriyf.rainbreaktorch.utils.common.TorchUtils;
import ru.mitriyf.rainbreaktorch.utils.common.height.MinHeight;
import ru.mitriyf.rainbreaktorch.utils.common.height.impl.MinHeight16;
import ru.mitriyf.rainbreaktorch.utils.common.height.impl.MinHeight17;
import ru.mitriyf.rainbreaktorch.utils.common.rules.Rules;
import ru.mitriyf.rainbreaktorch.utils.common.rules.impl.Rules12;
import ru.mitriyf.rainbreaktorch.utils.common.rules.impl.Rules13;
import ru.mitriyf.rainbreaktorch.utils.hand.HandItem;
import ru.mitriyf.rainbreaktorch.utils.hand.impl.HandItem8;
import ru.mitriyf.rainbreaktorch.utils.hand.impl.HandItem9;
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
    private MinHeight minHeight;
    private HandItem handItem;
    private SetType setType;
    private Rules rules;

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
        boolean minVersion = version < 17;
        if (minVersion) {
            minHeight = new MinHeight16();
        } else {
            minHeight = new MinHeight17();
        }
        minVersion = minVersion && version < 13;
        if (minVersion) {
            rules = new Rules12(this);
        } else {
            rules = new Rules13(this);
        }
        minVersion = minVersion && version < 9;
        if (minVersion) {
            handItem = new HandItem8();
        } else {
            handItem = new HandItem9();
        }
        minVersion = minVersion && version < 8;
        if (minVersion) {
            pistonRetract = new RetractVersion7(plugin);
            setType = new SetType7(this);
        } else {
            pistonRetract = new RetractVersion8(plugin);
            setType = new SetType8();
        }
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

    public void checkTorch(Block block, Material material, boolean remove) {
        Chunk chunk = block.getChunk();
        World world = block.getWorld();
        if (checkRules(world, block.getBiome())) {
            return;
        }
        int x = block.getX() & 15;
        int y = block.getY();
        int z = block.getZ() & 15;
        if (material == null) {
            material = block.getType();
        }
        Material finalMaterial = material;
        boolean isStorm = world.hasStorm();
        scheduler.runTaskAsynchronously(plugin, () -> torchUtils.checkTorch(world, chunk, null, x, y, z, finalMaterial, isStorm, remove));
    }

    public void checkTorch(Block block, boolean remove) {
        checkTorch(block, null, remove);
    }

    public boolean checkRules(World world, Biome biome) {
        return values.getWorldType().notContainsWorld(world) || values.getBiomeType().notContainsBiome(biome);
    }
}

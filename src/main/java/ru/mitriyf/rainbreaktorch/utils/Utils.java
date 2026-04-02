package ru.mitriyf.rainbreaktorch.utils;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.compat.abstraction.*;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_12.BlockSetterV12;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_12.VersionRulesV12;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_13.BlockSetterV13;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_13.VersionRulesV13;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_16.HeightProviderV16;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_17.HeightProviderV17;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_7.PistonHandlerV7;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_8.HandInquisitorV8;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_8.PistonHandlerV8;
import ru.mitriyf.rainbreaktorch.compat.impl.v1_9.HandInquisitorV9;
import ru.mitriyf.rainbreaktorch.utils.actions.Action;
import ru.mitriyf.rainbreaktorch.utils.actions.ActionType;
import ru.mitriyf.rainbreaktorch.utils.common.CommonUtils;
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
    private final CommonUtils commonUtils;
    private final BukkitScheduler scheduler;
    private HeightProvider heightProvider;
    private HandInquisitor handInquisitor;
    private PistonHandler pistonHandler;
    private VersionRules versionRules;
    private BlockSetter blockSetter;

    public Utils(RainBreakTorch plugin) {
        this.plugin = plugin;
        values = plugin.getValues();
        logger = plugin.getLogger();
        latch = new CountDownLatch(1);
        scheduler = plugin.getServer().getScheduler();
        commonUtils = new CommonUtils(this, plugin);
    }

    public void setup() {
        int version = plugin.getVersion();
        boolean minVersion = version < 17;
        if (minVersion) {
            heightProvider = new HeightProviderV16();
        } else {
            heightProvider = new HeightProviderV17();
        }
        minVersion = minVersion && version < 13;
        if (minVersion) {
            blockSetter = new BlockSetterV12(logger);
            versionRules = new VersionRulesV12(values);
        } else {
            versionRules = new VersionRulesV13(values);
            blockSetter = new BlockSetterV13();
        }
        minVersion = minVersion && version < 9;
        if (minVersion) {
            handInquisitor = new HandInquisitorV8();
        } else {
            handInquisitor = new HandInquisitorV9();
        }
        minVersion = minVersion && version < 8;
        if (minVersion) {
            pistonHandler = new PistonHandlerV7(plugin);
        } else {
            pistonHandler = new PistonHandlerV8(plugin);
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
            case CONSOLE: {
                commonUtils.dispatchConsole(context);
                break;
            }
            case BROADCAST: {
                commonUtils.broadcast(context);
                break;
            }
            case LOG: {
                log(context);
                break;
            }
            case DELAY: {
                try {
                    if (latch.await(formatInt(context) * 50L, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (Exception ignored) {
                }
                break;
            }
            default: {
                sendMessage(sender, context);
                break;
            }
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
}

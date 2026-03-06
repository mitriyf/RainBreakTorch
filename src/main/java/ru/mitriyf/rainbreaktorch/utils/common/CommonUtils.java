package ru.mitriyf.rainbreaktorch.utils.common;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;

public class CommonUtils {
    private final BukkitScheduler scheduler;
    private final RainBreakTorch plugin;
    private final Utils utils;

    public CommonUtils(Utils utils, RainBreakTorch plugin) {
        this.utils = utils;
        this.plugin = plugin;
        scheduler = plugin.getServer().getScheduler();
    }

    public void broadcast(String message) {
        String text = utils.formatString(message);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(text);
        }
    }

    public void dispatchConsole(String cmd) {
        scheduler.runTaskLater(plugin, () -> plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd), 0);
    }
}

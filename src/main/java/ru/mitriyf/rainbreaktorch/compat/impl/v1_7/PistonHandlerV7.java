package ru.mitriyf.rainbreaktorch.compat.impl.v1_7;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.scheduler.BukkitScheduler;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.compat.abstraction.PistonHandler;
import ru.mitriyf.rainbreaktorch.service.TorchService;

@SuppressWarnings("deprecation")
public class PistonHandlerV7 implements PistonHandler {
    private final RainBreakTorch plugin;
    private final TorchService torchService;
    private final BukkitScheduler scheduler;

    public PistonHandlerV7(RainBreakTorch plugin) {
        this.plugin = plugin;
        torchService = plugin.getTorchService();
        scheduler = plugin.getServer().getScheduler();
    }

    @Override
    public void checkTorch(BlockPistonRetractEvent e) {
        Block block = e.getRetractLocation().getBlock();
        torchService.checkTorch(block, true);
        Block newBlock = e.getBlock().getRelative(e.getDirection());
        scheduler.runTaskLater(plugin, () -> torchService.checkTorch(newBlock, false), 4L);
    }
}

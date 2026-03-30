package ru.mitriyf.rainbreaktorch.utils.piston.impl;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPistonRetractEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.piston.PistonRetract;

@SuppressWarnings("deprecation")
public class RetractVersion7 implements PistonRetract {
    private final RainBreakTorch plugin;
    private final Utils utils;

    public RetractVersion7(RainBreakTorch plugin) {
        this.plugin = plugin;
        utils = plugin.getUtils();
    }

    @Override
    public void checkTorch(BlockPistonRetractEvent e) {
        Block block = e.getRetractLocation().getBlock();
        utils.checkTorch(block, true);
        Block newBlock = e.getBlock().getRelative(e.getDirection());
        utils.getScheduler().runTaskLater(plugin, () -> utils.checkTorch(newBlock, false), 4L);
    }
}

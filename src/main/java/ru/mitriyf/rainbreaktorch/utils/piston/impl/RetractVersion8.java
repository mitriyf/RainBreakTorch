package ru.mitriyf.rainbreaktorch.utils.piston.impl;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPistonRetractEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.piston.PistonRetract;

public class RetractVersion8 implements PistonRetract {
    private final RainBreakTorch plugin;
    private final Utils utils;

    public RetractVersion8(RainBreakTorch plugin) {
        this.plugin = plugin;
        utils = plugin.getUtils();
    }

    @Override
    public void checkTorch(BlockPistonRetractEvent e) {
        BlockFace blockFace = e.getDirection();
        for (Block block : e.getBlocks()) {
            utils.checkTorch(block, true);
            Block newBlock = block.getRelative(blockFace);
            utils.getScheduler().runTaskLater(plugin, () -> utils.checkTorch(newBlock, false), 4L);
        }
    }
}

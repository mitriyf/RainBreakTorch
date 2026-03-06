package ru.mitriyf.rainbreaktorch.utils.piston.impl;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPistonRetractEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.piston.PistonRetract;

public class RetractVersion8 implements PistonRetract {
    private final Utils utils;

    public RetractVersion8(RainBreakTorch plugin) {
        utils = plugin.getUtils();
    }

    @Override
    public void saveTorch(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            utils.saveTorch(block, true);
        }
    }
}

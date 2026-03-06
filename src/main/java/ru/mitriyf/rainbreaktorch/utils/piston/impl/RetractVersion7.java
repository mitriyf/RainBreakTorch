package ru.mitriyf.rainbreaktorch.utils.piston.impl;

import org.bukkit.event.block.BlockPistonRetractEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.piston.PistonRetract;

@SuppressWarnings("deprecation")
public class RetractVersion7 implements PistonRetract {
    private final Utils utils;

    public RetractVersion7(RainBreakTorch plugin) {
        utils = plugin.getUtils();
    }

    @Override
    public void saveTorch(BlockPistonRetractEvent e) {
        utils.saveTorch(e.getRetractLocation().getBlock(), true);
    }
}

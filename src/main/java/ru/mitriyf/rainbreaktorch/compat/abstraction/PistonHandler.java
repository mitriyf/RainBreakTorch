package ru.mitriyf.rainbreaktorch.compat.abstraction;

import org.bukkit.event.block.BlockPistonRetractEvent;

public interface PistonHandler {
    void checkTorch(BlockPistonRetractEvent e);
}

package ru.mitriyf.rainbreaktorch.compat.impl.v1_8;

import org.bukkit.event.block.BlockPistonRetractEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.compat.abstraction.PistonHandler;
import ru.mitriyf.rainbreaktorch.service.TorchService;

public class PistonHandlerV8 implements PistonHandler {
    private final TorchService torchService;

    public PistonHandlerV8(RainBreakTorch plugin) {
        torchService = plugin.getTorchService();
    }

    @Override
    public void checkTorch(BlockPistonRetractEvent e) {
        torchService.checkPistonBlocks(e.getBlocks(), e.getDirection());
    }
}

package ru.mitriyf.rainbreaktorch.listener.compat;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.service.TorchService;

public final class WorldListenerV8 implements Listener {
    private final TorchService torchService;

    public WorldListenerV8(RainBreakTorch plugin) {
        this.torchService = plugin.getTorchService();
    }

    @EventHandler
    public void Explode(BlockExplodeEvent e) {
        for (Block block : e.blockList()) {
            torchService.checkTorch(block, true);
        }
    }
}

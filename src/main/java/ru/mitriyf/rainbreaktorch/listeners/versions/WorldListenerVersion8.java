package ru.mitriyf.rainbreaktorch.listeners.versions;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;

public final class WorldListenerVersion8 implements Listener {
    private final Utils utils;

    public WorldListenerVersion8(RainBreakTorch plugin) {
        this.utils = plugin.getUtils();
    }

    @EventHandler
    public void Explode(BlockExplodeEvent e) {
        for (Block block : e.blockList()) {
            utils.saveTorch(block, true);
        }
    }
}

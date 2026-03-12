package ru.mitriyf.rainbreaktorch.listeners.versions;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;

public class WorldListenerVersion14 implements Listener {
    private final Utils utils;

    public WorldListenerVersion14(RainBreakTorch plugin) {
        this.utils = plugin.getUtils();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFluidLevenChange(FluidLevelChangeEvent e) {
        
        utils.saveTorch(e.getBlock(), true);
    }
}

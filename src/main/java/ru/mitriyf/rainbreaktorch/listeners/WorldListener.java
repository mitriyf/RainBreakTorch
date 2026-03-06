package ru.mitriyf.rainbreaktorch.listeners;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;

public class WorldListener implements Listener {
    private final Utils utils;

    public WorldListener(RainBreakTorch plugin) {
        utils = plugin.getUtils();
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        World world = e.getWorld();
        if (utils.checkRules(world, null)) {
            return;
        }
        utils.getTorchUtils().updateChunks(world);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        utils.saveTorch(e.getBlock(), false);
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent e) {
        if (utils.checkRules(e.getBlock().getWorld(), null)) {
            return;
        }
        for (Block block : e.getBlocks()) {
            utils.saveTorch(block, true);
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent e) {
        if (utils.checkRules(e.getBlock().getWorld(), null)) {
            return;
        }
        utils.getPistonRetract().saveTorch(e);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            utils.saveTorch(block, true);
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        utils.saveTorch(event.getBlockClicked().getRelative(event.getBlockFace()), true);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        utils.saveTorch(e.getBlock(), true);
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        utils.saveTorch(event.getToBlock(), true);
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        utils.saveTorch(e.getBlock(), true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        utils.saveTorch(e.getBlock(), true);
    }
}
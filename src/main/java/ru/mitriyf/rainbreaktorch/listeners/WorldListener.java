package ru.mitriyf.rainbreaktorch.listeners;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.WorldLoadEvent;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class WorldListener implements Listener {
    private final RainBreakTorch plugin;
    private final Values values;
    private final Utils utils;
    private Material redstoneTorchOn, redstoneTorchOff;

    public WorldListener(RainBreakTorch plugin) {
        this.plugin = plugin;
        utils = plugin.getUtils();
        values = plugin.getValues();
        if (plugin.getVersion() < 13) {
            redstoneTorchOn = Material.valueOf("REDSTONE_TORCH_ON");
            redstoneTorchOff = Material.valueOf("REDSTONE_TORCH_OFF");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent e) {
        utils.getTorchUtils().updateChunksAsync(e.getWorld(), e.toWeatherState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent e) {
        String worldName = e.getWorld().getName();
        if (!utils.getTorchUtils().getWorldChunks().containsKey(worldName)) {
            File worldChunksFile = new File(utils.getTorchUtils().getWorlds(), e.getWorld().getName());
            if (!worldChunksFile.exists() && !worldChunksFile.mkdirs()) {
                plugin.getLogger().warning("Error create mkdirs!");
            }
            utils.getTorchUtils().getWorldFiles().put(worldName, worldChunksFile);
            Set<String> chunks = new HashSet<>();
            File[] files = worldChunksFile.listFiles();
            if (files != null) {
                for (File fileName : files) {
                    chunks.add(fileName.getName());
                }
            }
            utils.getTorchUtils().getWorldChunks().putIfAbsent(worldName, chunks);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPopulateChunk(ChunkPopulateEvent e) {
        if (values.isCheckNewChunk()) {
            World world = e.getWorld();
            boolean isStorm = world.hasStorm();
            ChunkSnapshot snapshot = e.getChunk().getChunkSnapshot(true, true, false);
            utils.getScheduler().runTaskAsynchronously(plugin, () -> utils.getTorchUtils().checkChunk(world, snapshot, utils.getMinHeight().get(world), world.getMaxHeight(), isStorm, null));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLoadChunk(ChunkLoadEvent e) {
        utils.getTorchUtils().updateChunkAsync(e.getWorld(), e.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        Material material = block.getType();
        if (values.getRedstoneBlocks().contains(material) && e.getNewCurrent() > 0) {
            if (redstoneTorchOn != null && material == redstoneTorchOff) {
                material = redstoneTorchOn;
            }
            utils.checkTorch(block, material, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        utils.checkTorch(e.getBlock(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent e) {
        BlockFace blockFace = e.getDirection();
        for (Block block : e.getBlocks()) {
            utils.checkTorch(block, true);
            Block newBlock = block.getRelative(blockFace);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> utils.checkTorch(newBlock, false), 4L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent e) {
        utils.getPistonRetract().checkTorch(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            utils.checkTorch(block, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        utils.checkTorch(e.getBlock(), e.getTo() == Material.AIR);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        utils.checkTorch(event.getToBlock(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        utils.checkTorch(e.getBlock(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        utils.checkTorch(e.getBlock(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent e) {
        utils.checkTorch(e.getBlock(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        utils.checkTorch(e.getBlock(), true);
    }
}
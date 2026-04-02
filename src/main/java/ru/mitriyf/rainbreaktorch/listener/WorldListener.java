package ru.mitriyf.rainbreaktorch.listener;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import ru.mitriyf.rainbreaktorch.compat.abstraction.VersionRules;
import ru.mitriyf.rainbreaktorch.service.TorchService;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class WorldListener implements Listener {
    private final TorchService torchService;
    private final VersionRules versionRules;
    private final RainBreakTorch plugin;
    private final Values values;
    private final Logger logger;
    private final Utils utils;

    public WorldListener(RainBreakTorch plugin) {
        this.plugin = plugin;
        utils = plugin.getUtils();
        values = plugin.getValues();
        logger = plugin.getLogger();
        versionRules = utils.getVersionRules();
        torchService = plugin.getTorchService();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent e) {
        torchService.updateChunksAsync(e.getWorld(), e.toWeatherState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent e) {
        String worldName = e.getWorld().getName();
        if (!torchService.getWorldChunks().containsKey(worldName)) {
            File worldChunksFile = new File(torchService.getWorlds(), e.getWorld().getName());
            if (!worldChunksFile.exists() && !worldChunksFile.mkdirs()) {
                logger.warning("Error create mkdirs!");
            }
            torchService.getWorldFiles().put(worldName, worldChunksFile);
            Set<String> chunks = new HashSet<>();
            File[] files = worldChunksFile.listFiles();
            if (files != null) {
                for (File fileName : files) {
                    chunks.add(fileName.getName());
                }
            }
            torchService.getWorldChunks().putIfAbsent(worldName, chunks);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPopulateChunk(ChunkPopulateEvent e) {
        if (values.isCheckNewChunk()) {
            World world = e.getWorld();
            boolean isStorm = world.hasStorm();
            ChunkSnapshot snapshot = e.getChunk().getChunkSnapshot(true, true, false);
            utils.getScheduler().runTaskAsynchronously(plugin, () -> torchService.checkChunk(world, snapshot, utils.getHeightProvider().get(world), world.getMaxHeight(), isStorm, null));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLoadChunk(ChunkLoadEvent e) {
        torchService.updateChunkAsync(e.getWorld(), e.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        Material material = block.getType();
        if (values.getRedstoneBlocks().contains(material) && e.getNewCurrent() > 0) {
            material = versionRules.getRedstoneType(material);
            torchService.checkTorch(block, material, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        torchService.checkTorch(e.getBlock(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent e) {
        torchService.checkPistonBlocks(e.getBlocks(), e.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent e) {
        utils.getPistonHandler().checkTorch(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            torchService.checkTorch(block, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        torchService.checkTorch(e.getBlock(), e.getTo() == Material.AIR);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        torchService.checkTorch(event.getToBlock(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        torchService.checkTorch(e.getBlock(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        torchService.checkTorch(e.getBlock(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent e) {
        torchService.checkTorch(e.getBlock(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        torchService.checkTorch(e.getBlock(), true);
    }
}
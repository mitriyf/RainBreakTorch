package ru.mitriyf.rainbreaktorch.service;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.compat.abstraction.BlockSetter;
import ru.mitriyf.rainbreaktorch.compat.abstraction.VersionRules;
import ru.mitriyf.rainbreaktorch.model.DropData;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class DropTaskService extends BukkitRunnable {
    private final BlockFace[] faces = {BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN};
    private final Queue<DropData> dataList = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final RainBreakTorch plugin;
    private final BlockSetter blockSetter;
    private final Values values;
    private final VersionRules versionRules;
    private long disableTime = 0;

    public DropTaskService(RainBreakTorch plugin) {
        this.plugin = plugin;
        values = plugin.getValues();
        versionRules = plugin.getUtils().getVersionRules();
        blockSetter = plugin.getUtils().getBlockSetter();
    }

    @Override
    public void run() {
        if (!active.get()) {
            return;
        } else if (dataList.isEmpty()) {
            active.set(false);
            return;
        }
        long start = System.currentTimeMillis();
        boolean disableDrop = start < disableTime;
        int i = 0;
        while (i < values.getBreakLimit()) {
            DropData data = dataList.poll();
            if (data == null) {
                break;
            }
            drop(data, !disableDrop);
            i++;
        }
        long end = System.currentTimeMillis();
        if (end - start > values.getMsLimit()) {
            disableTime = end + values.getMsDelay();
        }
    }

    public void drop(DropData dropData, boolean drop) {
        World world = dropData.getWorld();
        Block block = world.getBlockAt(dropData.getX(), dropData.getY(), dropData.getZ());
        Material material = block.getType();
        if (isNoTorch(block, material)) {
            return;
        }
        updatePhysics(world, block);
        if (drop) {
            plugin.getLootService().dropLoot(world, block.getLocation(), material);
        }
    }

    private void updatePhysics(World world, Block block) {
        if (values.isPhysicsEnabled()) {
            if (values.isPhysicsFull()) {
                blockSetter.setAir(block, true);
            } else {
                updateNeighbors(world, block);
            }
        } else {
            blockSetter.setAir(block, false);
        }
    }

    private void updateNeighbors(World world, Block block) {
        blockSetter.setAir(block, false);
        for (BlockFace face : faces) {
            int neighborX = block.getX() + face.getModX();
            int neighborY = block.getY() + face.getModY();
            int neighborZ = block.getZ() + face.getModZ();
            int chunkX = neighborX >> 4;
            int chunkZ = neighborZ >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                if (!world.loadChunk(chunkX, chunkZ, false)) {
                    continue;
                }
            }
            Block neighbor = world.getBlockAt(neighborX, neighborY, neighborZ);
            Material neighborType = neighbor.getType();
            if (neighborType == Material.WATER || neighborType == Material.LAVA || neighborType.hasGravity()) {
                BlockState neighborData = neighbor.getState();
                blockSetter.setAir(neighbor, false);
                neighborData.update(true, true);
            }
        }
    }

    private boolean isNoTorch(Block block, Material material) {
        if (!values.getTorchesBlocks().contains(material)) {
            return true;
        }
        return versionRules.isNoTorch(block, material);
    }
}

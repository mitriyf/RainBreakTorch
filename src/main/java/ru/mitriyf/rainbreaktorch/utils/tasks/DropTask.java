package ru.mitriyf.rainbreaktorch.utils.tasks;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.block.SetType;
import ru.mitriyf.rainbreaktorch.utils.common.rules.Rules;
import ru.mitriyf.rainbreaktorch.utils.tasks.data.DropData;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class DropTask extends BukkitRunnable {
    private final BlockFace[] faces = {BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN};
    private final ConcurrentLinkedQueue<DropData> dataList = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final RainBreakTorch plugin;
    private final SetType setType;
    private final Values values;
    private final Material air;
    private final Rules rules;
    private long disableTime = 0;

    public DropTask(RainBreakTorch plugin) {
        air = Material.AIR;
        this.plugin = plugin;
        values = plugin.getValues();
        rules = plugin.getUtils().getRules();
        setType = plugin.getUtils().getSetType();
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
        for (int i = 0; i < values.getBreakLimit(); i++) {
            DropData data = dataList.poll();
            if (data == null) {
                break;
            }
            drop(data, !disableDrop);
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
            plugin.getLootManager().dropLoot(world, block.getLocation(), material);
        }
    }

    private void updatePhysics(World world, Block block) {
        if (values.isPhysicsEnabled()) {
            if (values.isPhysicsFull()) {
                setType.setAir(block, true);
            } else {
                updateNeighbors(world, block);
            }
        } else {
            setType.setAir(block, false);
        }
    }

    private void updateNeighbors(World world, Block block) {
        setType.setAir(block, false);
        for (BlockFace face : faces) {
            int neighborX = block.getX() + face.getModX();
            int neighborY = block.getY() + face.getModY();
            int neighborZ = block.getZ() + face.getModZ();
            int chunkX = neighborX >> 4;
            int chunkZ = neighborZ >> 4;
            if (!world.loadChunk(chunkX, chunkZ, false)) {
                continue;
            }
            Block neighbor = world.getBlockAt(neighborX, neighborY, neighborZ);
            if (neighbor.isLiquid() || neighbor.getType().hasGravity()) {
                BlockState neighborData = neighbor.getState();
                setType.setAir(neighbor, false);
                neighborData.update(true, true);
            }
        }
    }

    private boolean isNoTorch(Block block, Material material) {
        if (!values.getTorchesBlocks().contains(material)) {
            return true;
        }
        return rules.isNoTorch(block, material);
    }
}

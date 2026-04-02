package ru.mitriyf.rainbreaktorch.compat.impl.v1_13;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import ru.mitriyf.rainbreaktorch.compat.abstraction.BlockSetter;

public class BlockSetterV13 implements BlockSetter {
    private final BlockData blockData = Material.AIR.createBlockData();

    @Override
    public void setAir(Block block, boolean applyPhysics) {
        block.setBlockData(blockData, applyPhysics);
    }
}

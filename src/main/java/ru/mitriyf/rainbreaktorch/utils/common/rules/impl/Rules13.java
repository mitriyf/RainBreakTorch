package ru.mitriyf.rainbreaktorch.utils.common.rules.impl;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Waterlogged;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.common.rules.Rules;

public class Rules13 implements Rules {
    private final Utils utils;

    public Rules13(Utils utils) {
        this.utils = utils;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isNoTorch(Block block, Material material) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged()) {
            return true;
        } else if (blockData instanceof Lightable && !((Lightable) blockData).isLit()) {
            return true;
        } else if (utils.getValues().getRedstoneBlocks().contains(material)) {
            return block.getData() == 0;
        }
        return false;
    }

    @Override
    public Material getBlockType(ChunkSnapshot snapshot, int x, int y, int z) {
        return snapshot.getBlockType(x, y, z);
    }

    @Override
    public Biome getBiome(ChunkSnapshot snapshot, int x, int y, int z) {
        return snapshot.getBiome(x, y, z);
    }
}

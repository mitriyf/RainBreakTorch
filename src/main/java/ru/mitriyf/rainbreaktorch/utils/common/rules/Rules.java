package ru.mitriyf.rainbreaktorch.utils.common.rules;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

public interface Rules {
    boolean isNoTorch(Block block, Material material);

    Material getBlockType(ChunkSnapshot snapshot, int x, int y, int z);

    Biome getBiome(ChunkSnapshot snapshot, int x, int y, int z);
}

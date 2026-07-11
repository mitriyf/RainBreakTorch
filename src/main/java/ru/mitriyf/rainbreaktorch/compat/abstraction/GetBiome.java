package ru.mitriyf.rainbreaktorch.compat.abstraction;

import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Biome;

public interface GetBiome {
    Biome get(ChunkSnapshot snapshot, int x, int y, int z);
}

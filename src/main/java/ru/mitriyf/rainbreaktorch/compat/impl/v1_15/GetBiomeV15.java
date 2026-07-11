package ru.mitriyf.rainbreaktorch.compat.impl.v1_15;

import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Biome;
import ru.mitriyf.rainbreaktorch.compat.abstraction.GetBiome;

public class GetBiomeV15 implements GetBiome {
    @Override
    public Biome get(ChunkSnapshot snapshot, int x, int y, int z) {
        return snapshot.getBiome(x, y, z);
    }
}

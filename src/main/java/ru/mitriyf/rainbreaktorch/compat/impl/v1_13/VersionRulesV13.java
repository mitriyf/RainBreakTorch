package ru.mitriyf.rainbreaktorch.compat.impl.v1_13;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.*;
import ru.mitriyf.rainbreaktorch.compat.abstraction.VersionRules;
import ru.mitriyf.rainbreaktorch.values.Values;

public class VersionRulesV13 implements VersionRules {
    private final Values values;

    public VersionRulesV13(Values values) {
        this.values = values;
    }

    @Override
    public boolean isNoTorch(Block block, Material material) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged()) {
            return true;
        }
        if (blockData instanceof Lightable && !((Lightable) blockData).isLit()) {
            return true;
        }
        if (values.getRedstoneBlocks().contains(material)) {
            if (blockData instanceof AnaloguePowerable && ((AnaloguePowerable) blockData).getPower() == 0) {
                return true;
            } else return blockData instanceof Powerable && !((Powerable) blockData).isPowered();
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

    @Override
    public Material getRedstoneType(Material material) {
        return material;
    }
}

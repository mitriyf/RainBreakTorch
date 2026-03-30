package ru.mitriyf.rainbreaktorch.utils.common.rules.impl;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.common.rules.Rules;

import java.lang.reflect.Method;

@SuppressWarnings("all")
public class Rules12 implements Rules {
    private final Material redstoneTorchOff, redstoneTorchOn;
    private final Utils utils;
    private Method getBlockTypeId, getMaterialById;

    public Rules12(Utils utils) {
        this.utils = utils;
        redstoneTorchOff = Material.valueOf("REDSTONE_TORCH_OFF");
        redstoneTorchOn = Material.valueOf("REDSTONE_TORCH_ON");
        try {
            getBlockTypeId = ChunkSnapshot.class.getDeclaredMethod("getBlockTypeId", int.class, int.class, int.class);
            getMaterialById = Material.class.getDeclaredMethod("getMaterial", int.class);
        } catch (Exception e) {
            utils.getLogger().warning("Not found method getBlockTypeId: " + e);
        }
    }

    @Override
    public boolean isNoTorch(Block block, Material material) {
        if (utils.getValues().getRedstoneBlocks().contains(material)) {
            return block.getData() == 0 || material == redstoneTorchOff;
        }
        return false;
    }

    @Override
    public Material getBlockType(ChunkSnapshot snapshot, int x, int y, int z) {
        try {
            int id = (int) getBlockTypeId.invoke(snapshot, x, y, z);
            return (Material) getMaterialById.invoke(null, id);
        } catch (Exception e) {
            utils.getLogger().warning("Error getBlockType: " + e);
        }
        return null;
    }

    @Override
    public Biome getBiome(ChunkSnapshot snapshot, int x, int y, int z) {
        return snapshot.getBiome(x, z);
    }
}
package ru.mitriyf.rainbreaktorch.compat.impl.v1_12;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import ru.mitriyf.rainbreaktorch.compat.abstraction.VersionRules;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("all")
public class VersionRulesV12 implements VersionRules {
    private final Map<Material, Material> redstoneMap = new HashMap<>();
    private final Set<Material> offMaterialList = new HashSet<>();
    private final Set<Material> onMaterialList = new HashSet<>();
    private final Values values;
    private Method getBlockTypeId, getMaterialById;

    public VersionRulesV12(Values values) {
        this.values = values;
        put("REDSTONE_TORCH_OFF", "REDSTONE_TORCH_ON");
        put("REDSTONE_LAMP_OFF", "REDSTONE_LAMP_ON");
        put("DIODE_BLOCK_OFF", "DIODE_BLOCK_ON");
        try {
            getBlockTypeId = ChunkSnapshot.class.getDeclaredMethod("getBlockTypeId", int.class, int.class, int.class);
            getMaterialById = Material.class.getDeclaredMethod("getMaterial", int.class);
        } catch (Exception e) {
            values.getLogger().warning("Not found method getBlockTypeId. Error: " + e);
        }
    }

    private void put(String offMaterialString, String onMaterialString) {
        try {
            Material offMaterial = Material.valueOf(offMaterialString);
            Material onMaterial = Material.valueOf(onMaterialString);
            redstoneMap.put(offMaterial, onMaterial);
            offMaterialList.add(offMaterial);
            onMaterialList.add(onMaterial);
        } catch (Exception e) {
            values.getLogger().info("[REDSTONE] Block not found: " + offMaterialString + " | " + onMaterialString);
        }
    }

    @Override
    public boolean isNoTorch(Block block, Material material) {
        if (values.getRedstoneBlocks().contains(material)) {
            byte data = block.getData();
            if (onMaterialList.contains(material)) {
                return false;
            } else if (offMaterialList.contains(material)) {
                return true;
            } else if (material == Material.REDSTONE_WIRE) {
                return data == 0;
            }
            return (data & 0x8) == 0;
        }
        return false;
    }

    @Override
    public Material getBlockType(ChunkSnapshot snapshot, int x, int y, int z) {
        try {
            int id = (int) getBlockTypeId.invoke(snapshot, x, y, z);
            return (Material) getMaterialById.invoke(null, id);
        } catch (Exception e) {
            values.getLogger().warning("Error getBlockType: " + e);
        }
        return null;
    }

    @Override
    public Biome getBiome(ChunkSnapshot snapshot, int x, int y, int z) {
        return snapshot.getBiome(x, z);
    }

    @Override
    public Material getRedstoneType(Material material) {
        Material newMaterial = redstoneMap.get(material);
        return newMaterial != null ? newMaterial : material;
    }
}
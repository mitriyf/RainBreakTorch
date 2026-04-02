package ru.mitriyf.rainbreaktorch.filter.world.impl;

import org.bukkit.World;
import org.bukkit.block.Biome;
import ru.mitriyf.rainbreaktorch.filter.world.BiomesList;
import ru.mitriyf.rainbreaktorch.filter.world.WorldsList;
import ru.mitriyf.rainbreaktorch.values.Values;

public class BlockedWorlds implements WorldsList, BiomesList {
    private final Values values;

    public BlockedWorlds(Values values) {
        this.values = values;
    }

    @Override
    public boolean notContainsWorld(World world) {
        return values.getWorlds().contains(world);
    }

    @Override
    public boolean notContainsBiome(Biome biome) {
        return values.getBiomes().contains(biome);
    }
}

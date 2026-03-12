package ru.mitriyf.rainbreaktorch.utils.worlds.impl;

import org.bukkit.World;
import org.bukkit.block.Biome;
import ru.mitriyf.rainbreaktorch.utils.worlds.BiomesList;
import ru.mitriyf.rainbreaktorch.utils.worlds.WorldsList;
import ru.mitriyf.rainbreaktorch.values.Values;

public class BlockedWorlds implements WorldsList, BiomesList {
    private final Values values;

    public BlockedWorlds(Values values) {
        this.values = values;
    }

    @Override
    public boolean notContainsWorld(World world) {
        return values.getWorlds().contains(world.getName());
    }

    @Override
    public boolean notContainsBiome(Biome biome) {
        return values.getBiomes().contains(biome);
    }
}

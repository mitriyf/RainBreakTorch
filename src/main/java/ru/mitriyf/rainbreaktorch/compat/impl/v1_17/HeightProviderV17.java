package ru.mitriyf.rainbreaktorch.compat.impl.v1_17;

import org.bukkit.World;
import ru.mitriyf.rainbreaktorch.compat.abstraction.HeightProvider;

public class HeightProviderV17 implements HeightProvider {
    @Override
    public int get(World world) {
        return world.getMinHeight();
    }
}

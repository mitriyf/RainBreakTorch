package ru.mitriyf.rainbreaktorch.utils.common.height.impl;

import org.bukkit.World;
import ru.mitriyf.rainbreaktorch.utils.common.height.MinHeight;

public class MinHeight17 implements MinHeight {
    @Override
    public int get(World world) {
        return world.getMinHeight();
    }
}

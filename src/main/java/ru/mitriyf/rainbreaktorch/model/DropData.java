package ru.mitriyf.rainbreaktorch.model;

import lombok.Getter;
import org.bukkit.World;

@Getter
public class DropData {
    private final World world;
    private final int x;
    private final int y;
    private final int z;

    public DropData(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

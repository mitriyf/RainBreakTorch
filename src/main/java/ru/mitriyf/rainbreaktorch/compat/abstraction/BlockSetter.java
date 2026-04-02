package ru.mitriyf.rainbreaktorch.compat.abstraction;

import org.bukkit.block.Block;

public interface BlockSetter {
    void setAir(Block block, boolean applyPhysics);
}

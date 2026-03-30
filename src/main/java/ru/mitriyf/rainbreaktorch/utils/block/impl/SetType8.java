package ru.mitriyf.rainbreaktorch.utils.block.impl;

import org.bukkit.Material;
import org.bukkit.block.Block;
import ru.mitriyf.rainbreaktorch.utils.block.SetType;

public class SetType8 implements SetType {
    @Override
    public void setAir(Block block, boolean applyPhysics) {
        block.setType(Material.AIR, applyPhysics);
    }
}

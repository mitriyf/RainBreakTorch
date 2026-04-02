package ru.mitriyf.rainbreaktorch.filter.type.impl;

import org.bukkit.Material;
import ru.mitriyf.rainbreaktorch.filter.type.CheckType;

public class IsSolidCheckType implements CheckType {
    @Override
    public boolean isValidMaterial(Material material) {
        return material.isSolid();
    }
}

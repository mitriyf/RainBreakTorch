package ru.mitriyf.rainbreaktorch.utils.checks.type.impl;

import org.bukkit.Material;
import ru.mitriyf.rainbreaktorch.utils.checks.type.CheckType;

public class IsOccluding implements CheckType {
    @Override
    public boolean isValidMaterial(Material material) {
        return material.isOccluding();
    }
}

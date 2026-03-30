package ru.mitriyf.rainbreaktorch.utils.checks.blocks.impl;

import org.bukkit.Material;
import ru.mitriyf.rainbreaktorch.utils.checks.blocks.TypeList;
import ru.mitriyf.rainbreaktorch.values.Values;

public class OtherList implements TypeList {
    private final Values values;

    public OtherList(Values values) {
        this.values = values;
    }

    @Override
    public boolean isValid(Material material) {
        return values.getCheckType().isValidMaterial(material) || values.getListSafeBlocks().contains(material);
    }
}

package ru.mitriyf.rainbreaktorch.utils.checks.blocks.impl;

import org.bukkit.Material;
import ru.mitriyf.rainbreaktorch.utils.checks.blocks.TypeList;
import ru.mitriyf.rainbreaktorch.values.Values;

public class MultiList implements TypeList {
    private final Values values;

    public MultiList(Values values) {
        this.values = values;
    }

    @Override
    public boolean isValid(Material material) {
        return values.getListSafeBlocks().contains(material) || (values.getCheckType().isValidMaterial(material) && !values.getBlackListSafeBlocks().contains(material));
    }
}

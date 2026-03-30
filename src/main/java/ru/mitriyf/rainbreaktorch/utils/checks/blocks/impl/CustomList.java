package ru.mitriyf.rainbreaktorch.utils.checks.blocks.impl;

import org.bukkit.Material;
import ru.mitriyf.rainbreaktorch.utils.checks.blocks.TypeList;
import ru.mitriyf.rainbreaktorch.values.Values;

public class CustomList implements TypeList {
    private final Values values;

    public CustomList(Values values) {
        this.values = values;
    }

    @Override
    public boolean isValid(Material material) {
        return values.getListSafeBlocks().contains(material);
    }
}

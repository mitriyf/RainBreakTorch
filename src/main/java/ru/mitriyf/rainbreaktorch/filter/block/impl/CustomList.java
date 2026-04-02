package ru.mitriyf.rainbreaktorch.filter.block.impl;

import org.bukkit.Material;
import ru.mitriyf.rainbreaktorch.filter.block.TypeList;
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

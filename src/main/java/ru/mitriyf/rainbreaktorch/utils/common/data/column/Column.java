package ru.mitriyf.rainbreaktorch.utils.common.data.column;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Column {
    public final Set<Integer> blocks = new HashSet<>();
    public final Map<Integer, Boolean> torches = new HashMap<>();

    public boolean isEmpty() {
        return blocks.isEmpty() && torches.isEmpty();
    }

    public Column cloneColumn() {
        Column copy = new Column();
        copy.blocks.addAll(this.blocks);
        copy.torches.putAll(this.torches);
        return copy;
    }
}
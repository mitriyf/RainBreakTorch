package ru.mitriyf.rainbreaktorch.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Column {
    public final Set<Integer> blocks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final Map<Integer, Boolean> torches = new ConcurrentHashMap<>();

    public boolean isEmpty() {
        return blocks.isEmpty() && torches.isEmpty();
    }

    public Column cloneColumn() {
        Column copy = new Column();
        synchronized (this) {
            copy.blocks.addAll(this.blocks);
            copy.torches.putAll(this.torches);
            return copy;
        }
    }
}
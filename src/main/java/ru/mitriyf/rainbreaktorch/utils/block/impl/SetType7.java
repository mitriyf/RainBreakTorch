package ru.mitriyf.rainbreaktorch.utils.block.impl;

import org.bukkit.block.Block;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.block.SetType;

import java.lang.reflect.Method;

@SuppressWarnings("all")
public class SetType7 implements SetType {
    private final Utils utils;
    private Method setTypeId;

    public SetType7(Utils utils) {
        this.utils = utils;
        try {
            setTypeId = Block.class.getDeclaredMethod("setTypeId", int.class, boolean.class);
        } catch (Exception e) {
            utils.getLogger().warning("Not found method setTypeId: " + e);
        }
    }

    @Override
    public void setAir(Block block, boolean applyPhysics) {
        try {
            setTypeId.invoke(block, 0, applyPhysics);
        } catch (Exception e) {
            utils.getLogger().warning("Error getBlockType: " + e);
        }
    }
}

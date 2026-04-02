package ru.mitriyf.rainbreaktorch.compat.impl.v1_12;

import org.bukkit.block.Block;
import ru.mitriyf.rainbreaktorch.compat.abstraction.BlockSetter;

import java.lang.reflect.Method;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class BlockSetterV12 implements BlockSetter {
    private final Logger logger;
    private Method setTypeId;

    public BlockSetterV12(Logger logger) {
        this.logger = logger;
        try {
            setTypeId = Block.class.getDeclaredMethod("setTypeId", int.class, boolean.class);
        } catch (Exception e) {
            logger.warning("Not found method setTypeId: " + e);
        }
    }

    @Override
    public void setAir(Block block, boolean applyPhysics) {
        try {
            setTypeId.invoke(block, 0, applyPhysics);
        } catch (Exception e) {
            logger.warning("Error getBlockType: " + e);
        }
    }
}

package ru.mitriyf.rainbreaktorch.compat.impl.v1_9;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.mitriyf.rainbreaktorch.compat.abstraction.HandInquisitor;

public class HandInquisitorV9 implements HandInquisitor {
    @Override
    public ItemStack getItemInHand(Player player) {
        return player.getInventory().getItemInMainHand();
    }
}

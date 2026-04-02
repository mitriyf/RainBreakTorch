package ru.mitriyf.rainbreaktorch.compat.impl.v1_8;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.mitriyf.rainbreaktorch.compat.abstraction.HandInquisitor;

@SuppressWarnings("deprecation")
public class HandInquisitorV8 implements HandInquisitor {
    @Override
    public ItemStack getItemInHand(Player player) {
        return player.getInventory().getItemInHand();
    }
}

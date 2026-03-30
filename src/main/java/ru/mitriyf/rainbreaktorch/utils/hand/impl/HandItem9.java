package ru.mitriyf.rainbreaktorch.utils.hand.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.mitriyf.rainbreaktorch.utils.hand.HandItem;

public class HandItem9 implements HandItem {
    @Override
    public ItemStack getItemInHand(Player player) {
        return player.getInventory().getItemInMainHand();
    }
}

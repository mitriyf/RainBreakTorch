package ru.mitriyf.rainbreaktorch.utils.hand.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.mitriyf.rainbreaktorch.utils.hand.HandItem;

@SuppressWarnings("deprecation")
public class HandItem8 implements HandItem {
    @Override
    public ItemStack getItemInHand(Player player) {
        return player.getInventory().getItemInHand();
    }
}

package ru.mitriyf.rainbreaktorch.utils.hand;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface HandItem {
    ItemStack getItemInHand(Player player);
}

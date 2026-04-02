package ru.mitriyf.rainbreaktorch.compat.abstraction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface HandInquisitor {
    ItemStack getItemInHand(Player player);
}

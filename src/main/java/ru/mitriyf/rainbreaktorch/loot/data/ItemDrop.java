package ru.mitriyf.rainbreaktorch.loot.data;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public class ItemDrop {
    private final int maxAmount, minAmount, chance;
    private final ThreadLocalRandom random;
    private final double addX, addY, addZ;
    private final Material torchMaterial;
    private final String itemName;
    private final ItemStack item;

    public ItemDrop(Material torchMaterial, String itemName, ItemStack item, double addX, double addY, double addZ, int chance, int maxAmount, int minAmount, ThreadLocalRandom random) {
        this.addX = addX;
        this.addY = addY;
        this.addZ = addZ;
        this.item = item;
        this.random = random;
        this.chance = chance;
        this.itemName = itemName;
        this.maxAmount = maxAmount;
        this.minAmount = minAmount;
        this.torchMaterial = torchMaterial;
    }

    public ItemStack generateItem() {
        item.setAmount(getRandomAmount());
        return item;
    }

    private int getRandomAmount() {
        try {
            if ((maxAmount - minAmount) < 0) {
                return maxAmount;
            }
            return minAmount + random.nextInt(maxAmount - minAmount);
        } catch (Exception ignored) {
            return 1;
        }
    }
}

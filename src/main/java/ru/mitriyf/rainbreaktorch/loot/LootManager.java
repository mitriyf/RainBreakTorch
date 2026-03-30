package ru.mitriyf.rainbreaktorch.loot;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.loot.data.ItemDrop;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

@Getter
@SuppressWarnings("DataFlowIssue")
public final class LootManager {
    private final Map<Material, Set<ItemDrop>> items = new HashMap<>();
    private final Set<ItemDrop> defaultItems = new HashSet<>();
    private final ThreadLocalRandom random;
    private final RainBreakTorch plugin;
    private final Logger logger;
    private final Values values;
    private final Utils utils;

    public LootManager(RainBreakTorch plugin) {
        this.plugin = plugin;
        utils = plugin.getUtils();
        logger = plugin.getLogger();
        values = plugin.getValues();
        random = plugin.getRandom();
    }

    public void setup() {
        items.clear();
        defaultItems.clear();
        YamlConfiguration loot = values.getLoot();
        ConfigurationSection lootSection = loot.getConfigurationSection("loot");
        if (lootSection == null) {
            lootSection = loot.createSection("loot");
        }
        ConfigurationSection defaultLoot = loot.getConfigurationSection("default");
        if (defaultLoot == null) {
            defaultLoot = loot.createSection("default");
        }
        generateItems(null, defaultLoot);
        for (Material torchMaterial : values.getTorchesBlocks()) {
            String torchName = torchMaterial.name();
            ConfigurationSection torchSection = lootSection.getConfigurationSection(torchName);
            if (torchSection == null) {
                torchSection = lootSection.createSection(torchName);
            }
            try {
                generateItems(torchMaterial, torchSection);
                Set<ItemDrop> itemDrops = items.get(torchMaterial);
                if (itemDrops == null) {
                    itemDrops = new HashSet<>();
                }
                itemDrops.addAll(defaultItems);
                items.put(torchMaterial, itemDrops);
            } catch (Exception e) {
                logger.warning("There is no such Material. Error: " + e);
            }
        }
    }

    private void generateItems(Material torchMaterial, ConfigurationSection torchSection) {
        for (String itemName : torchSection.getKeys(false)) {
            ConfigurationSection itemSection = torchSection.getConfigurationSection(itemName);
            generateItem(torchMaterial, itemName, itemSection);
        }
    }

    private void generateItem(Material torchMaterial, String itemName, ConfigurationSection itemSection) {
        int amountMax = 1;
        int amountMin = 1;
        try {
            Object amountObject = itemSection.get("amount");
            if (amountObject instanceof Integer) {
                Integer amount = (Integer) amountObject;
                amountMax = amount;
                amountMin = amount;
            } else {
                String[] args = ((String) amountObject).split("-");
                amountMax = Integer.parseInt(args[1]);
                amountMin = Integer.parseInt(args[0]);
                if (amountMax < amountMin) {
                    logger.warning("Item-Id: " + itemName + "\nError: amountMax cannot be less than amountMin");
                    return;
                }
            }
        } catch (Exception e) {
            logger.warning("The amount of the item is incorrect. Fix it in loot.yml. Item-id: " + torchMaterial + "." + itemName + "\nError: " + e);
        }
        try {
            Object itemObject = itemSection.get("item");
            ItemStack stack;
            if (itemObject instanceof String) {
                stack = new ItemStack(Material.valueOf((String) itemObject));
            } else {
                stack = new ItemStack(itemSection.getItemStack("item"));
            }
            ConfigurationSection locationSection = itemSection.getConfigurationSection("location");
            double addX = 0.5, addY = 0, addZ = 0.5;
            if (locationSection != null) {
                addX = locationSection.getDouble("addX");
                addY = locationSection.getDouble("addY");
                addZ = locationSection.getDouble("addZ");
            }
            int chance = itemSection.getInt("chance");
            ItemDrop itemDrop = new ItemDrop(torchMaterial, itemName, stack, addX, addY, addZ, chance, amountMax, amountMin, random);
            if (torchMaterial == null) {
                defaultItems.add(itemDrop);
            } else {
                Set<ItemDrop> itemDrops = items.get(torchMaterial);
                if (itemDrops == null) {
                    itemDrops = new HashSet<>();
                }
                itemDrops.add(itemDrop);
                items.put(torchMaterial, itemDrops);
            }
        } catch (Exception e) {
            logger.warning("The item is defective. Maybe chance or Material. Item-id: " + (torchMaterial == null ? "default" : torchMaterial) + "." + itemName + "\nError: " + e);
        }
    }

    public void dropLoot(World world, Location location, Material torchMaterial) {
        Set<ItemDrop> itemDrops = items.get(torchMaterial);
        if (itemDrops == null) {
            return;
        }
        for (ItemDrop itemDrop : itemDrops) {
            int chance = random.nextInt(101);
            if (itemDrop.getChance() >= chance) {
                try {
                    ItemStack itemStack = itemDrop.generateItem();
                    world.dropItem(location.add(itemDrop.getAddX(), itemDrop.getAddY(), itemDrop.getAddZ()), itemStack);
                } catch (Throwable e) {
                    Material torchName = itemDrop.getTorchMaterial();
                    logger.warning("The item is defective. Item-id: " + (torchName == null ? "default" : torchName) + "." + itemDrop.getItemName());
                }
            }
        }
    }
}

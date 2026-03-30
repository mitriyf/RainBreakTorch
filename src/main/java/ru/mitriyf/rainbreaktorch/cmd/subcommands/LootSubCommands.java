package ru.mitriyf.rainbreaktorch.cmd.subcommands;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

@SuppressWarnings("DataFlowIssue")
public class LootSubCommands {
    private final RainBreakTorch plugin;
    private final Values values;
    private final Logger logger;
    private final Utils utils;

    public LootSubCommands(RainBreakTorch plugin, Values values, Utils utils) {
        this.utils = utils;
        this.plugin = plugin;
        this.values = values;
        logger = plugin.getLogger();
    }

    public void checkLootSubCommands(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendHelp(sender);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "add": {
                addLoot(sender, args);
                return;
            }
            case "list": {
                listLoot(sender, args);
                return;
            }
            case "get": {
                getLoot(sender, args);
                return;
            }
            case "remove": {
                removeLoot(sender, args);
                return;
            }
            default: {
                sendHelp(sender);
            }
        }
    }

    private void addLoot(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            logger.warning("§cYou must be a player to add a drop.");
            return;
        }
        Player player = (Player) sender;
        if (args.length < 6 || args.length > 9) {
            player.sendMessage("§c/rainbreaktorch loot add default/MaterialTorch Name Amount(from-to) Chance AddX AddY AddZ - §fAdd an item to gifts");
            return;
        }
        ItemStack itemStack = utils.getHandItem().getItemInHand(player);
        if (itemStack.getType() == Material.AIR) {
            player.sendMessage("§cYou don't have an item in your hand.");
            return;
        }
        FileConfiguration loot = values.getLoot();
        ConfigurationSection torchSection = getTorchSection(sender, loot, args[2].toUpperCase());
        if (torchSection == null) {
            return;
        }
        String itemName = args[3];
        if (torchSection.contains(itemName)) {
            sender.sendMessage("§cSuch an item already exists!");
            return;
        }
        ConfigurationSection itemSection = torchSection.getConfigurationSection(itemName);
        if (itemSection == null) {
            itemSection = torchSection.createSection(itemName);
        }
        if (isBadItem(player, itemStack, torchSection, itemName, itemSection, args) || isBadLocation(player, torchSection, itemName, itemSection, args)) {
            return;
        }
        saveLoot(loot);
        values.setup();
        player.sendMessage("§aSuccessfully!");
    }

    private boolean isBadItem(Player player, ItemStack itemStack, ConfigurationSection torchSection, String itemName, ConfigurationSection itemSection, String[] args) {
        itemSection.set("item", itemStack);
        String args4 = args[4];
        String[] s = args4.split("-");
        if (s.length == 1) {
            try {
                itemSection.set("amount", utils.formatInt(args4));
            } catch (Exception e) {
                player.sendMessage("§cEnter in amount a number or use the format 1-2\nError: " + e);
                torchSection.set(itemName, null);
                return true;
            }
        } else if (s.length == 2) {
            itemSection.set("amount", args4);
        } else {
            player.sendMessage("§cEnter in amount a number or use the format 1-2");
            torchSection.set(itemName, null);
            return true;
        }
        try {
            itemSection.set("chance", utils.formatInt(args[5]));
        } catch (Exception e) {
            player.sendMessage("§cEnter in amount a number or use the format 1-2\nError: " + e);
            torchSection.set(itemName, null);
            return true;
        }
        return false;
    }

    private boolean isBadLocation(Player player, ConfigurationSection torchSection, String itemName, ConfigurationSection itemSection, String[] args) {
        ConfigurationSection locationSection = itemSection.getConfigurationSection("location");
        if (locationSection == null) {
            locationSection = itemSection.createSection("location");
        }
        try {
            double addX, addY, addZ;
            addX = args[6] == null ? 0.5 : Double.parseDouble(args[6]);
            addY = args[7] == null ? 0 : Double.parseDouble(args[7]);
            addZ = args[8] == null ? 0.5 : Double.parseDouble(args[8]);
            locationSection.set("addX", addX);
            locationSection.set("addY", addY);
            locationSection.set("addZ", addZ);
        } catch (Exception e) {
            player.sendMessage("§cYou specified an incorrect Double!");
            torchSection.set(itemName, null);
            return true;
        }
        return false;
    }

    private void listLoot(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§c/rainbreaktorch loot list MaterialTorch - §fGet a list of items");
            return;
        }
        String materialName = args[2].toUpperCase();
        ConfigurationSection torchSection = getTorchSection(sender, materialName);
        if (torchSection == null) {
            return;
        }
        Object[] list = torchSection.getKeys(false).toArray();
        sender.sendMessage("§a" + list.length + " items found (" + materialName + "):\n" + Arrays.toString(list));
    }

    private void getLoot(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§c/rainbreaktorch loot get MaterialTorch Name - §fGet the item's ItemStack");
            return;
        }
        ConfigurationSection torchSection = getTorchSection(sender, args[2].toUpperCase());
        if (torchSection == null) {
            return;
        }
        String itemName = args[3];
        ConfigurationSection itemSection = torchSection.getConfigurationSection(itemName);
        if (itemSection == null) {
            sender.sendMessage("§cThere is no such ItemDrop!");
            return;
        }
        ItemStack itemStack = itemSection.getItemStack("item");
        sender.sendMessage("§aItemStack " + itemName + " item:\n" + itemStack.toString() + "\nItemMeta:\n" + itemStack.getItemMeta().toString() + "\nAmount: " + itemSection.getString("amount") + "\nChance: " + itemSection.getString("chance"));
    }

    private void removeLoot(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§c/rainbreaktorch loot remove MaterialTorch Name - §fRemove an item from gifts");
            return;
        }
        FileConfiguration loot = values.getLoot();
        ConfigurationSection torchSection = getTorchSection(sender, loot, args[2].toUpperCase());
        if (torchSection == null) {
            return;
        }
        String itemName = args[3];
        if (!torchSection.contains(itemName)) {
            sender.sendMessage("§cThere is no such item!");
            return;
        }
        torchSection.set(itemName, null);
        saveLoot(loot);
        values.setup();
        sender.sendMessage("§aSuccessfully!");
    }

    private ConfigurationSection getTorchSection(CommandSender sender, FileConfiguration loot, String materialName) {
        ConfigurationSection lootSection = getLootSection(loot);
        return getTorchSection(sender, lootSection, materialName);
    }

    private ConfigurationSection getTorchSection(CommandSender sender, ConfigurationSection lootSection, String materialName) {
        try {
            if (!values.getTorchesBlocks().contains(Material.valueOf(materialName))) {
                sender.sendMessage("There is no such torch!");
                return null;
            }
        } catch (Exception e) {
            sender.sendMessage("There is no such torch OR Material not found.");
            return null;
        }
        ConfigurationSection torchSection = lootSection.getConfigurationSection(materialName);
        if (torchSection == null) {
            torchSection = lootSection.createSection(materialName);
        }
        return torchSection;
    }

    private ConfigurationSection getTorchSection(CommandSender sender, String materialName) {
        FileConfiguration loot = values.getLoot();
        ConfigurationSection lootSection = getLootSection(loot);
        ConfigurationSection torchSection = lootSection.getConfigurationSection(materialName);
        if (torchSection == null) {
            sender.sendMessage("§cThere is no such torchSection!");
            return null;
        }
        return torchSection;
    }

    private ConfigurationSection getLootSection(FileConfiguration loot) {
        ConfigurationSection lootSection = loot.getConfigurationSection("loot");
        if (lootSection == null) {
            lootSection = loot.createSection("loot");
        }
        return lootSection;
    }

    private void saveLoot(FileConfiguration loot) {
        try {
            loot.save(new File(plugin.getDataFolder(), "loot.yml"));
        } catch (Exception e) {
            logger.warning("File loot.yml save error: " + e);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§aHelp from the loot subcommand:\n");
        sender.sendMessage("§a/rainbreaktorch loot add default/MaterialTorch Name Amount(from-to) Chance AddX AddY AddZ - §fAdd an item to gifts");
        sender.sendMessage("§a/rainbreaktorch loot list MaterialTorch - §fGet a list of items");
        sender.sendMessage("§a/rainbreaktorch loot get MaterialTorch Name - §fGet the item's ItemStack");
        sender.sendMessage("§a/rainbreaktorch loot remove MaterialTorch Name - §fRemove an item from gifts");
    }
}
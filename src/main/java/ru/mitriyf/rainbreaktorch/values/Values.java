package ru.mitriyf.rainbreaktorch.values;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.actions.Action;
import ru.mitriyf.rainbreaktorch.utils.actions.ActionType;
import ru.mitriyf.rainbreaktorch.utils.checks.blocks.TypeList;
import ru.mitriyf.rainbreaktorch.utils.checks.blocks.impl.BlackList;
import ru.mitriyf.rainbreaktorch.utils.checks.blocks.impl.CustomList;
import ru.mitriyf.rainbreaktorch.utils.checks.blocks.impl.MultiList;
import ru.mitriyf.rainbreaktorch.utils.checks.blocks.impl.OtherList;
import ru.mitriyf.rainbreaktorch.utils.checks.type.CheckType;
import ru.mitriyf.rainbreaktorch.utils.checks.type.impl.IsOccluding;
import ru.mitriyf.rainbreaktorch.utils.checks.type.impl.IsSolid;
import ru.mitriyf.rainbreaktorch.utils.colors.Colorizer;
import ru.mitriyf.rainbreaktorch.utils.colors.impl.LegacyColorizer;
import ru.mitriyf.rainbreaktorch.utils.colors.impl.MiniMessageColorizer;
import ru.mitriyf.rainbreaktorch.utils.worlds.BiomesList;
import ru.mitriyf.rainbreaktorch.utils.worlds.WorldsList;
import ru.mitriyf.rainbreaktorch.utils.worlds.impl.AllowedWorlds;
import ru.mitriyf.rainbreaktorch.utils.worlds.impl.BlockedWorlds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@SuppressWarnings("DataFlowIssue")
public class Values {
    private final Logger logger;
    private final RainBreakTorch plugin;
    private final File worldsFile, configFile, lootFile;
    private final List<Biome> biomes = new ArrayList<>();
    private final List<World> worlds = new ArrayList<>();
    private final List<Material> torchesBlocks = new ArrayList<>();
    private final List<Material> redstoneBlocks = new ArrayList<>();
    private final List<Material> listSafeBlocks = new ArrayList<>();
    private final List<Material> blackListSafeBlocks = new ArrayList<>();
    private final Pattern actionPattern = Pattern.compile("\\[(\\w+)] ?(.*)");
    private boolean miniMessage, checkNewChunk, physicsEnabled, physicsFull;
    private int objectRemove, breakLimit, msLimit, msDelay;
    private YamlConfiguration config, loot;
    private ConfigurationSection settings;
    private List<Action> noperm, help;
    private WorldsList worldType;
    private BiomesList biomeType;
    private CheckType checkType;
    private Colorizer colorizer;
    private TypeList type;

    public Values(RainBreakTorch plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
        File dataFolder = plugin.getDataFolder();
        lootFile = new File(dataFolder, "loot.yml");
        worldsFile = new File(dataFolder, "worlds");
        configFile = new File(dataFolder, "config.yml");
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            miniMessage = true;
        } catch (Exception e) {
            miniMessage = false;
        }
    }

    public void setup() {
        getConfigurations();
        loadConfigurations();
        clear();
        if (!worldsFile.exists() && !worldsFile.mkdirs()) {
            logger.warning("Error create mkdirs 'worlds'!");
        }
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings == null) {
            logger.warning("There is no section in the configuration: settings");
            return;
        }
        setupSettings(settings);
        ConfigurationSection messages = config.getConfigurationSection("messages");
        if (messages == null) {
            logger.warning("There is no section in the configuration: messages");
            return;
        }
        setupMessages(messages);
        plugin.getLootManager().setup();
    }

    private void getConfigurations() {
        saveConfig("config", configFile);
        saveConfig("loot", lootFile);
        loadConfigurations();
    }

    private void loadConfigurations() {
        config = YamlConfiguration.loadConfiguration(configFile);
        settings = config.getConfigurationSection("settings");
        loot = YamlConfiguration.loadConfiguration(lootFile);
    }

    private void setupSettings(ConfigurationSection settings) {
        String translate = settings.getString("translate").toLowerCase();
        if (miniMessage && translate.equalsIgnoreCase("minimessage")) {
            colorizer = new MiniMessageColorizer();
        } else {
            colorizer = new LegacyColorizer();
        }
        ConfigurationSection functions = settings.getConfigurationSection("functions");
        setupSettingsWorlds(functions);
        setupSettingsBiomes(functions);
        ConfigurationSection torches = functions.getConfigurationSection("torches");
        ConfigurationSection safeBlocks = torches.getConfigurationSection("safeBlocks");
        String checkTypeString = safeBlocks.getString("checkType");
        if (checkTypeString.equalsIgnoreCase("IsSolid")) {
            checkType = new IsSolid();
        } else {
            checkType = new IsOccluding();
        }
        setupSettingsType(safeBlocks);
        setupSettingsMaterials(torches, safeBlocks);
        checkNewChunk = torches.getBoolean("checkNewChunk");
        setupSettingsDeveloper(torches);
    }

    private void setupMessages(ConfigurationSection messages) {
        noperm = getActionList(messages.getStringList("noperm"));
        help = getActionList(messages.getStringList("help"));
    }

    private void setupSettingsBiomes(ConfigurationSection functions) {
        ConfigurationSection biomesSection = functions.getConfigurationSection("biomes");
        biomeType = biomesSection.getString("type").equals("allowed") ? new AllowedWorlds(this) : new BlockedWorlds(this);
        List<String> biomesList = biomesSection.getStringList("list");
        for (String biomeString : biomesList) {
            if (biomeString.isEmpty() || biomeString.equals("no")) {
                continue;
            }
            try {
                Biome biome = Biome.valueOf(biomeString.toUpperCase());
                biomes.add(biome);
            } catch (Exception e) {
                logger.warning("Error in biomes.list " + biomeString + ": " + e);
            }
        }
    }

    private void setupSettingsWorlds(ConfigurationSection functions) {
        ConfigurationSection worldsSection = functions.getConfigurationSection("worlds");
        worldType = worldsSection.getString("type").equals("allowed") ? new AllowedWorlds(this) : new BlockedWorlds(this);
        List<String> worldsList = worldsSection.getStringList("list");
        for (String worldString : worldsList) {
            if (worldString.isEmpty() || worldString.equals("no")) {
                continue;
            }
            try {
                World world = plugin.getServer().getWorld(worldString);
                worlds.add(world);
            } catch (Exception e) {
                logger.warning("Error in worlds.list " + worldString + ": " + e);
            }
        }
    }

    private void setupSettingsType(ConfigurationSection safeBlocks) {
        String typeString = safeBlocks.getString("type");
        switch (typeString.toLowerCase()) {
            case "customlist": {
                type = new CustomList(this);
                break;
            }
            case "blacklist": {
                type = new BlackList(this);
                break;
            }
            case "multilist": {
                type = new MultiList(this);
                break;
            }
            default: {
                type = new OtherList(this);
                break;
            }
        }
    }

    private void setupSettingsMaterials(ConfigurationSection torches, ConfigurationSection safeBlocks) {
        setupTorches(torches);
        setupSafeBlocks(safeBlocks);
    }

    private void setupTorches(ConfigurationSection torches) {
        List<String> torchesList = torches.getStringList("blocks");
        for (String torch : torchesList) {
            try {
                Material material = Material.valueOf(torch.toUpperCase());
                torchesBlocks.add(material);
            } catch (IllegalArgumentException e) {
                if (!torch.startsWith("REDSTONE_TORCH") && !torch.equals("WALL_TORCH") && !torch.equals("REDSTONE_WALL_TORCH") && !torch.equals("CAMPFIRE")) {
                    logger.warning("Error (IllegalArgumentException) in torches.blocks " + torch + ": " + e);
                }
            } catch (Exception e) {
                logger.warning("Error (EXCEPTION) in torches.blocks " + torch + ": " + e);
            }
        }
        List<String> redstoneList = torches.getStringList("redstoneBlocks");
        for (String redstone : redstoneList) {
            if (redstone.isEmpty()) {
                continue;
            }
            try {
                Material material = Material.valueOf(redstone.toUpperCase());
                redstoneBlocks.add(material);
            } catch (Exception e) {
                if (!redstone.startsWith("REDSTONE_TORCH")) {
                    logger.warning("Error in torches.redstoneBlocks " + redstone + ": " + e);
                }
            }
        }
    }

    private void setupSafeBlocks(ConfigurationSection safeBlocks) {
        List<String> listStringSafeBlocks = safeBlocks.getStringList("list");
        for (String materialName : listStringSafeBlocks) {
            if (materialName.isEmpty() || materialName.equals("no")) {
                continue;
            }
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                listSafeBlocks.add(material);
            } catch (Exception e) {
                logger.warning("Error in torches.safeBlocks.list " + materialName + ": " + e);
            }
        }
        List<String> blackListStringSafeBlocks = safeBlocks.getStringList("blacklist");
        for (String materialName : blackListStringSafeBlocks) {
            if (materialName.isEmpty() || materialName.equals("List for multilist")) {
                continue;
            }
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                blackListSafeBlocks.add(material);
            } catch (Exception e) {
                logger.warning("Error in torches.safeBlocks.blacklist " + materialName + ": " + e);
            }
        }
    }

    private void setupSettingsDeveloper(ConfigurationSection torches) {
        ConfigurationSection onlyDeveloper = torches.getConfigurationSection("onlyDeveloper");
        breakLimit = onlyDeveloper.getInt("breakLimit");
        objectRemove = onlyDeveloper.getInt("objectRemove");
        msLimit = onlyDeveloper.getInt("msLimit");
        msDelay = onlyDeveloper.getInt("msDelay");
        ConfigurationSection physicsDeveloper = onlyDeveloper.getConfigurationSection("physics");
        physicsEnabled = physicsDeveloper.getBoolean("enabled");
        physicsFull = physicsDeveloper.getBoolean("full");
    }

    private void saveConfig(String configName, File file) {
        if (file.exists()) {
            return;
        }
        String resource = configName + ".yml";
        try {
            plugin.saveResource(resource, true);
        } catch (Exception e) {
            logger.warning("Error save configurations. Error: " + e);
        }
    }

    private Action fromString(String str) {
        Matcher matcher = actionPattern.matcher(str);
        if (!matcher.matches()) {
            return new Action(ActionType.MESSAGE, str);
        }
        ActionType type;
        try {
            type = ActionType.valueOf(matcher.group(1).toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ActionType.MESSAGE;
            return new Action(type, str);
        }
        return new Action(type, matcher.group(2).trim());
    }

    public List<Action> getActionList(List<String> actionStrings) {
        ImmutableList.Builder<Action> actionListBuilder = ImmutableList.builder();
        for (String actionString : actionStrings) {
            actionListBuilder.add(fromString(actionString));
        }
        return actionListBuilder.build();
    }

    private void clear() {
        biomes.clear();
        worlds.clear();
        torchesBlocks.clear();
        redstoneBlocks.clear();
        listSafeBlocks.clear();
        blackListSafeBlocks.clear();
    }
}
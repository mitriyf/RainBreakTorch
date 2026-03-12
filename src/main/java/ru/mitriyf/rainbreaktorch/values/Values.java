package ru.mitriyf.rainbreaktorch.values;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.mitriyf.rainbreaktorch.RainBreakTorch;
import ru.mitriyf.rainbreaktorch.utils.actions.Action;
import ru.mitriyf.rainbreaktorch.utils.actions.ActionType;
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
    private final File worldsFile;
    private final RainBreakTorch plugin;
    private final List<Biome> biomes = new ArrayList<>();
    private final List<Material> torchesBlocks = new ArrayList<>();
    private final Pattern actionPattern = Pattern.compile("\\[(\\w+)] ?(.*)");
    private List<Action> noperm, help;
    private WorldsList worldType;
    private BiomesList biomeType;
    private List<String> worlds;
    private boolean miniMessage;
    private Colorizer colorizer;
    private int objectRemove;

    public Values(RainBreakTorch plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
        worldsFile = new File(plugin.getDataFolder(), "worlds");
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            miniMessage = true;
        } catch (Exception e) {
            miniMessage = false;
        }
    }

    public void setup() {
        clear();
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        if (!worldsFile.exists() && !worldsFile.mkdirs()) {
            logger.warning("Error create mkdirs 'worlds'!");
        }
        FileConfiguration config = plugin.getConfig();
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
    }

    private void setupSettings(ConfigurationSection settings) {
        String translate = settings.getString("translate").toLowerCase();
        if (miniMessage && translate.equalsIgnoreCase("minimessage")) {
            colorizer = new MiniMessageColorizer();
        } else {
            colorizer = new LegacyColorizer();
        }
        ConfigurationSection functions = settings.getConfigurationSection("functions");
        ConfigurationSection worldsSection = functions.getConfigurationSection("worlds");
        worldType = worldsSection.getString("type").equals("allowed") ? new AllowedWorlds(this) : new BlockedWorlds(this);
        worlds = worldsSection.getStringList("list");
        ConfigurationSection biomesSection = functions.getConfigurationSection("biomes");
        biomeType = biomesSection.getString("type").equals("allowed") ? new AllowedWorlds(this) : new BlockedWorlds(this);
        List<String> biomesList = biomesSection.getStringList("list");
        for (String biomeString : biomesList) {
            if (biomeString.isEmpty() || biomeString.equals("no")) {
                return;
            }
            try {
                Biome biome = Biome.valueOf(biomeString.toUpperCase());
                biomes.add(biome);
            } catch (Exception e) {
                logger.warning("Error in biomes.list " + biomeString + ": " + e);
            }
        }
        ConfigurationSection torches = functions.getConfigurationSection("torches");
        objectRemove = torches.getInt("objectRemove");
        List<String> torchesList = torches.getStringList("blocks");
        for (String torch : torchesList) {
            try {
                Material material = Material.valueOf(torch.toUpperCase());
                torchesBlocks.add(material);
            } catch (Exception e) {
                logger.warning("Error in torches.blocks " + torch + ": " + e);
            }
        }
    }

    private void setupMessages(ConfigurationSection messages) {
        noperm = getActionList(messages.getStringList("noperm"));
        help = getActionList(messages.getStringList("help"));
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
        torchesBlocks.clear();
    }
}
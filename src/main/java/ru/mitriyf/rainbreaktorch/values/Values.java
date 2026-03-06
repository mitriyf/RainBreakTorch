package ru.mitriyf.rainbreaktorch.values;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
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

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@SuppressWarnings("DataFlowIssue")
public class Values {
    private final Logger logger;
    private final RainBreakTorch plugin;
    private final Pattern actionPattern = Pattern.compile("\\[(\\w+)] ?(.*)");
    private List<String> worlds, biomes, torchesBlocks;
    private boolean torchesEnabled, miniMessage;
    private List<Action> noperm, help;
    private WorldsList worldType;
    private BiomesList biomeType;
    private Colorizer colorizer;
    private int objectRemove;

    public Values(RainBreakTorch plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            miniMessage = true;
        } catch (Exception e) {
            miniMessage = false;
        }
    }

    public void setup() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings == null) {
            logger.warning("В конфигурации отсутствует секция: settings");
            return;
        }
        setupSettings(settings);
        ConfigurationSection messages = config.getConfigurationSection("messages");
        if (messages == null) {
            logger.warning("В конфигурации отсутствует секция: messages");
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
        worlds = worldsSection.getStringList("worlds");
        ConfigurationSection biomesSection = functions.getConfigurationSection("biomes");
        biomeType = biomesSection.getString("type").equals("allowed") ? new AllowedWorlds(this) : new BlockedWorlds(this);
        biomes = worldsSection.getStringList("biomes");
        ConfigurationSection torches = functions.getConfigurationSection("torches");
        torchesEnabled = torches.getBoolean("enabled");
        objectRemove = torches.getInt("objectRemove");
        torchesBlocks = torches.getStringList("blocks");
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
}
package ru.mitriyf.rainbreaktorch;

import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mitriyf.rainbreaktorch.cmd.RainBreakTorchCommand;
import ru.mitriyf.rainbreaktorch.listeners.WorldListener;
import ru.mitriyf.rainbreaktorch.listeners.version8.WorldListenerVersion8;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.common.data.ChunkData;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@SuppressWarnings("DataFlowIssue")
public final class RainBreakTorch extends JavaPlugin {
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private Values values;
    private Utils utils;
    private int version;

    @Override
    public void onEnable() {
        getLogger().info("Support: https://vk.com/jdevs");
        saveDefaultConfig();
        getVer();
        values = new Values(this);
        utils = new Utils(this);
        values.setup();
        utils.setup();
        getCommand("rainbreaktorch").setExecutor(new RainBreakTorchCommand(this));
        if (values.isTorchesEnabled()) {
            PluginManager manager = getServer().getPluginManager();
            manager.registerEvents(new WorldListener(this), this);
            if (version > 7) {
                manager.registerEvents(new WorldListenerVersion8(this), this);
            }
        }
    }

    @Override
    public void onDisable() {
        for (ChunkData data : utils.getTorchUtils().getChunkData().values()) {
            data.save();
        }
    }

    private void getVer() {
        String ver = getServer().getBukkitVersion().split("-")[0].split("\\.")[1];
        if (ver.length() >= 2) {
            version = Integer.parseInt(ver.substring(0, 2));
        } else {
            version = Integer.parseInt(ver);
        }
    }
}
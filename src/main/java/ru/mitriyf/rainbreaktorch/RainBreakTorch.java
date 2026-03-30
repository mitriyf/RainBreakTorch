package ru.mitriyf.rainbreaktorch;

import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mitriyf.rainbreaktorch.cmd.RainBreakTorchCommand;
import ru.mitriyf.rainbreaktorch.listeners.WorldListener;
import ru.mitriyf.rainbreaktorch.listeners.versions.WorldListenerVersion8;
import ru.mitriyf.rainbreaktorch.loot.LootManager;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.utils.common.data.ChunkData;
import ru.mitriyf.rainbreaktorch.utils.tasks.DropTask;
import ru.mitriyf.rainbreaktorch.utils.tasks.data.DropData;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@SuppressWarnings("DataFlowIssue")
public final class RainBreakTorch extends JavaPlugin {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private LootManager lootManager;
    private DropTask dropTask;
    private Values values;
    private Utils utils;
    private int version;

    @Override
    public void onEnable() {
        getLogger().info("Support: https://vk.com/jdevs");
        saveDefaultConfig();
        getServerVersion();
        values = new Values(this);
        utils = new Utils(this);
        getCommand("rainbreaktorch").setExecutor(new RainBreakTorchCommand(this));
        lootManager = new LootManager(this);
        values.setup();
        utils.setup();
        startDropTask();
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new WorldListener(this), this);
        if (version > 7) {
            manager.registerEvents(new WorldListenerVersion8(this), this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        Map<String, ChunkData> dataMap = utils.getTorchUtils().getChunkData();
        for (ChunkData data : new ArrayList<>(dataMap.values())) {
            data.save(false);
        }
        dataMap.clear();
        if (dropTask != null) {
            dropTask.cancel();
            for (DropData data : dropTask.getDataList()) {
                dropTask.drop(data, false);
            }
        }
    }

    private void startDropTask() {
        dropTask = new DropTask(this);
        dropTask.runTaskTimer(this, 1, 1);
    }

    private void getServerVersion() {
        String[] serverVersion = getServer().getBukkitVersion().split("-")[0].split("\\.");
        String subVersion = serverVersion[1];
        if (Integer.parseInt(serverVersion[0]) > 1) {
            version = 26;
        } else if (subVersion.length() >= 2) {
            version = Integer.parseInt(subVersion.substring(0, 2));
        } else {
            version = Integer.parseInt(subVersion);
        }
    }
}
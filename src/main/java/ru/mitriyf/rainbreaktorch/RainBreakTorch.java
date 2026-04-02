package ru.mitriyf.rainbreaktorch;

import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mitriyf.rainbreaktorch.command.RainBreakTorchCommand;
import ru.mitriyf.rainbreaktorch.listener.WorldListener;
import ru.mitriyf.rainbreaktorch.listener.compat.WorldListenerV8;
import ru.mitriyf.rainbreaktorch.model.ChunkData;
import ru.mitriyf.rainbreaktorch.model.DropData;
import ru.mitriyf.rainbreaktorch.service.DropTaskService;
import ru.mitriyf.rainbreaktorch.service.LootService;
import ru.mitriyf.rainbreaktorch.service.TorchService;
import ru.mitriyf.rainbreaktorch.utils.Utils;
import ru.mitriyf.rainbreaktorch.values.Values;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@SuppressWarnings("DataFlowIssue")
public final class RainBreakTorch extends JavaPlugin {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private TorchService torchService;
    private LootService lootService;
    private DropTaskService dropTaskService;
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
        lootService = new LootService(this);
        torchService = new TorchService(this);
        getCommand("rainbreaktorch").setExecutor(new RainBreakTorchCommand(this));
        values.setup();
        utils.setup();
        startDropTask();
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new WorldListener(this), this);
        if (version > 7) {
            manager.registerEvents(new WorldListenerV8(this), this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        Map<String, ChunkData> dataMap = torchService.getChunkData();
        for (ChunkData data : new ArrayList<>(dataMap.values())) {
            data.save(false);
        }
        dataMap.clear();
        if (dropTaskService != null) {
            dropTaskService.cancel();
            for (DropData data : dropTaskService.getDataList()) {
                dropTaskService.drop(data, false);
            }
        }
    }

    private void startDropTask() {
        dropTaskService = new DropTaskService(this);
        dropTaskService.runTaskTimer(this, 1, 1);
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
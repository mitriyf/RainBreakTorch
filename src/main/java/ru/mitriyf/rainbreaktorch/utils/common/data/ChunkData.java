package ru.mitriyf.rainbreaktorch.utils.common.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.logging.Logger;

@Getter
@Setter
public class ChunkData {
    private final YamlConfiguration dataFile;
    private final Logger logger;
    private final File file;
    private boolean changed;
    private BukkitTask task;

    public ChunkData(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        dataFile = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    public void save() {
        if (changed) {
            try {
                dataFile.save(file);
            } catch (Exception e) {
                logger.warning("Error saving file: " + e);
            }
        }
    }
}
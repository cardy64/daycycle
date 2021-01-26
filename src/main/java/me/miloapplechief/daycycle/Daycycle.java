package me.miloapplechief.daycycle;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sets the speed that a whole day / night cycle takes to complete.
 *
 */
public final class Daycycle extends JavaPlugin {

    List<WorldRule> worldRules = new ArrayList<>();

    /**
     * All the information for each world
     */
    private static class WorldRule {
        final World world;
        final int fullDayCycle;
        float time = 0;
        int taskId = -1;

        public WorldRule(World world, int dayCycle) {
            this.world = world;
            this.fullDayCycle = dayCycle;
        }
    }

    @Override
    public void onEnable() {
        // Wait for worlds to load.
        getServer().getScheduler().scheduleSyncDelayedTask(this, this::setup, 0);
    }

    @Override
    public void onDisable() {
        for (WorldRule worldRule : worldRules) {
            worldRule.world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            if (worldRule.taskId != -1) {
                getServer().getScheduler().cancelTask(worldRule.taskId);
            }
        }
    }

    public void setup() {
        // If the config.yml file doesn't exist, copy it from the jar.
        saveDefaultConfig();

        // Get the config file.
        FileConfiguration config = this.getConfig();

        List<Map<String,Object>> worlds = (List<Map<String,Object>>) config.getList("worlds");

        if (worlds != null) {
            for (Map<String,Object> configEntry : worlds) {
                String worldName = (String) configEntry.get("name");
                Object minutesString = configEntry.get("minutes");
                World world = getServer().getWorld(worldName);
                int minutes;
                if (minutesString instanceof Integer) {
                    minutes = (Integer) minutesString;
                } else {
                    getLogger().warning("Attempted to assign a non integer value to world \"" + worldName + "\"");
                    continue;
                }
                if (world == null) {
                    getLogger().warning("World \"" + worldName + "\" does not exist");
                } else {
                    WorldRule worldRule = new WorldRule(world, minutes);
                    worldRule.world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                    worldRules.add(worldRule);

                    float increment = 20.0f / worldRule.fullDayCycle;
                    worldRule.time = 0;
                    worldRule.taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
                        worldRule.time += increment;

                        int ticks = (int) worldRule.time;
                        worldRule.world.setTime(worldRule.world.getTime() + ticks);

                        worldRule.time -= ticks;
                    }, 0, 1);
                }
            }
        }
    }
}
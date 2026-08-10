package me.vennlmao.ariscore.spawners.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.spawners.SpawnersModule;
import org.bukkit.Material;

import java.util.Map;

public class SpawnerProductionTask {

    private static final int TICK_PERIOD = 20;

    private final SpawnersModule module;
    private ScheduledTask task;
    private int autosaveCounter;

    public SpawnerProductionTask(SpawnersModule module) {
        this.module = module;
    }

    public void start() {
        task = module.getPlugin().getServer().getGlobalRegionScheduler()
                .runAtFixedRate(module.getPlugin(), t -> tick(), TICK_PERIOD, TICK_PERIOD);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        SpawnerManager manager = module.getSpawnerManager();
        SpawnerDefinitionManager defs = module.getSpawnerDefinitionManager();
        long maxPerMaterial = module.getConfig().getLong("storage.max-per-material", 999999999L);

        for (SpawnerData data : manager.getAll().values()) {
            MobSpawnerDefinition def = defs.get(data.getEntityType());
            if (def == null) continue;

            int intervalTicks = Math.max(20, def.getTimeSeconds() * 20);
            int remaining = data.getTicksUntilProduction() - TICK_PERIOD;
            if (remaining > 0) {
                data.setTicksUntilProduction(remaining);
                continue;
            }
            data.setTicksUntilProduction(intervalTicks);

            double multiplier = manager.getIsolationMultiplier(data) * data.getAmount();
            long cycles = Math.max(1, Math.round(multiplier));

            if (def.getXpAmount() > 0) {
                manager.addXpCapped(data, def.getXpAmount() * cycles);
            }

            for (Map.Entry<Material, Long> entry : def.getDrops().entrySet()) {
                long total = entry.getValue() * cycles;
                if (total <= 0) continue;

                long current = data.getStoredCount(entry.getKey());
                long space = maxPerMaterial - current;
                if (space <= 0) continue;
                data.addItem(entry.getKey(), Math.min(total, space));
            }
        }

        autosaveCounter += TICK_PERIOD;
        int autosaveInterval = module.getConfig().getInt("autosave-interval-seconds", 60) * 20;
        if (autosaveCounter >= autosaveInterval) {
            autosaveCounter = 0;
            module.getPlugin().getServer().getAsyncScheduler()
                    .runNow(module.getPlugin(), t -> manager.saveDirty());
        }
    }
}

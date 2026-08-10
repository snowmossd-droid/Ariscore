package me.vennlmao.ariscore.spawners.listeners;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public class SpawnerNoSpawnListener implements Listener {

    private final SpawnersModule module;

    public SpawnerNoSpawnListener(SpawnersModule module) {
        this.module = module;
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (module.getSpawnerManager().get(event.getSpawner().getLocation()) != null) {
            event.setCancelled(true);
        }
    }
}

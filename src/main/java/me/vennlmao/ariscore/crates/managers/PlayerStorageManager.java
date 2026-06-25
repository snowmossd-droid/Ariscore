package me.vennlmao.ariscore.crates.managers;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.GamerModel;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerStorageManager {

    private final CratesModule module;
    private final CratesDatabaseManager databaseManager;
    private final ExecutorService executor;

    public PlayerStorageManager(CratesModule module) {
        this.module = module;
        this.executor = Executors.newSingleThreadExecutor();
        this.databaseManager = new CratesDatabaseManager(module);
        this.databaseManager.init();
    }

    public CompletableFuture<GamerModel> retrievePlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> databaseManager.loadPlayer(uuid), executor)
                .exceptionally(ex -> {
                    module.getPlugin().getLogger().severe("[Crates] Failed to load player " + uuid + ": " + ex.getMessage());
                    return new GamerModel(uuid);
                });
    }

    public CompletableFuture<Void> savePlayer(GamerModel gamer) {
        return CompletableFuture.runAsync(() -> databaseManager.savePlayer(gamer), executor)
                .exceptionally(ex -> {
                    module.getPlugin().getLogger().severe("[Crates] Failed to save player " + gamer.getUniqueId() + ": " + ex.getMessage());
                    return null;
                });
    }

    public void saveAll(GamerDataManager dataManager) {
        databaseManager.saveAllPlayers(dataManager);
        executor.shutdown();
        databaseManager.close();
    }
}

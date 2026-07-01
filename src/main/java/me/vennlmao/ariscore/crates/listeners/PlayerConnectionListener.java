package me.vennlmao.ariscore.crates.listeners;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.GamerModel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final CratesModule module;

    public PlayerConnectionListener(CratesModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        module.getPlayerStorageManager().retrievePlayer(event.getUniqueId())
                .whenComplete((gamer, ex) -> {
                    if (ex != null) {
                        module.getPlugin().getLogger().severe("[Crates] Failed to load player " + event.getUniqueId() + ": " + ex.getMessage());
                        module.getGamerDataManager().cache(new GamerModel(event.getUniqueId()));
                        return;
                    }
                    module.getGamerDataManager().cache(gamer);
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GamerModel gamer = module.getGamerDataManager().find(event.getPlayer().getUniqueId());
        if (gamer == null) return;

        module.getPlayerStorageManager().savePlayer(gamer)
                .whenComplete((success, ex) -> {
                    if (ex != null) {
                        module.getPlugin().getLogger().severe("[Crates] Failed to save player " + gamer.getUniqueId() + ": " + ex.getMessage());
                    }
                    module.getGamerDataManager().remove(gamer.getUniqueId());
                });
    }
}

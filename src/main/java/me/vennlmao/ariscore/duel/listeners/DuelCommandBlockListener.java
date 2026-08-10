package me.vennlmao.ariscore.duel.listeners;

import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.duel.utils.MessageUtil;
import me.vennlmao.ariscore.duel.utils.SoundUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class DuelCommandBlockListener implements Listener {

    private final DuelModule module;

    public DuelCommandBlockListener(DuelModule module) { this.module = module; }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!module.getSessionManager().isDueling(event.getPlayer().getUniqueId())) return;

        String usedLabel = event.getMessage().split(" ")[0].toLowerCase();
        List<String> blocked = module.getConfig().getStringList("match-blocked-commands");

        for (String entry : blocked) {
            String normalized = entry.toLowerCase();
            if (!normalized.startsWith("/")) normalized = "/" + normalized;
            if (usedLabel.equals(normalized)) {
                event.setCancelled(true);
                MessageUtil.sendBoth(event.getPlayer(), "match-blocked-command");
                SoundUtil.play(event.getPlayer(), "error");
                return;
            }
        }
    }
}

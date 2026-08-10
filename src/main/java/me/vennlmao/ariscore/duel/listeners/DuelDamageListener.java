package me.vennlmao.ariscore.duel.listeners;

import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.duel.managers.DuelArena;
import me.vennlmao.ariscore.duel.managers.DuelSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DuelDamageListener implements Listener {

    private final DuelModule module;

    public DuelDamageListener(DuelModule module) { this.module = module; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        DuelSession session = module.getSessionManager().getSession(victim.getUniqueId());
        if (session == null) return;

        if (session.getState() != DuelSession.State.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        if (event instanceof EntityDamageByEntityEvent damageByEntity) {
            if (damageByEntity.getDamager() instanceof Player attacker) {
                if (!attacker.getUniqueId().equals(session.getOpponent(victim.getUniqueId()))) {
                    event.setCancelled(true);
                }
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DuelSession session = module.getSessionManager().getSession(player.getUniqueId());
        if (session == null || session.getState() != DuelSession.State.ACTIVE) return;

        if (!module.getConfig().getBoolean("duel.keep-inventory", false)) {
            event.setKeepInventory(false);
        } else {
            event.setKeepInventory(true);
            event.getDrops().clear();
        }
        event.setKeepLevel(true);

        module.getSessionManager().handleDeath(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        DuelSession session = module.getSessionManager().getSession(player.getUniqueId());
        if (session == null || session.getState() != DuelSession.State.POST_MATCH) return;
        if (!player.getUniqueId().equals(session.getLoser())) return;

        DuelArena arena = session.getArena();
        boolean isPlayer1 = player.getUniqueId().equals(session.getPlayer1());
        event.setRespawnLocation(isPlayer1 ? arena.getPos1() : arena.getPos2());

        module.getSessionManager().onPostDeathRespawn(player);
    }
}

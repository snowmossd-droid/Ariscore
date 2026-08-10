package me.vennlmao.ariscore.duel.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.duel.utils.MessageUtil;
import me.vennlmao.ariscore.duel.utils.SoundUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class DuelSessionManager {

    private final DuelModule module;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final Map<UUID, DuelSession> sessions = new HashMap<>();
    private final Map<UUID, DuelInvite> invitesByTarget = new HashMap<>();

    public DuelSessionManager(DuelModule module) {
        this.module = module;
    }

    public boolean isQueued(UUID uuid) { return queue.contains(uuid); }

    public boolean isBusy(UUID uuid) {
        return isQueued(uuid) || sessions.containsKey(uuid) || invitesByTarget.containsKey(uuid)
                || invitesByTarget.values().stream().anyMatch(i -> i.getInviter().equals(uuid));
    }

    public DuelSession getSession(UUID uuid) { return sessions.get(uuid); }

    public void joinQueue(Player player) {
        UUID id = player.getUniqueId();
        queue.add(id);
        MessageUtil.sendBoth(player, "queue_joined");
        SoundUtil.play(player, "queue_join");
        tryMatch();
    }

    public void leaveQueue(Player player) {
        if (queue.remove(player.getUniqueId())) {
            MessageUtil.sendBoth(player, "queue_left");
            SoundUtil.play(player, "queue_leave");
        }
    }

    private void tryMatch() {
        if (queue.size() < 2) return;
        Iterator<UUID> it = queue.iterator();
        UUID first = it.next();
        UUID second = it.next();
        queue.remove(first);
        queue.remove(second);

        Player p1 = Bukkit.getPlayer(first);
        Player p2 = Bukkit.getPlayer(second);
        if (p1 == null || !p1.isOnline()) { if (p2 != null) joinQueue(p2); return; }
        if (p2 == null || !p2.isOnline()) { joinQueue(p1); return; }

        if (!module.getArenaManager().hasArenas()) {
            MessageUtil.sendBoth(p1, "no_arenas");
            MessageUtil.sendBoth(p2, "no_arenas");
            return;
        }

        DuelArena arena = module.getArenaManager().getRandomArena();
        DuelSession session = new DuelSession(first, second, arena, DuelSession.Origin.QUEUE);
        sessions.put(first, session);
        sessions.put(second, session);

        module.getGuiListener().openQueueConfirm(p1);
        module.getGuiListener().openQueueConfirm(p2);

        int confirmSeconds = module.getConfig().getInt("queue.confirm-time", 15);
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(module.getPlugin(), t -> {
            DuelSession s = sessions.get(first);
            if (s != null && s.getState() == DuelSession.State.CONFIRMING) {
                cancelConfirming(s, "queue_confirm_timeout");
            }
        }, Math.max(1, confirmSeconds * 20));
        session.setCountdownTask(task);
    }

    public void confirmQueueMatch(Player player) {
        DuelSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getState() != DuelSession.State.CONFIRMING) return;
        session.confirm(player.getUniqueId());
        player.closeInventory();
        MessageUtil.sendBoth(player, "queue_confirmed");
        SoundUtil.play(player, "confirm");
        if (session.bothConfirmed()) {
            session.cancelTasks();
            startMatch(session);
        }
    }

    public void cancelQueueMatch(Player player) {
        DuelSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getState() != DuelSession.State.CONFIRMING) return;
        cancelConfirming(session, "queue_confirm_declined");
    }

    private void cancelConfirming(DuelSession session, String reasonKey) {
        session.cancelTasks();
        session.setState(DuelSession.State.FINISHED);
        sessions.remove(session.getPlayer1());
        sessions.remove(session.getPlayer2());

        for (UUID uuid : List.of(session.getPlayer1(), session.getPlayer2())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.closeInventory();
            MessageUtil.sendBoth(p, reasonKey);
            SoundUtil.play(p, "error");
            if (session.isConfirmed(uuid)) queue.add(uuid);
        }
        tryMatch();
    }

    public void sendCustomInvite(Player inviter, Player target, DuelArena arena) {
        DuelInvite invite = new DuelInvite(inviter.getUniqueId(), target.getUniqueId(), arena);
        invitesByTarget.put(target.getUniqueId(), invite);

        MessageUtil.sendBoth(target, "invite_received", s -> s.replace("{player}", inviter.getName()));
        SoundUtil.play(target, "invite");
        MessageUtil.sendBoth(inviter, "invite_sent", s -> s.replace("{player}", target.getName()));

        int expireSeconds = module.getConfig().getInt("duel.invite-expire", 30);
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(module.getPlugin(), t -> {
            DuelInvite current = invitesByTarget.get(target.getUniqueId());
            if (current == invite) {
                invitesByTarget.remove(target.getUniqueId());
                Player i = Bukkit.getPlayer(invite.getInviter());
                if (i != null) MessageUtil.sendBoth(i, "invite_expired", s -> s.replace("{player}", target.getName()));
                if (target.isOnline()) MessageUtil.sendBoth(target, "invite_expired_target", s -> s.replace("{player}", i != null ? i.getName() : "?"));
            }
        }, Math.max(1, expireSeconds * 20));
        invite.setExpireTask(task);
    }

    public boolean acceptInvite(Player target) {
        DuelInvite invite = invitesByTarget.remove(target.getUniqueId());
        if (invite == null) return false;
        invite.cancelExpireTask();

        Player inviter = Bukkit.getPlayer(invite.getInviter());
        if (inviter == null || !inviter.isOnline()) {
            MessageUtil.sendBoth(target, "invite_offline");
            return true;
        }
        if (isBusy(inviter.getUniqueId()) || isBusy(target.getUniqueId())) {
            MessageUtil.sendBoth(target, "invite_unavailable");
            return true;
        }

        DuelArena arena = invite.getArena() != null ? invite.getArena() : module.getArenaManager().getRandomArena();
        if (arena == null) {
            MessageUtil.sendBoth(target, "no_arenas");
            MessageUtil.sendBoth(inviter, "no_arenas");
            return true;
        }

        DuelSession session = new DuelSession(invite.getInviter(), invite.getTarget(), arena, DuelSession.Origin.CUSTOM);
        session.confirm(invite.getInviter());
        session.confirm(invite.getTarget());
        sessions.put(invite.getInviter(), session);
        sessions.put(invite.getTarget(), session);
        startMatch(session);
        return true;
    }

    public boolean denyInvite(Player target) {
        DuelInvite invite = invitesByTarget.remove(target.getUniqueId());
        if (invite == null) return false;
        invite.cancelExpireTask();
        Player inviter = Bukkit.getPlayer(invite.getInviter());
        if (inviter != null) MessageUtil.sendBoth(inviter, "invite_denied", s -> s.replace("{player}", target.getName()));
        MessageUtil.sendBoth(target, "invite_denied_self");
        return true;
    }

    private void startMatch(DuelSession session) {
        Player p1 = Bukkit.getPlayer(session.getPlayer1());
        Player p2 = Bukkit.getPlayer(session.getPlayer2());
        if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) {
            sessions.remove(session.getPlayer1());
            sessions.remove(session.getPlayer2());
            if (p1 != null && p1.isOnline()) MessageUtil.sendBoth(p1, "opponent_offline");
            if (p2 != null && p2.isOnline()) MessageUtil.sendBoth(p2, "opponent_offline");
            return;
        }

        session.setState(DuelSession.State.COUNTDOWN);
        p1.getScheduler().run(module.getPlugin(), t -> p1.teleportAsync(session.getArena().getPos1()), null);
        p2.getScheduler().run(module.getPlugin(), t -> p2.teleportAsync(session.getArena().getPos2()), null);

        MessageUtil.sendTitle(p1, "duel_start_title", "duel_start_subtitle", s -> s.replace("{opponent}", p2.getName()));
        MessageUtil.sendTitle(p2, "duel_start_title", "duel_start_subtitle", s -> s.replace("{opponent}", p1.getName()));

        int countdown = module.getConfig().getInt("duel.countdown", 5);
        int[] remaining = {countdown};
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(module.getPlugin(), t -> {
            if (!p1.isOnline() || !p2.isOnline()) {
                t.cancel();
                abortSession(session, "opponent_disconnected");
                return;
            }
            if (remaining[0] <= 0) {
                t.cancel();
                session.setState(DuelSession.State.ACTIVE);
                MessageUtil.sendBoth(p1, "duel_fight");
                MessageUtil.sendBoth(p2, "duel_fight");
                SoundUtil.play(p1, "fight");
                SoundUtil.play(p2, "fight");
                return;
            }
            MessageUtil.sendTitle(p1, "duel_countdown_title", "duel_countdown_subtitle", s -> s.replace("{seconds}", String.valueOf(remaining[0])));
            MessageUtil.sendTitle(p2, "duel_countdown_title", "duel_countdown_subtitle", s -> s.replace("{seconds}", String.valueOf(remaining[0])));
            SoundUtil.play(p1, "countdown");
            SoundUtil.play(p2, "countdown");
            remaining[0]--;
        }, 20L, 20L);
        session.setCountdownTask(task);
    }

    private void abortSession(DuelSession session, String messageKey) {
        session.cancelTasks();
        session.setState(DuelSession.State.FINISHED);
        sessions.remove(session.getPlayer1());
        sessions.remove(session.getPlayer2());
        for (UUID uuid : List.of(session.getPlayer1(), session.getPlayer2())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtil.sendBoth(p, messageKey);
        }
    }

    public boolean isDueling(UUID uuid) {
        DuelSession s = sessions.get(uuid);
        return s != null && (s.getState() == DuelSession.State.COUNTDOWN || s.getState() == DuelSession.State.ACTIVE);
    }

    public boolean isInSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public void handleDeath(Player dead) {
        DuelSession session = sessions.get(dead.getUniqueId());
        if (session == null || session.getState() != DuelSession.State.ACTIVE) return;

        UUID winnerId = session.getOpponent(dead.getUniqueId());
        session.setResult(winnerId, dead.getUniqueId());
        session.setState(DuelSession.State.POST_MATCH);

        Player winner = Bukkit.getPlayer(winnerId);
        module.getStatsManager().recordWin(winnerId);
        module.getStatsManager().recordLoss(dead.getUniqueId());

        int lootSeconds = module.getConfig().getInt("duel.loot-time", 120);

        if (winner != null && winner.isOnline()) {
            MessageUtil.sendTitle(winner, "duel_win_title", "duel_win_subtitle", s -> s.replace("{opponent}", dead.getName()));
            MessageUtil.sendBoth(winner, "duel_win_loot", s -> s.replace("{seconds}", String.valueOf(lootSeconds)));
            SoundUtil.play(winner, "win");
            payReward(winner);

            ScheduledTask returnTask = Bukkit.getGlobalRegionScheduler().runDelayed(module.getPlugin(), t -> {
                if (!session.hasLeft(winnerId)) {
                    if (winner.isOnline()) forceReturn(winner);
                    session.markLeft(winnerId);
                    checkSessionComplete(session);
                }
            }, Math.max(1, lootSeconds * 20));
            session.setReturnTask(returnTask);
        }

        MessageUtil.sendTitle(dead, "duel_lose_title", "duel_lose_subtitle", s -> s.replace("{opponent}", winner != null ? winner.getName() : "?"));
        SoundUtil.play(dead, "lose");
    }

    public void onPostDeathRespawn(Player player) {
        DuelSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getState() != DuelSession.State.POST_MATCH) return;
        if (!player.getUniqueId().equals(session.getLoser())) return;
        player.getScheduler().run(module.getPlugin(), t -> {
            player.setGameMode(GameMode.SPECTATOR);
            MessageUtil.sendBoth(player, "duel_spectate");
        }, null);
    }

    public void handleDraw(Player requester) {
        DuelSession session = sessions.get(requester.getUniqueId());
        if (session == null || session.getState() != DuelSession.State.ACTIVE) {
            MessageUtil.sendBoth(requester, "not_in_duel");
            return;
        }
        session.requestDraw(requester.getUniqueId());
        UUID opponentId = session.getOpponent(requester.getUniqueId());
        Player opponent = Bukkit.getPlayer(opponentId);

        if (session.bothRequestedDraw()) {
            finishDraw(session);
            return;
        }

        MessageUtil.sendBoth(requester, "draw_requested");
        if (opponent != null) {
            MessageUtil.sendBoth(opponent, "draw_offer", s -> s.replace("{player}", requester.getName()));
        }
    }

    private void finishDraw(DuelSession session) {
        session.cancelTasks();
        session.setState(DuelSession.State.FINISHED);
        sessions.remove(session.getPlayer1());
        sessions.remove(session.getPlayer2());

        module.getStatsManager().recordDraw(session.getPlayer1());
        module.getStatsManager().recordDraw(session.getPlayer2());

        for (UUID uuid : List.of(session.getPlayer1(), session.getPlayer2())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            MessageUtil.sendTitle(p, "duel_draw_title", "duel_draw_subtitle");
            SoundUtil.play(p, "draw");
            forceReturn(p);
        }
    }

    public void handleLeave(Player player) {
        UUID id = player.getUniqueId();

        if (isQueued(id)) { leaveQueue(player); return; }

        DuelSession session = sessions.get(id);
        if (session == null) { MessageUtil.sendBoth(player, "not_in_duel"); return; }

        switch (session.getState()) {
            case CONFIRMING -> cancelConfirming(session, "queue_confirm_declined");
            case COUNTDOWN, ACTIVE -> {
                UUID opponentId = session.getOpponent(id);
                session.setResult(opponentId, id);
                session.cancelTasks();
                session.setState(DuelSession.State.FINISHED);
                sessions.remove(session.getPlayer1());
                sessions.remove(session.getPlayer2());

                MessageUtil.sendBoth(player, "duel_left");
                module.getStatsManager().recordLoss(id);
                forceReturn(player);

                if (opponentId != null) {
                    module.getStatsManager().recordWin(opponentId);
                    Player opponent = Bukkit.getPlayer(opponentId);
                    if (opponent != null) {
                        MessageUtil.sendBoth(opponent, "duel_win_disconnect");
                        forceReturn(opponent);
                    }
                }
            }
            case POST_MATCH -> {
                session.markLeft(id);
                forceReturn(player);
                MessageUtil.sendBoth(player, "duel_returned");
                checkSessionComplete(session);
            }
            case FINISHED -> {}
        }
    }

    private void checkSessionComplete(DuelSession session) {
        if (session.bothLeft() || (session.getState() == DuelSession.State.POST_MATCH
                && session.hasLeft(session.getWinner()) && !Bukkit.getOnlinePlayers().stream()
                .anyMatch(p -> p.getUniqueId().equals(session.getLoser())))) {
            session.cancelTasks();
            session.setState(DuelSession.State.FINISHED);
            sessions.remove(session.getPlayer1());
            sessions.remove(session.getPlayer2());
        }
    }

    public void handleDisconnect(UUID id) {
        queue.remove(id);
        invitesByTarget.remove(id);
        invitesByTarget.values().removeIf(i -> i.getInviter().equals(id));

        DuelSession session = sessions.get(id);
        if (session == null) return;

        if (session.getState() == DuelSession.State.COUNTDOWN || session.getState() == DuelSession.State.ACTIVE) {
            UUID opponentId = session.getOpponent(id);
            session.setResult(opponentId, id);
            module.getStatsManager().recordLoss(id);
            if (opponentId != null) {
                module.getStatsManager().recordWin(opponentId);
                Player opponent = Bukkit.getPlayer(opponentId);
                if (opponent != null) {
                    MessageUtil.sendBoth(opponent, "duel_win_disconnect");
                    forceReturn(opponent);
                }
            }
            session.cancelTasks();
            session.setState(DuelSession.State.FINISHED);
            sessions.remove(session.getPlayer1());
            sessions.remove(session.getPlayer2());
        } else if (session.getState() == DuelSession.State.CONFIRMING) {
            cancelConfirming(session, "queue_confirm_declined");
        } else if (session.getState() == DuelSession.State.POST_MATCH) {
            session.markLeft(id);
            checkSessionComplete(session);
        }
    }

    private void forceReturn(Player player) {
        player.getScheduler().run(module.getPlugin(), t -> {
            if (player.getGameMode() == GameMode.SPECTATOR) player.setGameMode(GameMode.SURVIVAL);
            Location spawn = resolveSpawn();
            if (spawn != null) player.teleportAsync(spawn);
        }, null);
    }

    private Location resolveSpawn() {
        try {
            var spawnModule = ArisCore.getInstance().getSpawnModule();
            if (spawnModule != null && spawnModule.getSpawnManager() != null) {
                Location def = spawnModule.getSpawnManager().getSpawn("default");
                if (def != null) return def;
                return spawnModule.getSpawnManager().getRandomSpawn();
            }
        } catch (Exception ignored) {}
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    private void payReward(Player winner) {
        if (!module.getConfig().getBoolean("rewards.enabled", false)) return;
        double amount = module.getConfig().getDouble("rewards.money-per-win", 0);
        if (amount <= 0) return;
        try {
            var shopModule = ArisCore.getInstance().getShopModule();
            if (shopModule == null) return;
            Economy economy = shopModule.getEconomy();
            if (economy == null) return;
            economy.depositPlayer(winner, amount);
            MessageUtil.sendBoth(winner, "duel_win_reward", s -> s.replace("{amount}", String.valueOf(amount)));
        } catch (Exception ignored) {}
    }

    public void cancelAll() {
        for (DuelSession session : new HashSet<>(sessions.values())) session.cancelTasks();
        sessions.clear();
        queue.clear();
        invitesByTarget.values().forEach(DuelInvite::cancelExpireTask);
        invitesByTarget.clear();
    }
}

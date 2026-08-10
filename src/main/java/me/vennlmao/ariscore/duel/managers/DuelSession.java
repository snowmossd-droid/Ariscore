package me.vennlmao.ariscore.duel.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DuelSession {

    public enum State { CONFIRMING, COUNTDOWN, ACTIVE, POST_MATCH, FINISHED }
    public enum Origin { QUEUE, CUSTOM }

    private final UUID player1;
    private final UUID player2;
    private final DuelArena arena;
    private final Origin origin;
    private State state;

    private final Set<UUID> confirmed = new HashSet<>();
    private final Set<UUID> drawRequests = new HashSet<>();
    private final Set<UUID> leftPostMatch = new HashSet<>();

    private UUID winner;
    private UUID loser;

    private ScheduledTask countdownTask;
    private ScheduledTask returnTask;

    public DuelSession(UUID player1, UUID player2, DuelArena arena, Origin origin) {
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
        this.origin = origin;
        this.state = State.CONFIRMING;
    }

    public UUID getPlayer1() { return player1; }
    public UUID getPlayer2() { return player2; }
    public DuelArena getArena() { return arena; }
    public Origin getOrigin() { return origin; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public UUID getOpponent(UUID uuid) {
        if (uuid.equals(player1)) return player2;
        if (uuid.equals(player2)) return player1;
        return null;
    }

    public boolean hasPlayer(UUID uuid) {
        return uuid.equals(player1) || uuid.equals(player2);
    }

    public void confirm(UUID uuid) { confirmed.add(uuid); }
    public boolean isConfirmed(UUID uuid) { return confirmed.contains(uuid); }
    public boolean bothConfirmed() { return confirmed.contains(player1) && confirmed.contains(player2); }

    public void requestDraw(UUID uuid) { drawRequests.add(uuid); }
    public boolean hasRequestedDraw(UUID uuid) { return drawRequests.contains(uuid); }
    public boolean bothRequestedDraw() { return drawRequests.contains(player1) && drawRequests.contains(player2); }

    public UUID getWinner() { return winner; }
    public UUID getLoser() { return loser; }
    public void setResult(UUID winner, UUID loser) { this.winner = winner; this.loser = loser; }

    public void markLeft(UUID uuid) { leftPostMatch.add(uuid); }
    public boolean hasLeft(UUID uuid) { return leftPostMatch.contains(uuid); }
    public boolean bothLeft() { return leftPostMatch.contains(player1) && leftPostMatch.contains(player2); }

    public ScheduledTask getCountdownTask() { return countdownTask; }
    public void setCountdownTask(ScheduledTask countdownTask) { this.countdownTask = countdownTask; }
    public ScheduledTask getReturnTask() { return returnTask; }
    public void setReturnTask(ScheduledTask returnTask) { this.returnTask = returnTask; }

    public void cancelTasks() {
        if (countdownTask != null) countdownTask.cancel();
        if (returnTask != null) returnTask.cancel();
    }
}

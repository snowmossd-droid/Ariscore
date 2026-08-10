package me.vennlmao.ariscore.duel.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.UUID;

public class DuelInvite {

    private final UUID inviter;
    private final UUID target;
    private final DuelArena arena;
    private ScheduledTask expireTask;

    public DuelInvite(UUID inviter, UUID target, DuelArena arena) {
        this.inviter = inviter;
        this.target = target;
        this.arena = arena;
    }

    public UUID getInviter() { return inviter; }
    public UUID getTarget() { return target; }
    public DuelArena getArena() { return arena; }
    public ScheduledTask getExpireTask() { return expireTask; }
    public void setExpireTask(ScheduledTask expireTask) { this.expireTask = expireTask; }

    public void cancelExpireTask() {
        if (expireTask != null) expireTask.cancel();
    }
}

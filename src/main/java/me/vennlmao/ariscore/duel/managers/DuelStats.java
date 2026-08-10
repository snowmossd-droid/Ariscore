package me.vennlmao.ariscore.duel.managers;

public class DuelStats {

    private final java.util.UUID uuid;
    private int wins;
    private int losses;
    private int draws;
    private int streak;
    private int bestStreak;

    public DuelStats(java.util.UUID uuid, int wins, int losses, int draws, int streak, int bestStreak) {
        this.uuid = uuid;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.streak = streak;
        this.bestStreak = bestStreak;
    }

    public java.util.UUID getUuid() { return uuid; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
    public int getStreak() { return streak; }
    public int getBestStreak() { return bestStreak; }

    public void addWin() {
        wins++;
        streak++;
        if (streak > bestStreak) bestStreak = streak;
    }

    public void addLoss() {
        losses++;
        streak = 0;
    }

    public void addDraw() {
        draws++;
    }

    public int getTotal() {
        return wins + losses + draws;
    }

    public double getWinRate() {
        int total = wins + losses;
        return total == 0 ? 0.0 : (wins * 100.0) / total;
    }
}

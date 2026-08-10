package me.vennlmao.ariscore.duel.managers;

import org.bukkit.Location;

public class DuelArena {

    private final String name;
    private Location pos1;
    private Location pos2;

    public DuelArena(String name, Location pos1, Location pos2) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public String getName() { return name; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }
}

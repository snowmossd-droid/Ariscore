package me.vennlmao.ariscore.tab.models;

import java.util.List;

public class ScoreboardProfile {
    private final String id;
    private final String displayCondition;
    private final String world;
    private final String title;
    private final List<String> lines;

    public ScoreboardProfile(String id, String displayCondition, String world, String title, List<String> lines) {
        this.id = id; this.displayCondition = displayCondition; this.world = world; this.title = title; this.lines = lines;
    }

    public String getId() { return id; }
    public String getDisplayCondition() { return displayCondition; }
    public String getWorld() { return world; }
    public String getTitle() { return title; }
    public List<String> getLines() { return lines; }
}
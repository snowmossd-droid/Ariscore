package me.vennlmao.ariscore.crates.models;

import java.util.Map;

public class KeyAllConfig {

    private final int intervalSeconds;
    private final Map<String, Integer> keyRewards;
    private final String title;
    private final String subtitle;

    public KeyAllConfig(int intervalSeconds, Map<String, Integer> keyRewards, String title, String subtitle) {
        this.intervalSeconds = intervalSeconds;
        this.keyRewards = keyRewards;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getIntervalSeconds() { return intervalSeconds; }
    public Map<String, Integer> getKeyRewards() { return keyRewards; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
}

package me.vennlmao.ariscore.crates.models;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CrateModel {

    private final String name;
    private final RewardsGuiConfig rewardsGuiConfig;
    private final List<RewardInfo> rewards;
    private final Set<Location> locations;

    public CrateModel(String name, RewardsGuiConfig rewardsGuiConfig, List<RewardInfo> rewards) {
        this.name = name;
        this.rewardsGuiConfig = rewardsGuiConfig;
        this.rewards = new ArrayList<>(rewards);
        this.locations = new HashSet<>();
    }

    public void addLocation(Location location) { locations.add(location); }
    public void removeLocation(Location location) { locations.remove(location); }
    public void clearLocations() { locations.clear(); }

    public String getName() { return name; }
    public RewardsGuiConfig getRewardsGuiConfig() { return rewardsGuiConfig; }
    public List<RewardInfo> getRewards() { return rewards; }
    public Set<Location> getLocations() { return locations; }
}

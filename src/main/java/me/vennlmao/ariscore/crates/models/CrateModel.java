package me.vennlmao.ariscore.crates.models;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CrateModel {

    private final String name;
    private final ItemStack icon;
    private final RewardsGuiConfig rewardsGuiConfig;
    private final ConfirmGuiConfig confirmGuiConfig;
    private final List<RewardInfo> rewards;
    private final Set<Location> locations;

    public CrateModel(String name, ItemStack icon,
                      RewardsGuiConfig rewardsGuiConfig,
                      ConfirmGuiConfig confirmGuiConfig,
                      List<RewardInfo> rewards) {
        this.name = name;
        this.icon = icon;
        this.rewardsGuiConfig = rewardsGuiConfig;
        this.confirmGuiConfig = confirmGuiConfig;
        this.rewards = new ArrayList<>(rewards);
        this.locations = new HashSet<>();
    }

    public void addLocation(Location location) { locations.add(location); }
    public void removeLocation(Location location) { locations.remove(location); }

    public String getName() { return name; }
    public ItemStack getIcon() { return icon; }
    public RewardsGuiConfig getRewardsGuiConfig() { return rewardsGuiConfig; }
    public ConfirmGuiConfig getConfirmGuiConfig() { return confirmGuiConfig; }
    public List<RewardInfo> getRewards() { return rewards; }
    public Set<Location> getLocations() { return locations; }
}

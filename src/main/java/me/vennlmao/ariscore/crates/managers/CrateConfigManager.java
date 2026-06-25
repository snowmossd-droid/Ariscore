package me.vennlmao.ariscore.crates.managers;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.ConfirmGuiConfig;
import me.vennlmao.ariscore.crates.models.CrateModel;
import me.vennlmao.ariscore.crates.models.GuiButton;
import me.vennlmao.ariscore.crates.models.RewardInfo;
import me.vennlmao.ariscore.crates.models.RewardsGuiConfig;
import me.vennlmao.ariscore.crates.utils.ColorUtil;
import me.vennlmao.ariscore.crates.utils.ItemBuilderUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CrateConfigManager {

    private final CratesModule module;

    public CrateConfigManager(CratesModule module) {
        this.module = module;
    }

    public void loadAll() {
        File cratesFolder = new File(module.getPlugin().getDataFolder(), "crates/crates");
        if (!cratesFolder.exists()) return;

        File[] files = cratesFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String crateName = file.getName().replace(".yml", "");
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                module.getCrateRegistry().cache(buildCrate(cfg, crateName));
            } catch (Exception e) {
                module.getPlugin().getLogger().warning("[Crates] Failed to load crate '" + crateName + "': " + e.getMessage());
            }
        }

        loadLocations();
    }

    private CrateModel buildCrate(FileConfiguration cfg, String name) {
        ItemStack icon = buildItem(cfg.getConfigurationSection("icon"));
        RewardsGuiConfig rewardsGui = buildRewardsGui(cfg.getConfigurationSection("rewards-gui"));
        ConfirmGuiConfig confirmGui = buildConfirmGui(cfg.getConfigurationSection("confirm-gui"));

        List<RewardInfo> rewards = new ArrayList<>();
        ConfigurationSection rewardsSection = cfg.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                ConfigurationSection s = rewardsSection.getConfigurationSection(key);
                if (s != null) rewards.add(buildReward(s));
            }
        }

        return new CrateModel(name, icon, rewardsGui, confirmGui, rewards);
    }

    private RewardsGuiConfig buildRewardsGui(ConfigurationSection s) {
        String name = ColorUtil.translate(s != null ? s.getString("name", "") : "");
        int rows = s != null ? s.getInt("rows", 3) : 3;
        ItemStack bg = s != null ? buildItem(s.getConfigurationSection("background")) : null;
        GuiButton cancel = s != null ? buildButton(s.getConfigurationSection("cancel-button")) : null;
        return new RewardsGuiConfig(name, rows, bg, cancel);
    }

    private ConfirmGuiConfig buildConfirmGui(ConfigurationSection s) {
        String name = ColorUtil.translate(s != null ? s.getString("name", "") : "");
        int rows = s != null ? s.getInt("rows", 3) : 3;
        ItemStack bg = s != null ? buildItem(s.getConfigurationSection("background")) : null;
        int rewardSlot = s != null ? s.getInt("reward-slot", 13) : 13;
        GuiButton cancel = s != null ? buildButton(s.getConfigurationSection("cancel-button")) : null;
        GuiButton confirm = s != null ? buildButton(s.getConfigurationSection("confirm-button")) : null;
        return new ConfirmGuiConfig(name, rows, bg, rewardSlot, cancel, confirm);
    }

    private GuiButton buildButton(ConfigurationSection s) {
        if (s == null) return null;
        return new GuiButton(s.getInt("slot", 0), buildItem(s));
    }

    private RewardInfo buildReward(ConfigurationSection s) {
        int slot = s.getInt("slot", 0);
        ItemStack icon = buildItem(s.getConfigurationSection("icon"));

        List<ItemStack> items = new ArrayList<>();
        List<?> itemList = s.getList("items");
        if (itemList != null) {
            for (Object obj : itemList) {
                if (obj instanceof java.util.Map<?, ?> map) {
                    ConfigurationSection itemSection = toSection(map);
                    if (itemSection != null) items.add(buildItem(itemSection));
                }
            }
        }

        return new RewardInfo(slot, icon, items);
    }

    private ItemStack buildItem(ConfigurationSection s) {
        if (s == null) return new ItemStack(org.bukkit.Material.STONE);
        return ItemBuilderUtil.fromSection(s);
    }

    @SuppressWarnings("unchecked")
    private ConfigurationSection toSection(java.util.Map<?, ?> map) {
        YamlConfiguration tmp = new YamlConfiguration();
        for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
            tmp.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        return tmp;
    }

    public void loadLocations() {
        File locFile = new File(module.getPlugin().getDataFolder(), "crates/locations.yml");
        if (!locFile.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(locFile);
        for (String crateName : cfg.getKeys(false)) {
            CrateModel crate = module.getCrateRegistry().find(crateName);
            if (crate == null) continue;
            ConfigurationSection crateSection = cfg.getConfigurationSection(crateName);
            if (crateSection == null) continue;
            for (String locKey : crateSection.getKeys(false)) {
                ConfigurationSection loc = crateSection.getConfigurationSection(locKey);
                if (loc == null) continue;
                String worldName = loc.getString("world");
                if (worldName == null) continue;
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                crate.addLocation(new Location(world, loc.getInt("x"), loc.getInt("y"), loc.getInt("z")));
            }
        }
    }

    public void saveLocation(CrateModel crate, Location loc) {
        File locFile = new File(module.getPlugin().getDataFolder(), "crates/locations.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(locFile);
        String key = crate.getName() + "." + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        cfg.set(key + ".world", loc.getWorld().getName());
        cfg.set(key + ".x", loc.getBlockX());
        cfg.set(key + ".y", loc.getBlockY());
        cfg.set(key + ".z", loc.getBlockZ());
        trySave(cfg, locFile);
    }

    public void removeLocation(CrateModel crate, Location loc) {
        File locFile = new File(module.getPlugin().getDataFolder(), "crates/locations.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(locFile);
        cfg.set(crate.getName() + "." + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ(), null);
        trySave(cfg, locFile);
    }

    private void trySave(FileConfiguration cfg, File file) {
        try { cfg.save(file); }
        catch (Exception e) { module.getPlugin().getLogger().warning("[Crates] Failed to save locations: " + e.getMessage()); }
    }
}

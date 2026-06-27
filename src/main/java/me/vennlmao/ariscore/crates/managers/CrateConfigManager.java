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
        RewardsGuiConfig rewardsGui = buildRewardsGui(cfg.getConfigurationSection("rewards-gui"));

        List<RewardInfo> rewards = new ArrayList<>();
        ConfigurationSection rewardsSection = cfg.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                ConfigurationSection s = rewardsSection.getConfigurationSection(key);
                if (s != null) rewards.add(buildReward(s));
            }
        }

        return new CrateModel(name, rewardsGui, rewards);
    }

    private RewardsGuiConfig buildRewardsGui(ConfigurationSection s) {
        ConfigurationSection defaults = module.getConfig().getConfigurationSection("crate-defaults");

        String defaultName = defaults != null ? defaults.getString("name", "&8\u1d04\u029c\u1d0f\u1d0f\ua730\u1d07 1 \u026a\u1d1b\u1d07\u1d0d") : "&8\u1d04\u029c\u1d0f\u1d0f\ua730\u1d07 1 \u026a\u1d1b\u1d07\u1d0d";
        int defaultSize = defaults != null ? defaults.getInt("size", 27) : 27;
        boolean defaultBackgroundEnabled = defaults != null && defaults.getBoolean("background", false);
        String defaultMaterial = defaults != null ? defaults.getString("material", "BLACK_STAINED_GLASS_PANE") : "BLACK_STAINED_GLASS_PANE";

        String name = ColorUtil.translate(s != null ? s.getString("name", defaultName) : defaultName);
        int size = s != null ? s.getInt("size", defaultSize) : defaultSize;

        ItemStack bg = null;
        boolean backgroundEnabled = s != null ? s.getBoolean("background", defaultBackgroundEnabled) : defaultBackgroundEnabled;
        if (backgroundEnabled) {
            String material = s != null ? s.getString("material", defaultMaterial) : defaultMaterial;
            bg = ItemBuilderUtil.plainItem(org.bukkit.Material.matchMaterial(material) != null
                    ? org.bukkit.Material.matchMaterial(material) : org.bukkit.Material.BLACK_STAINED_GLASS_PANE, " ");
        }

        GuiButton cancel = s != null ? buildButton(s.getConfigurationSection("cancel-button")) : null;
        return new RewardsGuiConfig(name, size, bg, cancel);
    }

    public ConfirmGuiConfig buildGlobalConfirmGui() {
        ConfigurationSection s = module.getConfig().getConfigurationSection("gui-confirm");

        String name = ColorUtil.translate(s != null ? s.getString("title", "&8\u1d04\u1d0f\u0274\u0493\u026a\u0280\u1d0d") : "&8\u1d04\u1d0f\u0274\u0493\u026a\u0280\u1d0d");
        int size = s != null ? s.getInt("size", 27) : 27;

        ItemStack bg = null;
        boolean backgroundEnabled = s != null && s.getBoolean("background", false);
        if (backgroundEnabled) {
            String material = s != null ? s.getString("material", "BLACK_STAINED_GLASS_PANE") : "BLACK_STAINED_GLASS_PANE";
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(material);
            bg = ItemBuilderUtil.plainItem(mat != null ? mat : org.bukkit.Material.BLACK_STAINED_GLASS_PANE, " ");
        }

        ConfigurationSection items = s != null ? s.getConfigurationSection("items") : null;
        int rewardSlot = items != null ? items.getInt("reward.slot", 13) : 13;

        GuiButton cancel = items != null ? buildButton(items.getConfigurationSection("cancel")) : null;
        GuiButton confirm = items != null ? buildButton(items.getConfigurationSection("confirm")) : null;

        return new ConfirmGuiConfig(name, size, bg, rewardSlot, cancel, confirm);
    }

    private GuiButton buildButton(ConfigurationSection s) {
        if (s == null) return null;
        return new GuiButton(s.getInt("slot", 0), buildItem(s));
    }

    private RewardInfo buildReward(ConfigurationSection s) {
        int slot = s.getInt("slot", 0);
        ItemStack icon = readItemStack(s, "icon");

        List<ItemStack> items = new ArrayList<>();
        List<?> itemList = s.getList("items");
        if (itemList != null) {
            for (Object obj : itemList) {
                if (obj instanceof ItemStack item) {
                    items.add(item);
                }
            }
        }

        return new RewardInfo(slot, icon != null ? icon : new ItemStack(org.bukkit.Material.STONE), items);
    }

    private ItemStack readItemStack(ConfigurationSection parent, String path) {
        return parent.getItemStack(path);
    }

    private ItemStack buildItem(ConfigurationSection s) {
        if (s == null) return new ItemStack(org.bukkit.Material.STONE);
        return ItemBuilderUtil.fromSection(s);
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

    public void removeAllLocations(CrateModel crate) {
        File locFile = new File(module.getPlugin().getDataFolder(), "crates/locations.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(locFile);
        cfg.set(crate.getName(), null);
        trySave(cfg, locFile);
    }

    private void trySave(FileConfiguration cfg, File file) {
        try {
            cfg.save(file);
        } catch (Exception e) {
            module.getPlugin().getLogger().warning("[Crates] Failed to save locations: " + e.getMessage());
        }
    }

    public File getCrateFile(String crateName) {
        return new File(module.getPlugin().getDataFolder(), "crates/crates/" + crateName + ".yml");
    }

    public boolean crateFileExists(String crateName) {
        return getCrateFile(crateName).exists();
    }

    public CrateModel createCrateFile(String crateName) {
        File file = getCrateFile(crateName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        FileConfiguration cfg = new YamlConfiguration();
        trySave(cfg, file);

        return buildCrate(cfg, crateName);
    }

    public String findRewardId(String crateName, int slot) {
        File file = getCrateFile(crateName);
        if (!file.exists()) return "reward_" + slot;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection rewardsSection = cfg.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                ConfigurationSection s = rewardsSection.getConfigurationSection(key);
                if (s != null && s.getInt("slot", -1) == slot) return key;
            }
        }
        return "reward_" + slot;
    }

    public void setRewardItem(String crateName, String rewardId, int slot, ItemStack item) {
        File file = getCrateFile(crateName);
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String path = "rewards." + rewardId;

        cfg.set(path + ".slot", slot);
        cfg.set(path + ".icon", item);
        cfg.set(path + ".items", List.of(item));

        try {
            cfg.save(file);
        } catch (Exception e) {
            module.getPlugin().getLogger().warning("[Crates] Failed to save crate '" + crateName + "': " + e.getMessage());
        }
    }

    public void clearReward(String crateName, String rewardId) {
        File file = getCrateFile(crateName);
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("rewards." + rewardId, null);

        try {
            cfg.save(file);
        } catch (Exception e) {
            module.getPlugin().getLogger().warning("[Crates] Failed to save crate '" + crateName + "': " + e.getMessage());
        }
    }
}

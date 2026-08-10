package me.vennlmao.ariscore.spawners.managers;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SpawnerDefinitionManager {

    private final SpawnersModule module;
    private final Map<EntityType, MobSpawnerDefinition> definitions = new HashMap<>();
    private File folder;

    public SpawnerDefinitionManager(SpawnersModule module) {
        this.module = module;
    }

    public void load() {
        folder = new File(module.getPlugin().getDataFolder(), "spawners/spawners");
        if (!folder.exists()) folder.mkdirs();

        extractBundledDefaults();

        definitions.clear();
        File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                MobSpawnerDefinition def = parse(file);
                if (def != null) definitions.put(def.getEntityType(), def);
            } catch (Exception e) {
                module.getPlugin().getLogger().warning("[Spawners] Không thể đọc file: " + file.getName() + " - " + e.getMessage());
            }
        }
        module.getPlugin().getLogger().info("[Spawners] Đã tải " + definitions.size() + " loại spawner.");
    }

    private MobSpawnerDefinition parse(File file) {
        String baseName = file.getName().substring(0, file.getName().length() - 4);
        EntityType type;
        try {
            type = EntityType.valueOf(baseName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String spawnerName = cfg.getString("spawner-name", baseName);
        String title = cfg.getString("title", spawnerName);
        String material = cfg.getString("material", "SPAWNER");
        int time = cfg.getInt("time", 60);
        String displayName = cfg.getString("displayname", cfg.getString("display-name", spawnerName));
        List<String> lore = cfg.getStringList("lore");
        long xpAmount = cfg.getLong("xp-amount", 0);

        List<Material> layout = new ArrayList<>();
        for (String s : cfg.getStringList("item-layout-order")) {
            try {
                layout.add(Material.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        Map<Material, Long> drops = new LinkedHashMap<>();
        org.bukkit.configuration.ConfigurationSection dropsSection = cfg.getConfigurationSection("drops");
        if (dropsSection != null) {
            for (String key : dropsSection.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    drops.put(mat, dropsSection.getLong(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (layout.isEmpty()) layout.addAll(drops.keySet());

        return new MobSpawnerDefinition(type, spawnerName, title, material, time, displayName, lore, xpAmount, layout, drops);
    }

    private void extractBundledDefaults() {
        try {
            URL location = module.getPlugin().getClass().getProtectionDomain().getCodeSource().getLocation();
            File jarFile = new File(location.toURI());
            if (!jarFile.isFile()) return;

            try (JarFile jar = new JarFile(jarFile)) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith("spawners/spawners/") || entry.isDirectory() || !name.endsWith(".yml")) continue;

                    String fileName = name.substring("spawners/spawners/".length());
                    File target = new File(folder, fileName);
                    if (target.exists()) continue;

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception e) {
            module.getPlugin().getLogger().warning("[Spawners] Không thể trích xuất dữ liệu spawner mặc định: " + e.getMessage());
        }
    }

    public MobSpawnerDefinition get(EntityType type) {
        return definitions.get(type);
    }

    public boolean has(EntityType type) {
        return definitions.containsKey(type);
    }

    public Map<EntityType, MobSpawnerDefinition> getAll() {
        return definitions;
    }
}

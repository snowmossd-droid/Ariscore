package me.vennlmao.ariscore.spawners.managers;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MobSpawnerDefinition {

    private final EntityType entityType;
    private final String spawnerName;
    private final String title;
    private final String material;
    private final int timeSeconds;
    private final String displayName;
    private final List<String> lore;
    private final long xpAmount;
    private final List<Material> itemLayoutOrder;
    private final Map<Material, Long> drops;

    public MobSpawnerDefinition(EntityType entityType, String spawnerName, String title, String material,
                                 int timeSeconds, String displayName, List<String> lore, long xpAmount,
                                 List<Material> itemLayoutOrder, Map<Material, Long> drops) {
        this.entityType = entityType;
        this.spawnerName = spawnerName;
        this.title = title;
        this.material = material;
        this.timeSeconds = timeSeconds;
        this.displayName = displayName;
        this.lore = lore;
        this.xpAmount = xpAmount;
        this.itemLayoutOrder = itemLayoutOrder;
        this.drops = new LinkedHashMap<>(drops);
    }

    public EntityType getEntityType() { return entityType; }
    public String getSpawnerName() { return spawnerName; }
    public String getTitle() { return title; }
    public String getMaterial() { return material; }
    public int getTimeSeconds() { return timeSeconds; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public long getXpAmount() { return xpAmount; }
    public List<Material> getItemLayoutOrder() { return itemLayoutOrder; }
    public Map<Material, Long> getDrops() { return drops; }
}

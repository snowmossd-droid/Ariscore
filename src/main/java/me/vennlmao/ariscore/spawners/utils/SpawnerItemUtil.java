package me.vennlmao.ariscore.spawners.utils;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import me.vennlmao.ariscore.spawners.managers.MobSpawnerDefinition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SpawnerItemUtil {

    private static SpawnersModule module;
    private static NamespacedKey typeKey;
    private static NamespacedKey amountKey;

    public static void init(SpawnersModule pl) {
        module = pl;
        typeKey = new NamespacedKey(pl.getPlugin(), "spawner_entity_type");
        amountKey = new NamespacedKey(pl.getPlugin(), "spawner_stack_amount");
    }

    public static ItemStack createItem(EntityType type, long amount) {
        MobSpawnerDefinition def = module.getSpawnerDefinitionManager().get(type);
        ItemStack item = def != null
                ? SpawnerSkullUtil.fromMaterialSpec(def.getMaterial())
                : new ItemStack(Material.SPAWNER);
        item.setAmount(1);

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(amountKey, PersistentDataType.LONG, amount);

        String display = def != null ? def.getDisplayName() : mobName(type);
        meta.displayName(ColorUtil.parse(display.replace("{amount}", String.valueOf(amount))));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        if (def != null) {
            for (String line : def.getLore()) {
                lore.add(ColorUtil.parse(line.replace("{amount}", String.valueOf(amount))));
            }
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isSpawnerItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING);
    }

    public static EntityType getEntityType(ItemStack item) {
        if (!isSpawnerItem(item)) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        try {
            return EntityType.valueOf(raw);
        } catch (Exception e) {
            return null;
        }
    }

    public static long getStackAmount(ItemStack item) {
        if (!isSpawnerItem(item)) return 1;
        Long amount = item.getItemMeta().getPersistentDataContainer().get(amountKey, PersistentDataType.LONG);
        return amount != null ? amount : 1;
    }

    public static String mobName(EntityType type) {
        MobSpawnerDefinition def = module.getSpawnerDefinitionManager().get(type);
        if (def != null) return def.getSpawnerName();
        String raw = type.name().replace("_", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}

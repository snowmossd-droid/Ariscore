package me.vennlmao.ariscore.amethyst.managers;

import me.vennlmao.ariscore.amethyst.AmethystModule;
import me.vennlmao.ariscore.amethyst.utils.ColorUtil;
import me.vennlmao.ariscore.amethyst.utils.TimeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AmethystItemManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final AmethystModule module;
    private final NamespacedKey toolTypeKey;
    private final NamespacedKey expiryKey;
    private final NamespacedKey unlimitedKey;
    private final Map<UUID, Long> shardBoostExpiry = new HashMap<>();

    public AmethystItemManager(AmethystModule module) {
        this.module = module;
        this.toolTypeKey = new NamespacedKey(module.getPlugin(), "amethyst_tool_type");
        this.expiryKey = new NamespacedKey(module.getPlugin(), "amethyst_expiry");
        this.unlimitedKey = new NamespacedKey(module.getPlugin(), "amethyst_unlimited");
    }

    public ItemStack createTool(String toolId) {
        ConfigurationSection section = module.getConfig().getConfigurationSection("tools." + toolId);
        if (section == null) return null;

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = section.getString("display-name", toolId);
        meta.displayName(legacy(name));

        List<String> lore = section.getStringList("lore");
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(legacy(line));
        }
        meta.lore(loreComponents);

        ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String enchName : enchantSection.getKeys(false)) {
                Enchantment enchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchName.toLowerCase()));
                if (enchant != null) meta.addEnchant(enchant, enchantSection.getInt(enchName, 1), true);
            }
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(toolTypeKey, PersistentDataType.STRING, toolId);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createFireworkItem(String configPath, String toolType) {
        ConfigurationSection section = module.getConfig().getConfigurationSection(configPath);
        if (section == null) return null;

        Material material = Material.matchMaterial(section.getString("material", "FIREWORK_ROCKET"));
        if (material == null) material = Material.FIREWORK_ROCKET;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(legacy(section.getString("display-name", toolType)));

        List<String> lore = section.getStringList("lore");
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(legacy(line));
        }
        meta.lore(loreComponents);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(unlimitedKey, PersistentDataType.BOOLEAN, true);
        pdc.set(toolTypeKey, PersistentDataType.STRING, toolType);

        item.setItemMeta(meta);
        return item;
    }

    public String getToolType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(toolTypeKey, PersistentDataType.STRING);
    }

    public boolean isUnlimited(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(unlimitedKey, PersistentDataType.BOOLEAN, false);
    }

    public boolean canSetExpiry(ItemStack item) {
        String toolType = getToolType(item);
        if (toolType == null) return false;
        List<String> applicable = module.getConfig().getStringList("self-destruct.applies-to");
        return applicable.contains(toolType);
    }

    public boolean setExpiry(ItemStack item, long durationMillis) {
        if (!canSetExpiry(item)) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(expiryKey, PersistentDataType.LONG, System.currentTimeMillis() + durationMillis);
        item.setItemMeta(meta);

        refreshLore(item);
        return true;
    }

    public boolean hasExpiry(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(expiryKey, PersistentDataType.LONG);
    }

    public long getExpiry(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1L;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(expiryKey, PersistentDataType.LONG, -1L);
    }

    public boolean isExpired(ItemStack item) {
        if (!hasExpiry(item)) return false;
        return getExpiry(item) <= System.currentTimeMillis();
    }

    public void refreshLore(ItemStack item) {
        String toolType = getToolType(item);
        if (toolType == null || toolType.equals("firework")) return;

        ConfigurationSection section = module.getConfig().getConfigurationSection("tools." + toolType);
        if (section == null) return;

        ItemMeta meta = item.getItemMeta();
        List<String> baseLore = section.getStringList("lore");
        List<Component> loreComponents = new ArrayList<>();
        for (String line : baseLore) {
            loreComponents.add(legacy(line));
        }

        if (hasExpiry(item)) {
            long remaining = getExpiry(item) - System.currentTimeMillis();
            String template = module.getConfig().getString("self-destruct.countdown-lore-line", "&7Self-destructs in: &c{time}");
            String timeStr = remaining > 0 ? TimeParser.formatTimeLeft(remaining) : "0s";
            loreComponents.add(legacy(template.replace("{time}", timeStr)));
        }

        meta.lore(loreComponents);
        item.setItemMeta(meta);
    }

    public void activateShardBoost(Player player) {
        double durationHours = module.getConfig().getDouble("tools.booster.boost-duration-hours", 24.0);
        long durationMillis = (long) (durationHours * 3600000L);
        shardBoostExpiry.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    public boolean isShardBoostActive(UUID uuid) {
        Long expiry = shardBoostExpiry.get(uuid);
        if (expiry == null) return false;
        if (expiry <= System.currentTimeMillis()) {
            shardBoostExpiry.remove(uuid);
            return false;
        }
        return true;
    }

    private Component legacy(String text) {
        return LEGACY.deserialize(ColorUtil.translate(text))
                .decoration(TextDecoration.ITALIC, false);
    }
    }
                 

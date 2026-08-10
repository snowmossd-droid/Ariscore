package me.vennlmao.ariscore.spawners.gui;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import me.vennlmao.ariscore.spawners.managers.MobSpawnerDefinition;
import me.vennlmao.ariscore.spawners.managers.SpawnerData;
import me.vennlmao.ariscore.spawners.utils.ColorUtil;
import me.vennlmao.ariscore.spawners.utils.GuiConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpawnerGuiBuilder {

    public static Inventory buildInfo(SpawnersModule module, SpawnerData data) {
        FileConfiguration gui = module.getGuiConfig();
        MobSpawnerDefinition def = module.getSpawnerDefinitionManager().get(data.getEntityType());

        int size = gui.getInt("gui.info.size", 27);
        String title = def != null ? def.getTitle().replace("%amount%", String.valueOf(data.getAmount())) : "Spawner";

        SpawnerGuiHolder holder = new SpawnerGuiHolder(SpawnerGuiHolder.Screen.INFO, data, null, 0);
        Inventory inv = Bukkit.createInventory(holder, size, ColorUtil.parse(title));
        holder.setInventory(inv);

        String base = "gui.info.items.open-storage";
        int slot = gui.getInt(base + ".slot", 11);
        ItemStack icon = safeMaterial(gui.getString(base + ".material", "CHEST"));
        applyDisplay(icon, ColorUtil.parse(GuiConfigUtil.getName(gui, base).replace("{amount}", String.valueOf(data.getAmount()))));
        applyLoreRepeatedPerItem(module, icon, GuiConfigUtil.getLore(gui, base + ".lore"), data, def);
        inv.setItem(slot, icon);

        base = "gui.info.items.fullness";
        slot = gui.getInt(base + ".slot", 13);
        String matName = gui.getString(base + ".material", null);
        ItemStack fullness = matName != null
                ? safeMaterial(matName)
                : (def != null ? me.vennlmao.ariscore.spawners.utils.SpawnerSkullUtil.fromMaterialSpec(def.getMaterial()) : new ItemStack(Material.PAPER));
        applyDisplay(fullness, ColorUtil.parse(fillPlaceholders(module, GuiConfigUtil.getName(gui, base), data, def, -1)));
        List<String> fullnessLore = new ArrayList<>();
        for (String line : GuiConfigUtil.getLore(gui, base + ".lore")) {
            fullnessLore.add(fillPlaceholders(module, line, data, def, -1));
        }
        applyLore(fullness, fullnessLore);
        inv.setItem(slot, fullness);

        base = "gui.info.items.collect-xp";
        slot = gui.getInt(base + ".slot", 15);
        ItemStack xpItem = safeMaterial(gui.getString(base + ".material", "EXPERIENCE_BOTTLE"));
        applyDisplay(xpItem, ColorUtil.parse(GuiConfigUtil.getName(gui, base)));
        List<String> xpLore = new ArrayList<>();
        long maxXp = module.getConfig().getLong("xp.max-amount", 1000000);
        for (String line : GuiConfigUtil.getLore(gui, base + ".lore")) {
            xpLore.add(line.replace("%xp_amount%", String.valueOf(data.getStoredXp())).replace("%max_xp_amount%", String.valueOf(maxXp)));
        }
        applyLore(xpItem, xpLore);
        inv.setItem(slot, xpItem);

        return inv;
    }

    public static Inventory buildStorage(SpawnersModule module, SpawnerData data, int page) {
        FileConfiguration gui = module.getGuiConfig();
        MobSpawnerDefinition def = module.getSpawnerDefinitionManager().get(data.getEntityType());

        int size = gui.getInt("storage.size", 54);
        String title = def != null ? def.getTitle().replace("%amount%", String.valueOf(data.getAmount())) : "Storage";

        List<Material> materials = def != null && !def.getItemLayoutOrder().isEmpty()
                ? def.getItemLayoutOrder()
                : new ArrayList<>(data.getStorage().keySet());
        int maxPage = Math.max(0, materials.size() - 1);
        page = Math.max(0, Math.min(page, maxPage));

        SpawnerGuiHolder holder = new SpawnerGuiHolder(SpawnerGuiHolder.Screen.STORAGE, data, null, page);
        Inventory inv = Bukkit.createInventory(holder, size, ColorUtil.parse(title));
        holder.setInventory(inv);

        String base = "storage.storage";
        int slot = gui.getInt(base + ".slot", 49);
        ItemStack storageIcon = new ItemStack(Material.CHEST);
        long maxPerMaterial = module.getConfig().getLong("storage.max-per-material", 999999999L);
        if (!materials.isEmpty()) {
            Material mat = materials.get(page);
            long qty = data.getStoredCount(mat);
            storageIcon = new ItemStack(mat, (int) Math.max(1, Math.min(qty, 64)));
            double pct = maxPerMaterial > 0 ? (qty * 100.0 / maxPerMaterial) : 0;
            applyDisplay(storageIcon, ColorUtil.parse(fillPlaceholders(module, GuiConfigUtil.getName(gui, base), data, def, -1)));
            List<String> lore = new ArrayList<>();
            for (String line : GuiConfigUtil.getLore(gui, base + ".lore")) {
                lore.add(line
                        .replace("%amount%", String.valueOf(qty))
                        .replace("%item%", niceName(mat))
                        .replace("%pct%", String.format("%.0f", pct))
                        .replace("%spawner_name%", def != null ? def.getSpawnerName() : ""));
            }
            applyLore(storageIcon, lore);
        }
        inv.setItem(slot, storageIcon);

        putSimpleButton(gui, inv, "storage.back", data, def, module);
        putSimpleButton(gui, inv, "storage.previous-page", data, def, module);
        putSimpleButton(gui, inv, "storage.next-page", data, def, module);
        putSimpleButton(gui, inv, "storage.drop-all", data, def, module);
        putSimpleButton(gui, inv, "storage.sell-all", data, def, module);

        return inv;
    }

    public static Inventory buildConfirm(SpawnersModule module, SpawnerData data, SpawnerGuiHolder.Screen returnTo) {
        FileConfiguration gui = module.getGuiConfig();
        MobSpawnerDefinition def = module.getSpawnerDefinitionManager().get(data.getEntityType());

        int rows = Math.max(1, Math.min(6, gui.getInt("confirm-gui.rows", 3)));
        String title = gui.getString("confirm-gui.title", "Confirm Sell");

        SpawnerGuiHolder holder = new SpawnerGuiHolder(SpawnerGuiHolder.Screen.CONFIRM, data, returnTo, 0);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, ColorUtil.parse(title));
        holder.setInventory(inv);

        putSimpleButton(gui, inv, "confirm-gui.items.decline", data, def, module);

        String base = "confirm-gui.items.contents";
        int slot = gui.getInt(base + ".slot", 13);
        ItemStack contents = safeMaterial(gui.getString(base + ".material", "CHEST"));
        applyDisplay(contents, ColorUtil.parse(GuiConfigUtil.getName(gui, base)));
        applyLoreRepeatedPerStoredItem(contents, GuiConfigUtil.getLore(gui, base + ".lore"), data);
        inv.setItem(slot, contents);

        base = "confirm-gui.items.confirm";
        slot = gui.getInt(base + ".slot", 16);
        ItemStack confirm = safeMaterial(gui.getString(base + ".material", "LIME_STAINED_GLASS_PANE"));
        applyDisplay(confirm, ColorUtil.parse(GuiConfigUtil.getName(gui, base)));
        double value = module.getSpawnerManager().totalStorageValue(data);
        String valueStr = formatValue(module, value);
        List<String> lore = new ArrayList<>();
        for (String line : GuiConfigUtil.getLore(gui, base + ".lore")) {
            lore.add(line.replace("%items_value%", valueStr));
        }
        applyLore(confirm, lore);
        inv.setItem(slot, confirm);

        return inv;
    }

    public static String formatValue(SpawnersModule module, double value) {
        me.vennlmao.ariscore.ArisCore core = (me.vennlmao.ariscore.ArisCore) module.getPlugin();
        if (core.getSellModule() != null && core.getSellModule().getEconomy() != null) {
            return core.getSellModule().getEconomy().format(value);
        }
        return String.format("%.2f", value);
    }

    private static void putSimpleButton(FileConfiguration gui, Inventory inv, String base, SpawnerData data, MobSpawnerDefinition def, SpawnersModule module) {
        int slot = gui.getInt(base + ".slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;
        ItemStack item = safeMaterial(gui.getString(base + ".material", "STONE"));
        applyDisplay(item, ColorUtil.parse(fillPlaceholders(module, GuiConfigUtil.getName(gui, base), data, def, -1)));
        List<String> lore = new ArrayList<>();
        for (String line : GuiConfigUtil.getLore(gui, base + ".lore")) {
            lore.add(fillPlaceholders(module, line, data, def, -1));
        }
        applyLore(item, lore);
        inv.setItem(slot, item);
    }

    private static String fillPlaceholders(SpawnersModule module, String text, SpawnerData data, MobSpawnerDefinition def, double pctOverride) {
        double pct = pctOverride;
        if (pct < 0) pct = computeOverallPct(module, data, def);
        return text
                .replace("%amount%", String.valueOf(data.getAmount()))
                .replace("%spawner_name%", def != null ? def.getSpawnerName() : "")
                .replace("%pct%", String.format("%.0f", pct));
    }

    private static double computeOverallPct(SpawnersModule module, SpawnerData data, MobSpawnerDefinition def) {
        if (def == null || def.getDrops().isEmpty()) return 0;
        long maxPerMaterial = module.getConfig().getLong("storage.max-per-material", 999999999L);
        long capacity = maxPerMaterial * def.getDrops().size();
        if (capacity <= 0) return 0;
        return Math.min(100.0, data.totalStoredItems() * 100.0 / capacity);
    }

    private static void applyLoreRepeatedPerItem(SpawnersModule module, ItemStack item, List<String> template, SpawnerData data, MobSpawnerDefinition def) {
        List<String> lines = new ArrayList<>();
        List<Material> layout = def != null ? def.getItemLayoutOrder() : List.of();
        for (String templateLine : template) {
            if (templateLine.contains("%item%")) {
                if (layout.isEmpty()) continue;
                for (Material mat : layout) {
                    long qty = data.getStoredCount(mat);
                    lines.add(templateLine.replace("%amount%", String.valueOf(qty)).replace("%item%", niceName(mat)));
                }
            } else {
                lines.add(fillPlaceholders(module, templateLine, data, def, -1));
            }
        }
        applyLore(item, lines);
    }

    private static void applyLoreRepeatedPerStoredItem(ItemStack item, List<String> template, SpawnerData data) {
        List<String> lines = new ArrayList<>();
        for (String templateLine : template) {
            if (templateLine.contains("%item%")) {
                if (data.getStorage().isEmpty()) {
                    lines.add(templateLine.replace("%amount%", "0").replace("%item%", "-"));
                    continue;
                }
                for (Map.Entry<Material, Long> entry : data.getStorage().entrySet()) {
                    if (entry.getValue() <= 0) continue;
                    lines.add(templateLine.replace("%amount%", String.valueOf(entry.getValue())).replace("%item%", niceName(entry.getKey())));
                }
            } else {
                lines.add(templateLine);
            }
        }
        applyLore(item, lines);
    }

    private static ItemStack safeMaterial(String name) {
        try {
            return new ItemStack(Material.valueOf(name.toUpperCase()));
        } catch (Exception e) {
            return new ItemStack(Material.STONE);
        }
    }

    private static void applyDisplay(ItemStack item, net.kyori.adventure.text.Component name) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
    }

    private static void applyLore(ItemStack item, List<String> lines) {
        ItemMeta meta = item.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lines) lore.add(ColorUtil.parse(line));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private static String niceName(Material material) {
        String raw = material.name().replace("_", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}

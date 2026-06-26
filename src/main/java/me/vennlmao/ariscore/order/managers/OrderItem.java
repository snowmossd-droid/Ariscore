package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.order.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderItem {

    private UUID orderUuid;
    private final String name;
    private final List<String> description;
    private final double pricePerItem;
    private final Material material;
    private final String itemId;
    private final List<String> commands;
    private final UUID creator;
    private final Date creationDate;
    private Date expireDate;
    private Date deletionDate;
    private boolean active;
    private boolean expiredNotified;
    private String itemType;
    private String subType;
    private int requestedAmount;
    private int deliveredAmount;
    private int collectedAmount;
    private double paidAmount;
    private int deliveryTime;
    private boolean wasFullyDelivered;

    public OrderItem(String name, List<String> description, double pricePerItem, Material material,
                     String itemId, List<String> commands, UUID creator, int requestedAmount) {
        this(name, description, pricePerItem, material, itemId, commands, creator, null, null, requestedAmount);
    }

    public OrderItem(String name, List<String> description, double pricePerItem, Material material,
                     String itemId, List<String> commands, UUID creator,
                     String itemType, String subType, int requestedAmount) {
        this.name = name;
        this.description = description != null ? description : new ArrayList<>();
        this.pricePerItem = pricePerItem;
        this.material = material;
        this.itemId = itemId;
        this.commands = commands != null ? commands : new ArrayList<>();
        this.creator = creator;
        this.creationDate = new Date();
        this.requestedAmount = requestedAmount;
        this.deliveredAmount = 0;
        this.collectedAmount = 0;
        this.paidAmount = pricePerItem * requestedAmount;
        this.itemType = itemType;
        this.subType = subType;
        this.wasFullyDelivered = false;
        this.expiredNotified = false;
        this.active = true;
    }

    public void initExpiry(int expireSeconds, int deletionSeconds) {
        this.deliveryTime = expireSeconds / 3600;
        this.expireDate = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
        this.deletionDate = new Date(System.currentTimeMillis() + (long) (expireSeconds + deletionSeconds) * 1000L);
    }

    public UUID getOrderUuid() { return orderUuid; }
    public void setOrderUuid(UUID uuid) { this.orderUuid = uuid; }
    public String getName() { return name; }
    public List<String> getDescription() { return new ArrayList<>(description); }
    public double getPricePerItem() { return pricePerItem; }
    public double getTotalPrice() { return pricePerItem * requestedAmount; }
    public Material getMaterial() { return material; }
    public String getItemId() { return itemId; }
    public List<String> getCommands() { return new ArrayList<>(commands); }
    public UUID getCreator() { return creator; }
    public Date getCreationDate() { return creationDate; }
    public Date getExpireDate() { return expireDate; }
    public Date getDeletionDate() { return deletionDate; }
    public boolean isActive() { return active; }
    public boolean isExpired() { return new Date().after(expireDate); }
    public boolean isFullyDelivered() { return deliveredAmount >= requestedAmount; }
    public boolean wasFullyDelivered() { return wasFullyDelivered; }
    public boolean isExpiredNotified() { return expiredNotified; }
    public int getRequestedAmount() { return requestedAmount; }
    public int getDeliveredAmount() { return deliveredAmount; }
    public int getCollectedAmount() { return collectedAmount; }
    public int getRemainingAmount() { return Math.max(0, requestedAmount - deliveredAmount); }
    public int getAvailableToCollect() { return Math.max(0, deliveredAmount - collectedAmount); }
    public double getPaidAmount() { return paidAmount; }
    public double getRemainingPayment() { return Math.max(0.0, getTotalPrice() - paidAmount); }
    public int getDeliveryTime() { return deliveryTime; }
    public String getItemType() { return itemType; }
    public String getSubType() { return subType; }
    public boolean isSpecialItem() { return itemType != null && !itemType.isEmpty(); }

    public void setActive(boolean active) { this.active = active; }
    public void setExpireDate(Date date) { this.expireDate = date; }
    public void setDeletionDate(Date date) { this.deletionDate = date; }
    public void setExpiredNotified(boolean v) { this.expiredNotified = v; }
    public void setDeliveryTime(int hours) {
        this.deliveryTime = hours;
        this.expireDate = new Date(creationDate.getTime() + hours * 3600000L);
    }
    public void setRequestedAmount(int n) { this.requestedAmount = Math.max(0, n); }
    public void setDeliveredAmount(int n) { this.deliveredAmount = Math.max(0, Math.min(n, requestedAmount)); }
    public void setCollectedAmount(int n) { this.collectedAmount = Math.max(0, Math.min(n, deliveredAmount)); }
    public void setPaidAmount(double n) { this.paidAmount = Math.max(0.0, Math.min(n, getTotalPrice())); }
    public void setItemType(String v) { this.itemType = v; }
    public void setSubType(String v) { this.subType = v; }

    public void addDeliveredAmount(int amount) {
        deliveredAmount = Math.min(requestedAmount, deliveredAmount + Math.max(0, amount));
        if (deliveredAmount >= requestedAmount) wasFullyDelivered = true;
    }
    public void addCollectedAmount(int amount) {
        collectedAmount = Math.min(deliveredAmount, collectedAmount + Math.max(0, amount));
    }
    public void addPaidAmount(double amount) {
        paidAmount = Math.min(getTotalPrice(), paidAmount + Math.max(0.0, amount));
    }

    public boolean shouldBeDeleted() {
        return new Date().after(deletionDate) || (isExpired() && getAvailableToCollect() == 0 && getRemainingAmount() == 0);
    }
    public boolean shouldNotifyExpired() { return isExpired() && !expiredNotified; }
    public boolean shouldBeInYourOrders() { return isActive() && !isExpired() && (getRemainingAmount() > 0 || getAvailableToCollect() > 0); }
    public boolean isCompleted() { return !active || (wasFullyDelivered && getAvailableToCollect() == 0); }
    public boolean shouldOrderBeVisible() { return isActive() && !isExpired() && (getRemainingAmount() > 0 || getAvailableToCollect() > 0); }

    public String getFormattedExpireTime() {
        if (isExpired()) return "Expired";
        long ms = expireDate.getTime() - System.currentTimeMillis();
        long d = ms / 86400000L, h = ms % 86400000L / 3600000L, m = ms % 3600000L / 60000L;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0 || d > 0) sb.append(h).append("h ");
        sb.append(m).append("m");
        return sb.toString().trim();
    }

    public String getFormattedDeletionTime() {
        if (shouldBeDeleted()) return "deleted";
        long ms = deletionDate.getTime() - System.currentTimeMillis();
        long d = ms / 86400000L, h = ms % 86400000L / 3600000L, m = ms % 3600000L / 60000L;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0 || d > 0) sb.append(h).append("h ");
        sb.append(m).append("m");
        return sb.toString().trim();
    }

    public ItemStack toItemStack(OrderManager orderManager) { return toItemStack(orderManager, null); }

    public ItemStack toItemStack(OrderManager orderManager, Map<?, ?> sourceItemConfig) {
        ItemStack item = createBaseItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (sourceItemConfig != null) {
            Map<String, String> ph = getPlaceholders(orderManager);
            if (sourceItemConfig.containsKey("name") || sourceItemConfig.containsKey("displayname")) {
                String dn = sourceItemConfig.containsKey("name") ? sourceItemConfig.get("name").toString() : sourceItemConfig.get("displayname").toString();
                for (Map.Entry<String, String> e : ph.entrySet()) dn = dn.replace(e.getKey(), e.getValue());
                meta.setDisplayName(ColorUtil.color(dn));
            }
            if (sourceItemConfig.containsKey("lore") && sourceItemConfig.get("lore") instanceof List) {
                List<?> loreList = (List<?>) sourceItemConfig.get("lore");
                List<String> lore = new ArrayList<>();
                String enchVal = ph.get("%enchantment-display%"), potionVal = ph.get("%potion-display%");
                for (Object lineObj : loreList) {
                    String line = lineObj.toString();
                    boolean isEnch = line.contains("%enchantment-display%"), isPotion = line.contains("%potion-display%");
                    if (isEnch && (enchVal == null || enchVal.isEmpty())) continue;
                    if (isPotion && (potionVal == null || potionVal.isEmpty())) continue;
                    for (Map.Entry<String, String> e : ph.entrySet()) line = line.replace(e.getKey(), e.getValue());
                    lore.add(ColorUtil.color(line));
                }
                meta.setLore(lore);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBaseItem() {
        if (isSpecialItem() && itemType != null && subType != null) {
            String type = itemType.toUpperCase();
            if (type.equals("POTION")) return createPotionItem(subType, 1);
            if (type.equals("TIPPED_ARROW")) return createTippedArrowItem(subType, 1);
            if (type.equals("ENCHANTED_BOOK")) return createEnchantedBookItem(subType, 1);
        }
        return new ItemStack(material, 1);
    }

    private ItemStack createPotionItem(String sub, int amount) {
        ItemStack potion = new ItemStack(Material.POTION, amount);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) return potion;
        String clean = sub; int dur = 3600, amp = 0;
        if (clean.startsWith("LONG_")) { dur = 9600; clean = clean.substring(5); }
        else if (clean.startsWith("STRONG_")) { amp = 1; clean = clean.substring(7); }
        PotionEffectType t = getPotionEffectType(clean);
        if (t != null) meta.addCustomEffect(new PotionEffect(t, dur, amp, true, true, true), true);
        potion.setItemMeta(meta);
        return potion;
    }

    private ItemStack createTippedArrowItem(String sub, int amount) {
        ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta meta = (PotionMeta) arrow.getItemMeta();
        if (meta == null) return arrow;
        String clean = sub; int dur = 3600, amp = 0;
        if (clean.startsWith("LONG_")) { dur = 9600; clean = clean.substring(5); }
        else if (clean.startsWith("STRONG_")) { amp = 1; clean = clean.substring(7); }
        if (clean.startsWith("SPLASH_")) clean = clean.substring(7);
        else if (clean.startsWith("LINGERING_")) clean = clean.substring(10);
        PotionEffectType t = getPotionEffectType(clean);
        if (t != null) {
            meta.addCustomEffect(new PotionEffect(t, dur, amp, true, true, true), true);
            Color color = getPotionColor(t);
            if (color != null) meta.setColor(color);
        }
        arrow.setItemMeta(meta);
        return arrow;
    }

    private ItemStack createEnchantedBookItem(String sub, int amount) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, amount);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta == null) return book;
        String[] parts = sub.split("_"); int level = 1; String enchantName;
        if (parts.length >= 2) {
            try {
                level = Integer.parseInt(parts[parts.length - 1]);
                StringBuilder nb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) { if (i > 0) nb.append("_"); nb.append(parts[i]); }
                enchantName = nb.toString();
            } catch (NumberFormatException e) { enchantName = sub; }
        } else enchantName = sub;
        for (Enchantment ench : Enchantment.values()) {
            if (ench.getKey().getKey().equalsIgnoreCase(enchantName)) { meta.addStoredEnchant(ench, level, true); break; }
        }
        book.setItemMeta(meta);
        return book;
    }

    private Color getPotionColor(PotionEffectType t) {
        if (t == null) return null;
        String n = t.getName().toLowerCase();
        if (n.contains("fire_resistance")) return Color.fromRGB(255, 69, 0);
        if (n.contains("strength")) return Color.fromRGB(255, 0, 0);
        if (n.contains("speed")) return Color.fromRGB(0, 191, 255);
        if (n.contains("slowness")) return Color.fromRGB(128, 128, 128);
        if (n.contains("poison")) return Color.fromRGB(50, 205, 50);
        if (n.contains("regeneration")) return Color.fromRGB(255, 105, 180);
        if (n.contains("weakness")) return Color.fromRGB(169, 169, 169);
        if (n.contains("night_vision")) return Color.fromRGB(0, 0, 128);
        if (n.contains("invisibility")) return Color.fromRGB(211, 211, 211);
        if (n.contains("water_breathing")) return Color.fromRGB(30, 144, 255);
        if (n.contains("healing") || n.contains("instant_health")) return Color.fromRGB(255, 20, 147);
        if (n.contains("harming") || n.contains("instant_damage")) return Color.fromRGB(139, 0, 0);
        if (n.contains("jump")) return Color.fromRGB(124, 252, 0);
        if (n.contains("luck")) return Color.fromRGB(81, 195, 161);
        if (n.contains("slow_falling")) return Color.fromRGB(240, 248, 255);
        return Color.fromRGB(162, 0, 255);
    }

    public PotionEffectType getPotionEffectType(String name) {
        switch (name.toUpperCase()) {
            case "FIRE_RESISTANCE": return PotionEffectType.FIRE_RESISTANCE;
            case "STRENGTH": return PotionEffectType.STRENGTH;
            case "SPEED": case "SWIFTNESS": return PotionEffectType.SPEED;
            case "SLOWNESS": return PotionEffectType.SLOWNESS;
            case "POISON": return PotionEffectType.POISON;
            case "REGENERATION": return PotionEffectType.REGENERATION;
            case "WEAKNESS": return PotionEffectType.WEAKNESS;
            case "NIGHT_VISION": return PotionEffectType.NIGHT_VISION;
            case "INVISIBILITY": return PotionEffectType.INVISIBILITY;
            case "WATER_BREATHING": return PotionEffectType.WATER_BREATHING;
            case "HEALING": case "INSTANT_HEALTH": return PotionEffectType.INSTANT_HEALTH;
            case "HARMING": return PotionEffectType.INSTANT_DAMAGE;
            case "JUMP": case "LEAPING": return PotionEffectType.JUMP_BOOST;
            case "LUCK": return PotionEffectType.LUCK;
            case "SLOW_FALLING": return PotionEffectType.SLOW_FALLING;
            default: return PotionEffectType.getByName(name.toUpperCase());
        }
    }

    public Map<String, String> getPlaceholders(OrderManager orderManager) {
        Map<String, String> ph = new HashMap<>();
        String ownerName = creator != null ? Bukkit.getOfflinePlayer(creator).getName() : "Unknown";
        ph.put("%owner%", ownerName != null ? ownerName : "Unknown");
        ph.put("%requested-material%", formatMaterialName(material.name()));
        ph.put("%price%", orderManager.formatCurrency(pricePerItem));
        ph.put("%total-paid-amount%", orderManager.formatCurrency(getTotalPrice()));
        ph.put("%requested-amount%", String.valueOf(requestedAmount));
        ph.put("%required-amount%", String.valueOf(requestedAmount));
        ph.put("%delivered-amount%", String.valueOf(deliveredAmount));
        ph.put("%collected-amount%", String.valueOf(collectedAmount));
        ph.put("%available-to-collect%", String.valueOf(getAvailableToCollect()));
        ph.put("%paid-amount%", orderManager.formatCurrency(pricePerItem * deliveredAmount));
        ph.put("%expire-time%", getFormattedExpireTime());
        String enchDisplay = "", potionDisplay = "", itemDetails = "";
        if (isSpecialItem() && itemType != null && subType != null) {
            String type = itemType.toUpperCase();
            if (type.equals("ENCHANTED_BOOK")) {
                String[] parts = subType.split("_");
                if (parts.length >= 2) {
                    StringBuilder nb = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (i > 0) nb.append(" ");
                        String p = parts[i];
                        nb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
                    }
                    enchDisplay = nb + " " + parts[parts.length - 1];
                    itemDetails = enchDisplay;
                    ph.put("%enchantment%", parts[0]);
                    ph.put("%enchantment-level%", parts[parts.length - 1]);
                } else { enchDisplay = subType; itemDetails = subType; }
                ph.put("%enchantment-display%", enchDisplay);
                ph.put("%potion-display%", "");
            } else if (type.equals("POTION") || type.equals("TIPPED_ARROW")) {
                potionDisplay = getPotionDisplayName(subType);
                itemDetails = potionDisplay;
                ph.put("%potion-display%", potionDisplay);
                ph.put("%potion-color%", getPotionColorCode(subType));
                ph.put("%potion-effect%", getPotionEffectName(subType));
                ph.put("%enchantment-display%", "");
            } else {
                ph.put("%variant%", subType);
                ph.put("%enchantment-display%", "");
                ph.put("%potion-display%", "");
            }
        } else {
            ph.put("%enchantment-display%", ""); ph.put("%potion-display%", "");
            ph.put("%potion-color%", ""); ph.put("%potion-effect%", "");
            ph.put("%variant%", ""); ph.put("%enchantment%", ""); ph.put("%enchantment-level%", "");
        }
        ph.put("%item-details%", itemDetails);
        return ph;
    }

    private String getPotionDisplayName(String sub) {
        StringBuilder result = new StringBuilder();
        for (String part : sub.split("_")) {
            switch (part) {
                case "LONG": result.append("Long "); break;
                case "STRONG": result.append("Strong "); break;
                case "SPLASH": result.append("Splash "); break;
                case "LINGERING": result.append("Lingering "); break;
                default: result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
            }
        }
        return result.toString().trim();
    }

    private String getPotionColorCode(String sub) {
        String u = sub.toUpperCase();
        if (u.contains("FIRE_RESISTANCE")) return "&#FF4500";
        if (u.contains("STRENGTH")) return "&#FF0000";
        if (u.contains("SPEED") || u.contains("SWIFTNESS")) return "&#00BFFF";
        if (u.contains("SLOWNESS")) return "&#808080";
        if (u.contains("POISON")) return "&#32CD32";
        if (u.contains("REGENERATION")) return "&#FF69B4";
        if (u.contains("WEAKNESS")) return "&#A9A9A9";
        if (u.contains("NIGHT_VISION")) return "&#000080";
        if (u.contains("INVISIBILITY")) return "&#D3D3D3";
        if (u.contains("WATER_BREATHING")) return "&#1E90FF";
        if (u.contains("HEALING") || u.contains("INSTANT_HEALTH")) return "&#FF1493";
        if (u.contains("HARMING")) return "&#8B0000";
        if (u.contains("JUMP") || u.contains("LEAPING")) return "&#7CFC00";
        if (u.contains("LUCK")) return "&#51C3A1";
        if (u.contains("SLOW_FALLING")) return "&#F0F8FF";
        return "&#00fc88";
    }

    private String getPotionEffectName(String sub) {
        List<String> parts = new ArrayList<>();
        for (String p : sub.split("_"))
            if (!p.equals("LONG") && !p.equals("STRONG") && !p.equals("SPLASH") && !p.equals("LINGERING"))
                parts.add(Character.toUpperCase(p.charAt(0)) + p.substring(1).toLowerCase());
        return String.join(" ", parts);
    }

    public static String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }
}

package me.vennlmao.ariscore.amethyst.listeners;

import me.vennlmao.ariscore.amethyst.AmethystModule;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AmethystToolListener implements Listener {

    private static final Random RANDOM = new Random();
    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART);

    private final AmethystModule module;

    public AmethystToolListener(AmethystModule module) {
        this.module = module;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String toolType = module.getItemManager().getToolType(item);
        if (toolType == null) return;

        if (module.getItemManager().isExpired(item)) {
            player.getInventory().setItemInMainHand(null);
            return;
        }

        switch (toolType) {
            case "pickaxe", "shovel" -> breakRadius(event, player, item, toolType);
            case "axe", "treechopper" -> breakTree(event, player, item, toolType);
            case "magichoe" -> harvestCrops(event, player, item, "magichoe");
            case "multitool" -> handleMultitool(event, player, item);
            default -> {}
        }
    }

    private void handleMultitool(BlockBreakEvent event, Player player, ItemStack item) {
        Material type = event.getBlock().getType();

        if (isLog(type)) {
            breakTree(event, player, item, "multitool");
        } else if (CROPS.contains(type)) {
            harvestCrops(event, player, item, "multitool");
        } else if (isDirtLike(type)) {
            breakRadius(event, player, item, "multitool");
        } else {
            breakRadius(event, player, item, "multitool");
        }
    }

    private boolean isDirtLike(Material material) {
        return material == Material.DIRT || material == Material.GRASS_BLOCK
                || material == Material.SAND || material == Material.GRAVEL
                || material == Material.SOUL_SAND || material == Material.SOUL_SOIL
                || material == Material.CLAY || material == Material.MUD;
    }

    private void breakRadius(BlockBreakEvent event, Player player, ItemStack item, String toolType) {
        ConfigurationSection section = module.getConfig().getConfigurationSection("tools." + toolType);
        int radius = section != null ? section.getInt("radius", 1) : 1;

        Block center = event.getBlock();
        BlockFace face = getTargetFace(player);

        List<Block> toBreak = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                Block relative = offsetBlock(center, face, x, y);
                if (relative.equals(center)) continue;
                if (relative.getType() == Material.AIR) continue;
                toBreak.add(relative);
            }
        }

        for (Block block : toBreak) {
            block.breakNaturally(item);
        }

        spawnParticles(player, center.getLocation(), "tools." + toolType + ".particle");
    }

    private BlockFace getTargetFace(Player player) {
        return player.getFacing().getOppositeFace();
    }

    private Block offsetBlock(Block center, BlockFace face, int a, int b) {
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            return center.getRelative(a, 0, b);
        } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
            return center.getRelative(a, b, 0);
        } else {
            return center.getRelative(0, b, a);
        }
    }

    private void breakTree(BlockBreakEvent event, Player player, ItemStack item, String toolType) {
        Block origin = event.getBlock();
        if (!isLog(origin.getType())) return;

        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        List<Block> toBreak = new ArrayList<>();

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            toBreak.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block neighbor = current.getRelative(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;
                        if (isLog(neighbor.getType()) || isLeaves(neighbor.getType())) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        for (Block block : toBreak) {
            if (!block.equals(origin)) block.breakNaturally(item);
        }

        spawnParticles(player, origin.getLocation(), "tools." + toolType + ".particle");
    }

    private void harvestCrops(BlockBreakEvent event, Player player, ItemStack item, String toolType) {
        Block origin = event.getBlock();
        if (!CROPS.contains(origin.getType())) return;

        event.setCancelled(true);

        ConfigurationSection section = module.getConfig().getConfigurationSection("tools." + toolType);
        int radius = section != null ? section.getInt("radius", 1) : 1;

        List<Block> toHarvest = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block relative = origin.getRelative(x, 0, z);
                if (!CROPS.contains(relative.getType())) continue;
                if (!(relative.getBlockData() instanceof Ageable ageable)) continue;
                if (ageable.getAge() != ageable.getMaximumAge()) continue;
                toHarvest.add(relative);
            }
        }

        for (Block block : toHarvest) {
            Material cropType = block.getType();
            int maximumAge = ((Ageable) block.getBlockData()).getMaximumAge();

            block.breakNaturally(item);

            Ageable replanted = (Ageable) Bukkit.createBlockData(cropType);
            replanted.setAge(maximumAge);
            block.setType(cropType);
            block.setBlockData(replanted);
        }

        spawnParticles(player, origin.getLocation(), "tools." + toolType + ".particle");
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG") || material.name().endsWith("_WOOD");
    }

    private boolean isLeaves(Material material) {
        return material.name().endsWith("_LEAVES");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        String toolType = module.getItemManager().getToolType(item);
        if (toolType == null) return;

        if (module.getItemManager().isExpired(item)) {
            player.getInventory().setItem(event.getHand(), null);
            return;
        }

        if (toolType.equals("bucket")) {
            event.setCancelled(true);
            drainWater(player, item);
        } else if (toolType.equals("firework")) {
            event.setCancelled(true);
            launchFirework(player);
        } else if (toolType.equals("fun")) {
            event.setCancelled(true);
            launchFunBurst(player);
        } else if (toolType.equals("booster")) {
            event.setCancelled(true);
            module.getItemManager().activateShardBoost(player);
            player.getInventory().setItem(event.getHand(), null);
        }
    }

    private void drainWater(Player player, ItemStack item) {
        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.WATER) return;

        ConfigurationSection section = module.getConfig().getConfigurationSection("tools.bucket");
        int radius = section != null ? section.getInt("radius", 1) : 1;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block relative = target.getRelative(x, y, z);
                    if (relative.getType() == Material.WATER) {
                        relative.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void launchFirework(Player player) {
        ConfigurationSection section = module.getConfig().getConfigurationSection("firework");
        if (section == null) return;

        spawnFirework(player.getLocation(), section);
    }

    private void launchFunBurst(Player player) {
        ConfigurationSection section = module.getConfig().getConfigurationSection("fun");
        if (section == null) return;

        int burstCount = section.getInt("burst-count", 5);
        double burstRadius = section.getDouble("burst-radius", 3);

        for (int i = 0; i < burstCount; i++) {
            double offsetX = (RANDOM.nextDouble() * 2 - 1) * burstRadius;
            double offsetZ = (RANDOM.nextDouble() * 2 - 1) * burstRadius;
            Location location = player.getLocation().clone().add(new Vector(offsetX, 0, offsetZ));
            spawnFirework(location, section);
        }
    }

    private void spawnFirework(Location location, ConfigurationSection section) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Builder builder = FireworkEffect.builder();
        List<String> allTypes = List.of("BALL", "BALL_LARGE", "STAR", "CREEPER", "BURST");
        String typeName = section.getString("effect.type", allTypes.get(RANDOM.nextInt(allTypes.size())));
        try {
            builder.with(FireworkEffect.Type.valueOf(typeName));
        } catch (IllegalArgumentException ignored) {
            builder.with(FireworkEffect.Type.BALL_LARGE);
        }

        List<String> colorNames = section.getStringList("effect.colors");
        List<Color> colors = new ArrayList<>();
        for (String colorName : colorNames) {
            try {
                colors.add((Color) Color.class.getField(colorName.toUpperCase()).get(null));
            } catch (Exception ignored) {}
        }
        if (colors.isEmpty()) {
            colors.add(Color.fromRGB(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256)));
        }
        builder.withColor(colors);

        builder.flicker(section.getBoolean("effect.flicker", true));
        builder.trail(section.getBoolean("effect.trail", true));

        meta.addEffect(builder.build());
        meta.setPower(section.getInt("power", 1));
        firework.setFireworkMeta(meta);
    }

    private void spawnParticles(Player player, Location location, String configPath) {
        ConfigurationSection section = module.getConfig().getConfigurationSection(configPath);
        if (section == null) return;

        String particleName = section.getString("type", "WITCH");
        int count = section.getInt("count", 20);

        try {
            Particle particle = Particle.valueOf(particleName);
            player.getWorld().spawnParticle(particle, location.clone().add(0.5, 0.5, 0.5), count);
        } catch (IllegalArgumentException ignored) {}
    }
}

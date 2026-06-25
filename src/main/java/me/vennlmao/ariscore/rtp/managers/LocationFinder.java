package me.vennlmao.ariscore.rtp.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class LocationFinder {

    private static final Random RANDOM = new Random();
    private static final int MAX_ATTEMPTS = 20;

    public static CompletableFuture<Location> findSafe(World world, ConfigurationSection sec) {
        int minX = sec.getInt("min_x", -5000);
        int maxX = sec.getInt("max_x", 5000);
        int minZ = sec.getInt("min_z", -5000);
        int maxZ = sec.getInt("max_z", 5000);
        int minY = sec.getInt("min_y", 60);
        int maxY = sec.getInt("max_y", 250);

        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                int x = minX + RANDOM.nextInt(maxX - minX + 1);
                int z = minZ + RANDOM.nextInt(maxZ - minZ + 1);

                int highestY = world.getHighestBlockYAt(x, z);
                if (highestY < minY || highestY > maxY) continue;

                Location loc = new Location(world, x + 0.5, highestY + 1, z + 0.5);
                Material below = world.getBlockAt(x, highestY, z).getType();

                if (isSafe(below)) return loc;
            }
            return null;
        });
    }

    private static boolean isSafe(Material mat) {
        return mat.isSolid()
                && mat != Material.LAVA
                && mat != Material.CACTUS
                && mat != Material.CAMPFIRE
                && mat != Material.SOUL_CAMPFIRE
                && mat != Material.MAGMA_BLOCK
                && mat != Material.WITHER_ROSE
                && mat != Material.SWEET_BERRY_BUSH;
    }
}

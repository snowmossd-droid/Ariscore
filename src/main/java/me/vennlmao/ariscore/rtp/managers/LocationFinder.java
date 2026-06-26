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

        return tryAttempt(world, minX, maxX, minZ, maxZ, minY, maxY, 0);
    }

    private static CompletableFuture<Location> tryAttempt(
            World world, int minX, int maxX, int minZ, int maxZ,
            int minY, int maxY, int attempt) {

        if (attempt >= MAX_ATTEMPTS) {
            return CompletableFuture.completedFuture(null);
        }

        int x = minX + RANDOM.nextInt(maxX - minX + 1);
        int z = minZ + RANDOM.nextInt(maxZ - minZ + 1);

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        return world.getChunkAtAsync(chunkX, chunkZ).thenCompose(chunk -> {
            int highestY = world.getHighestBlockYAt(x, z);

            if (highestY < minY || highestY > maxY) {
                return tryAttempt(world, minX, maxX, minZ, maxZ, minY, maxY, attempt + 1);
            }

            Material below = world.getBlockAt(x, highestY, z).getType();
            if (!isSafe(below)) {
                return tryAttempt(world, minX, maxX, minZ, maxZ, minY, maxY, attempt + 1);
            }

            return CompletableFuture.completedFuture(
                    new Location(world, x + 0.5, highestY + 1, z + 0.5));
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

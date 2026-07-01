package me.vennlmao.ariscore.rtp.managers;

import me.vennlmao.ariscore.rtp.RtpModule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final RtpModule plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CooldownManager(RtpModule plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(UUID id) {
        if (!cooldowns.containsKey(id)) return false;
        long elapsed = (System.currentTimeMillis() - cooldowns.get(id)) / 1000;
        return elapsed < plugin.getConfig().getLong("cooldown", 300);
    }

    public long getRemainingSeconds(UUID id) {
        if (!cooldowns.containsKey(id)) return 0;
        long elapsed = (System.currentTimeMillis() - cooldowns.get(id)) / 1000;
        long cooldown = plugin.getConfig().getLong("cooldown", 300);
        return Math.max(0, cooldown - elapsed);
    }

    public void setCooldown(UUID id) {
        cooldowns.put(id, System.currentTimeMillis());
    }

    public void clear() {
        cooldowns.clear();
    }
}

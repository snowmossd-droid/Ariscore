package me.vennlmao.ariscore.crates.managers;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.CrateModel;
import me.vennlmao.ariscore.crates.models.GamerModel;
import me.vennlmao.ariscore.crates.models.KeyAllConfig;
import me.vennlmao.ariscore.crates.utils.ColorUtil;
import me.vennlmao.ariscore.crates.utils.FoliaUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class KeyAllManager {

    private final CratesModule module;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;
    private volatile long nextRunMillis;
    private volatile long intervalMillis;

    public KeyAllManager(CratesModule module) {
        this.module = module;
    }

    public void start() {
        KeyAllConfig cfg = buildConfig();
        if (cfg == null) return;

        intervalMillis = cfg.getIntervalSeconds() * 1000L;
        nextRunMillis = System.currentTimeMillis() + intervalMillis;

        scheduler = new ScheduledThreadPoolExecutor(1);
        task = scheduler.scheduleAtFixedRate(
                () -> {
                    nextRunMillis = System.currentTimeMillis() + intervalMillis;
                    runKeyAll(cfg);
                },
                cfg.getIntervalSeconds(),
                cfg.getIntervalSeconds(),
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (task != null) task.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();
    }

    public long getSecondsUntilNextRun() {
        if (nextRunMillis == 0) return 0;
        long remaining = (nextRunMillis - System.currentTimeMillis()) / 1000L;
        return Math.max(remaining, 0);
    }

    private void runKeyAll(KeyAllConfig cfg) {
        for (GamerModel gamer : module.getGamerDataManager().values()) {
            for (Map.Entry<String, Integer> entry : cfg.getKeyRewards().entrySet()) {
                String crateName = entry.getKey();
                CrateModel crate = module.getCrateRegistry().find(crateName);
                if (crate == null) {
                    module.getPlugin().getLogger().warning("[Crates] KeyAll: unknown crate '" + crateName + "'");
                    continue;
                }
                gamer.addKeyAmount(crateName, entry.getValue());
            }

            Player online = module.getPlugin().getServer().getPlayer(gamer.getUniqueId());
            if (online == null || !online.isOnline()) continue;

            module.getMessageUtil().playSound(online, "keyall");

            String title = cfg.getTitle();
            String subtitle = cfg.getSubtitle();
            if (title.isEmpty() && subtitle.isEmpty()) continue;

            FoliaUtil.runForEntity(module.getPlugin(), online, () ->
                    online.sendTitle(
                            ColorUtil.translate(title),
                            ColorUtil.translate(subtitle),
                            10, 40, 10
                    )
            );
        }
    }

    private KeyAllConfig buildConfig() {
        ConfigurationSection section = module.getConfig().getConfigurationSection("automatic-key-all");
        if (section == null) return null;

        int interval = section.getInt("time", 300);

        Map<String, Integer> keyRewards = new HashMap<>();
        ConfigurationSection keysSection = section.getConfigurationSection("keys");
        if (keysSection != null) {
            for (String key : keysSection.getKeys(false)) {
                keyRewards.put(key, keysSection.getInt(key, 1));
            }
        }

        ConfigurationSection msgSection = section.getConfigurationSection("message");
        String title    = msgSection != null ? msgSection.getString("title", "")    : "";
        String subtitle = msgSection != null ? msgSection.getString("subtitle", "") : "";

        return new KeyAllConfig(interval, keyRewards, title, subtitle);
    }
}

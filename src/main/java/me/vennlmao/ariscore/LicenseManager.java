package me.vennlmao.ariscore;

import dev.respark.licensegate.LicenseGate;
import org.bukkit.plugin.java.JavaPlugin;

public class LicenseManager {

    private final JavaPlugin plugin;

    public LicenseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean validate() {
        String key = plugin.getConfig().getString("license-key", "");

        if (key.isEmpty()) {
            plugin.getLogger().severe("                                          ");
            plugin.getLogger().severe("  ╔════════════════════════════════════╗  ");
            plugin.getLogger().severe("  ║         ✗  ARISCORE LICENSE       ║  ");
            plugin.getLogger().severe("  ╠════════════════════════════════════╣  ");
            plugin.getLogger().severe("  ║  License key not found!            ║  ");
            plugin.getLogger().severe("  ║  Please add to config.yml:         ║  ");
            plugin.getLogger().severe("  ║  license-key: \"YOUR-KEY-HERE\"      ║  ");
            plugin.getLogger().severe("  ╚════════════════════════════════════╝  ");
            plugin.getLogger().severe("                                          ");
            return false;
        }

        LicenseGate licenseGate = new LicenseGate("a2691");
        LicenseGate.ValidationType result = licenseGate.verify(key);

        if (result == LicenseGate.ValidationType.VALID) {
            plugin.getLogger().info("                                          ");
            plugin.getLogger().info("  ╔════════════════════════════════════╗  ");
            plugin.getLogger().info("  ║        ✔  ARISCORE LICENSE        ║  ");
            plugin.getLogger().info("  ╠════════════════════════════════════╣  ");
            plugin.getLogger().info("  ║  License validated successfully!   ║  ");
            plugin.getLogger().info("  ║  All modules are now active.       ║  ");
            plugin.getLogger().info("  ╚════════════════════════════════════╝  ");
            plugin.getLogger().info("                                          ");
            return true;
        } else if (result == LicenseGate.ValidationType.EXPIRED) {
            plugin.getLogger().severe("                                          ");
            plugin.getLogger().severe("  ╔════════════════════════════════════╗  ");
            plugin.getLogger().severe("  ║         ✗  ARISCORE LICENSE       ║  ");
            plugin.getLogger().severe("  ╠════════════════════════════════════╣  ");
            plugin.getLogger().severe("  ║  Your license has EXPIRED!         ║  ");
            plugin.getLogger().severe("  ║  Please renew your license to      ║  ");
            plugin.getLogger().severe("  ║  continue using ArisCore.          ║  ");
            plugin.getLogger().severe("  ╚════════════════════════════════════╝  ");
            plugin.getLogger().severe("                                          ");
            return false;
        } else {
            plugin.getLogger().severe("                                          ");
            plugin.getLogger().severe("  ╔════════════════════════════════════╗  ");
            plugin.getLogger().severe("  ║         ✗  ARISCORE LICENSE       ║  ");
            plugin.getLogger().severe("  ╠════════════════════════════════════╣  ");
            plugin.getLogger().severe("  ║  License is INVALID!               ║  ");
            plugin.getLogger().severe("  ║  Reason: " + padRight(result.name(), 26) + "║  ");
            plugin.getLogger().severe("  ║  All modules have been disabled.   ║  ");
            plugin.getLogger().severe("  ╚════════════════════════════════════╝  ");
            plugin.getLogger().severe("                                          ");
            return false;
        }
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}

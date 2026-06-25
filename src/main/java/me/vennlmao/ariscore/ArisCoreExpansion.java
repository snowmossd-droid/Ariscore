package me.vennlmao.ariscore;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ArisCoreExpansion extends PlaceholderExpansion {

    private final ArisCore plugin;

    public ArisCoreExpansion(ArisCore plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "ariscore"; }
    @Override public @NotNull String getAuthor()     { return "vennlmao"; }
    @Override public @NotNull String getVersion()    { return "1.0.0"; }
    @Override public boolean persist()               { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        switch (params) {
            case "shards": {
                try { return String.valueOf(plugin.getShardsModule().getShardsManager().getShards(player)); }
                catch (Exception e) { return "0"; }
            }

            case "team":
            case "team_name": {
                if (plugin.getTeamModule() == null) return "N/A";
                String name = plugin.getTeamModule().getTeamManager().getPlayerTeamName(player.getUniqueId());
                return name != null ? name : "N/A";
            }
            case "team_display": {
                if (plugin.getTeamModule() == null) return "";
                String name = plugin.getTeamModule().getTeamManager().getPlayerTeamName(player.getUniqueId());
                return name != null ? "&7[&b" + name + "&7] " : "";
            }
            case "has_team": {
                if (plugin.getTeamModule() == null) return "false";
                return plugin.getTeamModule().getTeamManager().hasTeam(player.getUniqueId()) ? "true" : "false";
            }

            case "keyall_time": {
                if (plugin.getCratesModule() == null) return "N/A";
                long seconds = plugin.getCratesModule().getKeyAllManager().getSecondsUntilNextRun();
                long minutes = seconds / 60;
                long secs = seconds % 60;
                return minutes + "m " + secs + "s";
            }

            default:
                if (params.startsWith("crates_key_")) {
                    if (plugin.getCratesModule() == null) return "0";
                    String crateName = params.substring("crates_key_".length());
                    var gamer = plugin.getCratesModule().getGamerDataManager().find(player.getUniqueId());
                    return gamer != null ? String.valueOf(gamer.getKeyAmount(crateName)) : "0";
                }
                return null;
        }
    }
                    }

package me.vennlmao.ariscore.team.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.vennlmao.ariscore.team.TeamModule;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamPlaceholder extends PlaceholderExpansion {

    private final TeamModule module;

    public TeamPlaceholder(TeamModule module) {
        this.module = module;
    }

    @Override
    public @NotNull String getIdentifier() { return "ariscore"; }

    @Override
    public @NotNull String getAuthor() { return "vennlmao"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        if (params.equals("team") || params.equals("team_name")) {
            String team = module.getTeamManager().getPlayerTeamName(player.getUniqueId());
            return team != null ? team : "N/A";
        }
        if (params.equals("has_team")) {
            return module.getTeamManager().hasTeam(player.getUniqueId()) ? "true" : "false";
        }
        return null;
    }
}

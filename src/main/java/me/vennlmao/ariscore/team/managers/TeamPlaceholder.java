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

    @Override public @NotNull String getIdentifier() { return "ariscore"; }
    @Override public @NotNull String getAuthor()     { return "vennlmao"; }
    @Override public @NotNull String getVersion()    { return "1.0.0"; }
    @Override public boolean persist()               { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String teamName = module.getTeamManager().getPlayerTeamName(player.getUniqueId());

        switch (params) {
            // %ariscore_team% — trả tên team hoặc "N/A" để condition scoreboard hoạt động
            case "team":
            case "team_name":
                return teamName != null ? teamName : "N/A";

            // %ariscore_team_display% — trả "[Team] " có màu hoặc rỗng cho prefix nametag
            case "team_display":
                return teamName != null ? "&7[&b" + teamName + "&7] " : "";

            // %ariscore_has_team% — true/false
            case "has_team":
                return module.getTeamManager().hasTeam(player.getUniqueId()) ? "true" : "false";

            default:
                return null;
        }
    }
}

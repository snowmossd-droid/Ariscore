package me.vennlmao.ariscore.team.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeamChatListener implements Listener {

    private final TeamModule module;
    private final Set<UUID> teamChatEnabled = new HashSet<>();

    public TeamChatListener(TeamModule module) {
        this.module = module;
    }

    public void toggleChat(Player player) {
        if (teamChatEnabled.contains(player.getUniqueId())) {
            teamChatEnabled.remove(player.getUniqueId());
        } else {
            teamChatEnabled.add(player.getUniqueId());
        }
    }

    public boolean isChatEnabled(UUID uuid) {
        return teamChatEnabled.contains(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!teamChatEnabled.contains(player.getUniqueId())) return;

        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            teamChatEnabled.remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String format = module.getConfig().getString("messages.chat_format")
                .replace("{player}", player.getName())
                .replace("{message}", message)
                .replace("{team}", team.getName());

        for (UUID uuid : team.getMembers().keySet()) {
            Player member = module.getPlugin().getServer().getPlayer(uuid);
            if (member != null) {
                member.sendMessage(ColorUtil.parse(format));
            }
        }
    }
}

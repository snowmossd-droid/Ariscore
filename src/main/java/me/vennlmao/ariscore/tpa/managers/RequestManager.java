package me.vennlmao.ariscore.tpa.managers;

import me.vennlmao.ariscore.tpa.TpaModule;
import org.bukkit.entity.Player;

import java.util.*;

public class RequestManager {

    private final TpaModule plugin;
    private final Map<UUID, TpaRequest> pendingRequests = new HashMap<>();
    private final Set<UUID> tpautoEnabled = new HashSet<>();
    private final Set<UUID> tpaDisabled = new HashSet<>();
    private final Set<UUID> tpahereDisabled = new HashSet<>();

    public RequestManager(TpaModule plugin) {
        this.plugin = plugin;
    }

    public void addRequest(TpaRequest request) {
        pendingRequests.put(request.getReceiver().getUniqueId(), request);
    }

    public TpaRequest getRequest(Player receiver) {
        return pendingRequests.get(receiver.getUniqueId());
    }

    public TpaRequest getRequestBySender(Player sender) {
        for (TpaRequest r : pendingRequests.values()) {
            if (r.getSender().getUniqueId().equals(sender.getUniqueId())) {
                return r;
            }
        }
        return null;
    }

    public void removeRequest(Player receiver) {
        pendingRequests.remove(receiver.getUniqueId());
    }

    public void removeRequestBySender(Player sender) {
        pendingRequests.values().removeIf(r -> r.getSender().getUniqueId().equals(sender.getUniqueId()));
    }

    public boolean hasIncomingRequest(Player receiver) {
        return pendingRequests.containsKey(receiver.getUniqueId());
    }

    public boolean hasOutgoingRequest(Player sender) {
        return getRequestBySender(sender) != null;
    }

    public void setTpauto(Player player, boolean enabled) {
        if (enabled) tpautoEnabled.add(player.getUniqueId());
        else tpautoEnabled.remove(player.getUniqueId());
    }

    public boolean isTpautoEnabled(Player player) {
        return tpautoEnabled.contains(player.getUniqueId());
    }

    public void setTpaDisabled(Player player, boolean disabled) {
        if (disabled) tpaDisabled.add(player.getUniqueId());
        else tpaDisabled.remove(player.getUniqueId());
    }

    public boolean isTpaDisabled(Player player) {
        return tpaDisabled.contains(player.getUniqueId());
    }

    public void setTpahereDisabled(Player player, boolean disabled) {
        if (disabled) tpahereDisabled.add(player.getUniqueId());
        else tpahereDisabled.remove(player.getUniqueId());
    }

    public boolean isTpahereDisabled(Player player) {
        return tpahereDisabled.contains(player.getUniqueId());
    }
}

package me.vennlmao.ariscore.duel.listeners;

import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.duel.managers.DuelArena;
import me.vennlmao.ariscore.duel.utils.ColorUtil;
import me.vennlmao.ariscore.duel.utils.MessageUtil;
import me.vennlmao.ariscore.duel.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DuelGuiListener implements Listener {

    private final DuelModule module;
    private final Map<UUID, PendingCreate> pendingCreate = new HashMap<>();

    public DuelGuiListener(DuelModule module) { this.module = module; }

    private static class PendingCreate {
        final UUID target;
        int arenaIndex = -1;
        PendingCreate(UUID target) { this.target = target; }
    }

    public void openQueueConfirm(Player player) {
        var session = module.getSessionManager().getSession(player.getUniqueId());
        if (session == null) return;
        UUID opponentId = session.getOpponent(player.getUniqueId());
        Player opponent = Bukkit.getPlayer(opponentId);
        if (opponent == null) return;
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(module.getGuiBuilder().buildQueueConfirm(player, opponent)), null);
    }

    public void openCreateDuel(Player viewer, OfflinePlayer target) {
        pendingCreate.put(viewer.getUniqueId(), new PendingCreate(target.getUniqueId()));
        openCreateDuelInventory(viewer, target, null);
    }

    private void openCreateDuelInventory(Player viewer, OfflinePlayer target, DuelArena arena) {
        viewer.getScheduler().run(module.getPlugin(), t ->
                viewer.openInventory(module.getGuiBuilder().buildCreateDuel(viewer, target, arena)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        String queueBase = ColorUtil.strip(module.getConfig().getString("gui.queue-confirm.title", ""));
        String createBase = ColorUtil.strip(module.getConfig().getString("gui.create-duel.title", ""))
                .replaceAll("\\{[^}]*}", "").trim();

        if (title.equals(queueBase)) {
            event.setCancelled(true);
            handleQueueConfirm(player, event.getSlot());
        } else if (!createBase.isEmpty() && title.contains(createBase)) {
            event.setCancelled(true);
            handleCreateDuel(player, event.getSlot());
        }
    }

    private void handleQueueConfirm(Player player, int slot) {
        int confirmSlot = module.getConfig().getInt("gui.queue-confirm.items.confirm.slot");
        int cancelSlot = module.getConfig().getInt("gui.queue-confirm.items.cancel.slot");
        if (slot == confirmSlot) {
            module.getSessionManager().confirmQueueMatch(player);
        } else if (slot == cancelSlot) {
            player.closeInventory();
            module.getSessionManager().cancelQueueMatch(player);
        }
    }

    private void handleCreateDuel(Player player, int slot) {
        PendingCreate pending = pendingCreate.get(player.getUniqueId());
        if (pending == null) return;

        int arenaSlot = module.getConfig().getInt("gui.create-duel.items.arena.slot");
        int confirmSlot = module.getConfig().getInt("gui.create-duel.items.confirm.slot");
        int cancelSlot = module.getConfig().getInt("gui.create-duel.items.cancel.slot");

        OfflinePlayer target = Bukkit.getOfflinePlayer(pending.target);

        if (slot == arenaSlot) {
            List<String> names = module.getArenaManager().getArenaNames();
            if (names.isEmpty()) { SoundUtil.play(player, "error"); return; }
            pending.arenaIndex = (pending.arenaIndex + 1) % (names.size() + 1);
            DuelArena arena = pending.arenaIndex == names.size() ? null : module.getArenaManager().getArena(names.get(pending.arenaIndex));
            SoundUtil.play(player, "click");
            openCreateDuelInventory(player, target, arena);
            return;
        }

        if (slot == confirmSlot) {
            Player targetPlayer = Bukkit.getPlayer(pending.target);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                MessageUtil.sendBoth(player, "player_not_found");
                SoundUtil.play(player, "error");
                return;
            }
            if (module.getSessionManager().isBusy(player.getUniqueId())) {
                MessageUtil.sendBoth(player, "already_busy");
                SoundUtil.play(player, "error");
                return;
            }
            if (module.getSessionManager().isBusy(targetPlayer.getUniqueId())) {
                MessageUtil.sendBoth(player, "target_busy");
                SoundUtil.play(player, "error");
                return;
            }
            List<String> names = module.getArenaManager().getArenaNames();
            DuelArena arena = (pending.arenaIndex >= 0 && pending.arenaIndex < names.size())
                    ? module.getArenaManager().getArena(names.get(pending.arenaIndex)) : null;
            player.closeInventory();
            pendingCreate.remove(player.getUniqueId());
            module.getSessionManager().sendCustomInvite(player, targetPlayer, arena);
            SoundUtil.play(player, "confirm");
            return;
        }

        if (slot == cancelSlot) {
            player.closeInventory();
            pendingCreate.remove(player.getUniqueId());
        }
    }

    public void clearPending(UUID uuid) {
        pendingCreate.remove(uuid);
    }
}

package me.vennlmao.ariscore.shards.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.vennlmao.ariscore.shards.ShardsModule;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShardsPlaceholder extends PlaceholderExpansion {

    private final ShardsModule module;

    public ShardsPlaceholder(ShardsModule module) {
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
        if (player == null) return "0";
        if (params.equals("shards")) {
            try {
                return String.valueOf(module.getShardsManager().getShards(player));
            } catch (Exception e) {
                return "0";
            }
        }
        return null;
    }
}

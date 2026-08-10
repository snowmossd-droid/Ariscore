package me.vennlmao.ariscore.crates;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.vennlmao.ariscore.crates.managers.GamerDataManager;
import me.vennlmao.ariscore.crates.models.GamerModel;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CratesExpansion extends PlaceholderExpansion {

    private final CratesModule module;

    public CratesExpansion(CratesModule module) {
        this.module = module;
    }

    @Override
    public @NotNull String getIdentifier() { return "ariscrates"; }

    @Override
    public @NotNull String getAuthor() { return "vennlmao"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "0";

        if (params.startsWith("key_")) {
            String crateName = params.substring(4);
            GamerDataManager dataManager = module.getGamerDataManager();
            GamerModel gamer = dataManager.find(player.getUniqueId());
            if (gamer == null) return "0";
            return String.valueOf(gamer.getKeyAmount(crateName));
        }

        return null;
    }
}

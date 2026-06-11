package me.vennlmao.ariscore.auction.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import me.vennlmao.ariscore.auction.AuctionModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SignEditorUtil {

    private static AuctionModule module;

    public static void init(AuctionModule m) { module = m; }

    public static void openSign(Player player, String configKey, Consumer<String[]> callback) {
        Location loc = player.getLocation().clone().add(0, 3, 0);
        loc.setX(loc.getBlockX()); loc.setY(loc.getBlockY()); loc.setZ(loc.getBlockZ());

        String matStr = module.getConfig().getString(configKey + ".material", "OAK_SIGN");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.OAK_SIGN;

        List<String> cfgLines = module.getConfig().getStringList(configKey + ".lines");
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) lines[i] = i < cfgLines.size() ? cfgLines.get(i) : "";

        com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState fakeState =
                com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState.getByString(
                        mat.name().toLowerCase().replace("_sign", "_wall_sign") + "[facing=south]");

        com.github.retrooper.packetevents.protocol.world.Location peLoc =
                new com.github.retrooper.packetevents.protocol.world.Location(loc.getX(), loc.getY(), loc.getZ());

        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerBlockChange(peLoc, fakeState.getGlobalId()));

        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerOpenSignEditor(peLoc, true));

        module.getPendingSign().put(player.getUniqueId(), callback);
    }
}

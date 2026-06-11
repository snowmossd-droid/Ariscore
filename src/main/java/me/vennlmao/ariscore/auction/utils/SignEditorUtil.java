package me.vennlmao.ariscore.auction.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import me.vennlmao.ariscore.auction.AuctionModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public class SignEditorUtil {

    private static AuctionModule module;

    public static void init(AuctionModule m) { module = m; }

    public static void openSign(Player player, String configKey, Consumer<String[]> callback) {
        Location loc = player.getLocation().clone().add(0, 3, 0);
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();

        String matStr = module.getConfig().getString(configKey + ".material", "OAK_SIGN");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.OAK_SIGN;

        String blockId = mat.name().toLowerCase() + "[facing=south]";
        WrappedBlockState fakeState;
        try {
            fakeState = WrappedBlockState.getByString(blockId);
        } catch (Exception e) {
            fakeState = WrappedBlockState.getByString("oak_sign[facing=south]");
        }

        Vector3i pos = new Vector3i(bx, by, bz);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerBlockChange(pos, fakeState));

        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerOpenSignEditor(pos, true));

        module.getPendingSign().put(player.getUniqueId(), callback);
    }
            }

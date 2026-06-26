package me.vennlmao.ariscore.auction.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ItemSerializer {

    public static String toBase64(ItemStack item) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream data = new BukkitObjectOutputStream(out);
            data.writeObject(item);
            data.close();
            return Base64Coder.encodeLines(out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
            BukkitObjectInputStream data = new BukkitObjectInputStream(in);
            ItemStack item = (ItemStack) data.readObject();
            data.close();
            return item;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

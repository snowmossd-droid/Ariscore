package me.vennlmao.ariscore.spawners.utils;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class GuiConfigUtil {

    public static List<String> getLore(FileConfiguration cfg, String path) {
        Object raw = cfg.get(path);
        List<String> lines = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) lines.add(String.valueOf(o));
        } else if (raw instanceof String s) {
            lines.add(s);
        }
        return lines;
    }

    public static String getName(FileConfiguration cfg, String path) {
        String v = cfg.getString(path + ".display-name", null);
        if (v == null) v = cfg.getString(path + ".displayname", " ");
        return v;
    }
}

package me.vennlmao.ariscore.tab.models;

import java.util.List;

public class TabProfile {
    private final String id;
    private final String displayCondition;
    private final List<String> header;
    private final List<String> footer;
    private final String tablistNameFormat;

    public TabProfile(String id, String displayCondition, List<String> header, List<String> footer, String tablistNameFormat) {
        this.id = id; this.displayCondition = displayCondition; this.header = header; this.footer = footer; this.tablistNameFormat = tablistNameFormat;
    }

    public String getId() { return id; }
    public String getDisplayCondition() { return displayCondition; }
    public List<String> getHeader() { return header; }
    public List<String> getFooter() { return footer; }
    public String getTablistNameFormat() { return tablistNameFormat; }
}

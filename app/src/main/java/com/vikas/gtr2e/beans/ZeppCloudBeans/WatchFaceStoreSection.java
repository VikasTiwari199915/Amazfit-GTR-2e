package com.vikas.gtr2e.beans.ZeppCloudBeans;

import java.util.Collections;
import java.util.List;

/**
 * One scroll row in the watch face store: a heading plus a horizontal list of {@link WatchfaceItem}.
 */
public final class WatchFaceStoreSection {
    public final String title;
    public final List<WatchfaceItem> items;

    public WatchFaceStoreSection(String title, List<WatchfaceItem> items) {
        this.title = title != null ? title : "";
        this.items = items != null ? items : Collections.emptyList();
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }
}

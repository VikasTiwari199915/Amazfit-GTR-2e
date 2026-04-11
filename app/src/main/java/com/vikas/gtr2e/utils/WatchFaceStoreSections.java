package com.vikas.gtr2e.utils;

import android.content.Context;

import com.vikas.gtr2e.R;
import com.vikas.gtr2e.beans.ZeppCloudBeans.Category;
import com.vikas.gtr2e.beans.ZeppCloudBeans.Ranking;
import com.vikas.gtr2e.beans.ZeppCloudBeans.StoreResponse;
import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchFaceStoreSection;
import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchfaceItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Flattens {@link StoreResponse} into ordered store sections: {@code new} → rankings → categories.
 */
public final class WatchFaceStoreSections {

    private WatchFaceStoreSections() {
    }

    public static List<WatchFaceStoreSection> fromResponse(Context context, StoreResponse body) {
        if (body == null) {
            return Collections.emptyList();
        }
        List<WatchFaceStoreSection> sections = new ArrayList<>();

        if (body.getNewItems() != null && !body.getNewItems().isEmpty()) {
            sections.add(new WatchFaceStoreSection(
                    context.getString(R.string.watch_face_store_section_new),
                    new ArrayList<>(body.getNewItems())));
        }

        if (body.getRanking() != null) {
            for (Ranking ranking : body.getRanking()) {
                if (ranking == null || ranking.getData() == null || ranking.getData().isEmpty()) {
                    continue;
                }
                String title = rankingTitle(context, ranking.getType());
                sections.add(new WatchFaceStoreSection(title, new ArrayList<>(ranking.getData())));
            }
        }

        List<Category> categories = filterDisplayableCategories(body.getCategories());
        for (Category category : categories) {
            List<WatchfaceItem> data = category.getData();
            if (data == null || data.isEmpty()) {
                continue;
            }
            String title = category.getCategory();
            if (title == null || title.trim().isEmpty()) {
                title = context.getString(R.string.watch_face_store_category_fallback, category.getCategory_id());
            }
            sections.add(new WatchFaceStoreSection(title, new ArrayList<>(data)));
        }

        return sections;
    }

    private static String rankingTitle(Context context, String type) {
        if (type == null || type.trim().isEmpty()) {
            return context.getString(R.string.watch_face_store_ranking_unknown);
        }
        switch (type) {
            case "total":
                return context.getString(R.string.watch_face_store_ranking_total);
            case "week":
                return context.getString(R.string.watch_face_store_ranking_week);
            default:
                return context.getString(R.string.watch_face_store_ranking_format, type);
        }
    }

    private static List<Category> filterDisplayableCategories(List<Category> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        List<Category> out = new ArrayList<>();
        for (Category c : raw) {
            if (c == null) {
                continue;
            }
            boolean hasTitle = c.getCategory() != null && !c.getCategory().trim().isEmpty();
            boolean hasData = c.getData() != null && !c.getData().isEmpty();
            if (hasTitle || hasData) {
                out.add(c);
            }
        }
        return out;
    }
}

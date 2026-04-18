package com.vikas.gtr2e.beans.ZeppCloudBeans;

import java.util.List;

import lombok.Data;

@Data
public class PageableWatchFaceStoreResponse {
    private int page;
    private int per_page;
    private int total_page;
    private List<WatchfaceItem> data;
}

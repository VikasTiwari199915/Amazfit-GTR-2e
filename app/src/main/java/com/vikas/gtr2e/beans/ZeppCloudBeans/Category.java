package com.vikas.gtr2e.beans.ZeppCloudBeans;

import java.util.ArrayList;

import lombok.Data;

@Data
public class Category {
    private int page;
    private int per_page;
    private int total_page;
    private int category_id;
    private String category;
    private ArrayList<WatchfaceItem> data;
}
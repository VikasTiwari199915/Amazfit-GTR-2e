package com.vikas.gtr2e.beans.ZeppCloudBeans;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import lombok.Data;

@Data
public class StoreResponse {
    private int total;
    @SerializedName("new")
    private ArrayList<WatchfaceItem> newItems;
    private int new_count;
    private ArrayList<Ranking> ranking;
    private ArrayList<Object> banner;
    private ArrayList<Category> categories;
    private ArrayList<Object> subjects;
    private ArrayList<Object> topics;
}

package com.vikas.gtr2e.beans.ZeppCloudBeans;

import java.util.ArrayList;

import lombok.Data;

@Data
public class Ranking {
    private String type;
    private ArrayList<WatchfaceItem> data;
}
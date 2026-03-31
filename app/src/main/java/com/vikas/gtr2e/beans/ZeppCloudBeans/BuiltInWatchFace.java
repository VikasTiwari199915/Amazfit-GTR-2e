package com.vikas.gtr2e.beans.ZeppCloudBeans;

import lombok.Data;

@Data
public class BuiltInWatchFace {
    private int id;
    private String name;
    private int builtin_id;
    private String image;
    private String device_image;
    private boolean official_builtin;
}

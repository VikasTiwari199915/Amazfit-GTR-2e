package com.vikas.gtr2e.beans.ZeppCloudBeans;

import lombok.Data;
import java.io.Serializable;

@Data
public class WatchfaceItem implements Serializable {
    private int id;
    private String name;
    private boolean is_free;
    private boolean in_app_purchase;
    private int price;
    private int rank;
    private boolean customizable;
    private String customizable_type;
    private String download_url;
    private int size;
    private String version;
    private String device_support_version;
    private String image;
    private String description;
    private WatchfaceMeta metas;
    private int updated_at;
}
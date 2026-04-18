package com.vikas.gtr2e.beans.ZeppCloudBeans;

import lombok.Data;
import java.io.Serializable;

@Data
public class WatchfaceMeta implements Serializable {
    private String transparent_background;
    private String device_image;
    private int builtin_id;
    private String minimum_firmware_version;
    private String gif;
}

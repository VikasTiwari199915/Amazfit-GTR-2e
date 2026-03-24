/*
 * Copyright (c) 2026. All rights reserved.
 * File : Item.java, Last Modified on : 3/23/26, 2:47 PM
 * Created by Vikas Tiwari.
 * Do not copy/modify without permission.
 * For any contact email at Vikastiwari199915@gmail.com
 */

package com.vikas.gtr2e.beans.zeppAuthBeans;

import lombok.Data;

@Data
public class ZeppDeviceItem {
    private long deviceType;
    private long deviceSource;
    private String deviceId;
    private String macAddress;
    private String sn;
    private long bindingStatus;
    private long applicationTime;
    private long lastStatusUpdateTime;
    private AdditionalInfo additionalInfo;
    private String lastBindingPlatform;
    private String firmwareVersion;
    private long lastActiveStatusUpdateTime;
    private long activeStatus;
    private String priority;
    private long sort;
}
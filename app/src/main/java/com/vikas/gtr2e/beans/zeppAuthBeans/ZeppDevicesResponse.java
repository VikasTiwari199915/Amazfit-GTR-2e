/*
 * Copyright (c) 2026. All rights reserved.
 * File : ZeppDevicesResponse.java, Last Modified on : 3/23/26, 2:48 PM
 * Created by Vikas Tiwari.
 * Do not copy/modify without permission.
 * For any contact email at Vikastiwari199915@gmail.com
 */

package com.vikas.gtr2e.beans.zeppAuthBeans;

import lombok.Data;

import java.util.List;

@Data
public class ZeppDevicesResponse {
    private List<ZeppDeviceItem> items;
}
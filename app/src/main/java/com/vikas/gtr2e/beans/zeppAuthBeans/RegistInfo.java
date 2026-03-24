/*
 * Copyright (c) 2026. All rights reserved.
 * File : RegistInfo.java, Last Modified on : 3/23/26, 1:57 PM
 * Created by Vikas Tiwari.
 * Do not copy/modify without permission.
 * For any contact email at Vikastiwari199915@gmail.com
 */

package com.vikas.gtr2e.beans.zeppAuthBeans;

import lombok.Data;

@Data
public class RegistInfo {
    private int is_new_user;
    private long regist_date;
    private String region;
    private String country_code;
}
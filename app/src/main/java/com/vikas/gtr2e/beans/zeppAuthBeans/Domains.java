/*
 * Copyright (c) 2026. All rights reserved.
 * File : Domains.java, Last Modified on : 3/23/26, 1:57 PM
 * Created by Vikas Tiwari.
 * Do not copy/modify without permission.
 * For any contact email at Vikastiwari199915@gmail.com
 */

package com.vikas.gtr2e.beans.zeppAuthBeans;

import lombok.Data;

import java.util.List;

/**
 * Retrofit model for Zepp API
 * @author Vikas Tiwari
 */
@Data
public class Domains {
    private List<String> cnames;
    private String host;
}
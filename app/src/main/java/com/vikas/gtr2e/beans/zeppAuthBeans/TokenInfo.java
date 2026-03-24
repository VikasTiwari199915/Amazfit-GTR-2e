/*
 * Copyright (c) 2026. All rights reserved.
 * File : TokenInfo.java, Last Modified on : 3/23/26, 1:58 PM
 * Created by Vikas Tiwari.
 * Do not copy/modify without permission.
 * For any contact email at Vikastiwari199915@gmail.com
 */

package com.vikas.gtr2e.beans.zeppAuthBeans;

import lombok.Data;

@Data
public class TokenInfo {
    private String login_token;
    private String app_token;
    private String user_id;
    private int ttl;
    private int app_ttl;
}
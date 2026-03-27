/*
 * Copyright (c) 2026. All rights reserved.
 * File : ZeppLoginResponse.java, Last Modified on : 3/23/26, 1:58 PM
 * Created by Vikas Tiwari.
 * Do not copy/modify without permission.
 * For any contact email at Vikastiwari199915@gmail.com
 */

package com.vikas.gtr2e.beans.zeppAuthBeans;

import lombok.Data;

import java.util.List;

@Data
public class ZeppLoginResponse {
    private TokenInfo token_info;
    private RegistInfo regist_info;
    private ThirdPartyInfo thirdparty_info;
    private String result;
    private Domain domain;
    private List<Domains> domains;
    private String error_code;
}
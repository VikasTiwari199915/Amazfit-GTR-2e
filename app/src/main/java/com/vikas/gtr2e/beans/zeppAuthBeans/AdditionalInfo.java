/*
 * Copyright (c) 2026. All rights reserved.
 * File : AdditionalInfo.java, Last Modified on : 3/23/26, 2:46 PM
 * Created by Vikas Tiwari.
 * Do not copy/modify without permission.
 * For any contact email at Vikastiwari199915@gmail.com
 */

package com.vikas.gtr2e.beans.zeppAuthBeans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Data
@NoArgsConstructor
public class AdditionalInfo {
    private String sn;
    private String priority;
    private long sort;
    @JsonProperty("bind_timezone")
    private long bindTimezone;
    @JsonProperty("auth_key")
    private String authKey;
    private String productId;
    private String productVersion;
    private String hardwareVersion;
    @JsonProperty("brand type")
    private long brandType;

    @JsonCreator
    public static AdditionalInfo fromJson(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        // Since the JSON response contains additionalInfo as a stringified JSON object,
        // this creator allows Jackson to deserialize that string into this object.
        return new ObjectMapper().readValue(json, AdditionalInfo.class);
    }
}

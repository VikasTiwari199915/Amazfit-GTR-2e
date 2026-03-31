package com.vikas.gtr2e.beans.ZeppCloudBeans;

import lombok.Data;

@Data
public class FamilyMember {
    private String uid;
    private int fuid;
    private String nickname;
    private String city;
    private String brithday;
    private int gender;
    private int height;
    private double weight;
    private double targetweight;
    private int last_modify;
    private int scale_avatar_id;
    private int measure_mode;
}

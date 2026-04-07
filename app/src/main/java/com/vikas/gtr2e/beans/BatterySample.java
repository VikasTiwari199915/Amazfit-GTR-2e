package com.vikas.gtr2e.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatterySample {
    long time;     // millis
    int battery;   // percentage
}
package com.vikas.gtr2e.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity(tableName = "battery_samples", indices = {@Index("timestamp")})
public class BatterySampleEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;
    public int batteryPercent;
    public boolean isCharging;

    public BatterySampleEntity(long timestamp, int batteryPercent, boolean isCharging) {
        this.timestamp = timestamp;
        this.batteryPercent = batteryPercent;
        this.isCharging = isCharging;
    }
}
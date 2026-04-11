package com.vikas.gtr2e.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.vikas.gtr2e.db.entities.BatterySampleEntity;

import java.util.List;

@Dao
public interface BatterySampleDao {

    @Insert
    void insert(BatterySampleEntity sample);

    @Query("SELECT * FROM BATTERY_SAMPLES ORDER BY timestamp ASC")
    List<BatterySampleEntity> getAll();

    @Query("SELECT * FROM BATTERY_SAMPLES WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    List<BatterySampleEntity> getBetween(long start, long end);

    @Query("SELECT * FROM BATTERY_SAMPLES ORDER BY timestamp DESC LIMIT 1")
    BatterySampleEntity getLast();

    @Query("DELETE FROM BATTERY_SAMPLES WHERE timestamp < :cutoff")
    void deleteOlderThan(long cutoff);
}
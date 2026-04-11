package com.vikas.gtr2e.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.vikas.gtr2e.db.entities.BatterySampleEntity;

@Database(entities = {BatterySampleEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract BatterySampleDao batterySampleDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "gtr_2e_db")
                    .fallbackToDestructiveMigration(true)
                    .build();
        }
        return INSTANCE;
    }
}
package com.example.pracaobecnosc;

import android.content.Context;

import androidx.room.Room;

public class DatabaseClient {
    private static DatabaseClient instance;
    private AppDatabase appDatabase;

    private DatabaseClient(Context context) {
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "praca_obecnosc_db")
                .allowMainThreadQueries() // Dla uproszczenia, w produkcji lepiej używać wątków
                .build();
    }

    public static synchronized DatabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseClient(context);
        }
        return instance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}
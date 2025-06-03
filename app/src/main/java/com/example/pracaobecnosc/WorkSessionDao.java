package com.example.pracaobecnosc;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WorkSessionDao {
    @Insert
    long insertWorkSession(WorkSession workSession);

    @Update
    void updateWorkSession(WorkSession workSession);

    @Query("SELECT * FROM work_sessions WHERE userId = :userId ORDER BY startTime DESC")
    List<WorkSession> getSessionsForUser(int userId);

    @Query("SELECT * FROM work_sessions WHERE userId = :userId AND endTime IS NULL")
    WorkSession getActiveSessionForUser(int userId);
}
package com.example.pracaobecnosc;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "work_sessions",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE))
public class WorkSession {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private int userId;
    private Date startTime;
    private Date endTime;
    private long duration; // w milisekundach

    public WorkSession(){};
    public WorkSession(int userId, Date startTime) {
        this.userId = userId;
        this.startTime = startTime;
    }

    // Gettery i settery
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.duration = endTime.getTime() - startTime.getTime();
        }
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
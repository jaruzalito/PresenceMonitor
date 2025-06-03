package com.example.pracaobecnosc;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long insertUser(User user);

    @Update
    void updateUser(User user);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    User login(String username, String password);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("SELECT * FROM users WHERE status = 'working'")
    List<User> getWorkingUsers();

    @Query("SELECT * FROM users WHERE status = 'offline'")
    List<User> getOfflineUsers();

    @Query("SELECT * FROM users WHERE status = :status")
    List<User> getUsersByStatus(String status);
}
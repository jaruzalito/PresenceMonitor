package com.example.pracaobecnosc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText editUsername, editPassword;
    private Button btnLoginSubmit;
    private DatabaseClient databaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate: Inicjalizacja LoginActivity");

        // Inicjalizacja bazy danych
        databaseClient = DatabaseClient.getInstance(getApplicationContext());
        Log.d(TAG, "onCreate: DatabaseClient zainicjalizowany");

        // Znajdź komponenty UI
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);

        Log.d(TAG, "onCreate: Komponenty UI znalezione");

        // Obsługa przycisku logowania
        btnLoginSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Kliknięto przycisk logowania");

                String username = editUsername.getText().toString().trim();
                String password = editPassword.getText().toString().trim();

                Log.d(TAG, "onClick: Próba logowania dla użytkownika: " + username);

                // Walidacja pól
                if (username.isEmpty() || password.isEmpty()) {
                    Log.w(TAG, "onClick: Brak username lub password");
                    Toast.makeText(LoginActivity.this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Wykonaj operację bazodanową w osobnym wątku
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Próba logowania
                            final User user = databaseClient.getAppDatabase().userDao().login(username, password);

                            // Wykonaj zadania w wątku UI
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (user != null) {
                                        Log.d(TAG, "Logowanie udane: " + username);
                                        Log.d(TAG, "Typ użytkownika: " + user.getType());
                                        Log.d(TAG, "ID użytkownika: " + user.getId());

                                        // Zapisz dane zalogowanego użytkownika w SharedPreferences
                                        SharedPreferences preferences = getSharedPreferences("pracaObecnoscPrefs", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putInt("userId", user.getId());
                                        editor.putString("username", user.getUsername());
                                        editor.putString("userType", user.getType());
                                        editor.apply();

                                        Log.d(TAG, "Zapisano dane użytkownika w SharedPreferences");

                                        // Przekieruj do odpowiedniej aktywności w zależności od typu użytkownika
                                        if ("admin".equals(user.getType())) {
                                            Log.d(TAG, "Przekierowanie do AdminActivity");
                                            Toast.makeText(LoginActivity.this, "Zalogowano jako admin", Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                                            intent.putExtra("username", user.getUsername());
                                            startActivity(intent);
                                        } else {
                                            Log.d(TAG, "Przekierowanie do UserActivity");
                                            Toast.makeText(LoginActivity.this, "Zalogowano jako użytkownik", Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(LoginActivity.this, UserActivity.class);
                                            intent.putExtra("username", user.getUsername());
                                            startActivity(intent);
                                        }

                                        // Zakończ aktywność logowania
                                        finish();
                                    } else {
                                        Log.w(TAG, "Logowanie nieudane dla użytkownika: " + username);
                                        Toast.makeText(LoginActivity.this, "Nieprawidłowa nazwa użytkownika lub hasło", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Błąd podczas logowania: " + e.getMessage());
                            e.printStackTrace();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }
}
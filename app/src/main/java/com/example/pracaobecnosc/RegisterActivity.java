package com.example.pracaobecnosc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

public class RegisterActivity extends AppCompatActivity {

    private EditText editRegUsername, editRegPassword, editAuthCode;
    private Button btnAuthCode, btnRegisterSubmit;
    private DatabaseClient databaseClient;
    private boolean isAuthCodeVisible = false;
    private final String ADMIN_AUTH_CODE = "123456789";
    private TextView errorbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicjalizacja bazy danych
        databaseClient = DatabaseClient.getInstance(getApplicationContext());

        // Znajdź komponenty UI
        editRegUsername = findViewById(R.id.editRegUsername);
        editRegPassword = findViewById(R.id.editRegPassword);
        editAuthCode = findViewById(R.id.editAuthCode);
        btnAuthCode = findViewById(R.id.btnAuthCode);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        errorbox = findViewById(R.id.texterror);

        // Obsługa przycisku do pokazania pola z kodem uwierzytelniania
        btnAuthCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAuthCodeVisible) {
                    editAuthCode.setVisibility(View.VISIBLE);
                    isAuthCodeVisible = true;
                    // Ukryj błąd gdy otwieramy pole kodu
                    errorbox.setVisibility(View.GONE);
                } else {
                    editAuthCode.setVisibility(View.GONE);
                    isAuthCodeVisible = false;
                    editAuthCode.setText("");
                    // Ukryj błąd gdy zamykamy pole kodu
                    errorbox.setVisibility(View.GONE);
                }
            }
        });

        // Obsługa przycisku rejestracji
        btnRegisterSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editRegUsername.getText().toString().trim();
                String password = editRegPassword.getText().toString().trim();
                String authCode = editAuthCode.getText().toString().trim();

                // Walidacja pól
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Wypełnij wszystkie wymagane pola", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Sprawdź, czy pole kodu jest widoczne i czy kod jest poprawny
                if (isAuthCodeVisible) {
                    if (!ADMIN_AUTH_CODE.equals(authCode)) {
                        // Pokaż błąd i zatrzymaj dalsze wykonywanie
                        errorbox.setText("Błędny kod");
                        errorbox.setVisibility(View.VISIBLE);
                        return; // Zatrzymaj wykonywanie - nie przechodź dalej
                    }
                    // Jeśli kod jest poprawny, ukryj błąd
                    errorbox.setVisibility(View.GONE);
                }

                // Sprawdź, czy nazwa użytkownika już istnieje
                User existingUser = databaseClient.getAppDatabase().userDao().getUserByUsername(username);
                if (existingUser != null) {
                    Toast.makeText(RegisterActivity.this, "Nazwa użytkownika jest już zajęta", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Określ typ użytkownika na podstawie kodu uwierzytelniania
                String userType = isAuthCodeVisible ? "admin" : "user";

                // Utwórz i zapisz nowego użytkownika
                User user = new User(username, password, userType);
                long userId = databaseClient.getAppDatabase().userDao().insertUser(user);

                if (userId > 0) {
                    Toast.makeText(RegisterActivity.this, "Rejestracja udana! Możesz się teraz zalogować.", Toast.LENGTH_SHORT).show();
                    // Przekieruj z powrotem do ekranu logowania
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Błąd podczas rejestracji. Spróbuj ponownie.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
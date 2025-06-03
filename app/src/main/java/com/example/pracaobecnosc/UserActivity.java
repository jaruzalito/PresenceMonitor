package com.example.pracaobecnosc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserActivity extends AppCompatActivity {

    private TextView textWelcome, textWorkStatus, textWorkTime, textStartTimeInfo, textHolidayInfo,textQuote;
    private Button btnToggleWork, btnLogout;

    private DatabaseClient databaseClient;
    private int userId;
    private String username;
    private User currentUser;
    private WorkSession currentSession;

    private boolean isWorking = false;
    private long startTimeMillis = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // Inicjalizacja bazy danych
        databaseClient = DatabaseClient.getInstance(getApplicationContext());

        // Pobierz dane zalogowanego użytkownika
        SharedPreferences preferences = getSharedPreferences("pracaObecnoscPrefs", MODE_PRIVATE);
        userId = preferences.getInt("userId", -1);
        username = preferences.getString("username", "");

        if (userId == -1) {
            // Jeśli nie ma zalogowanego użytkownika, przekieruj do logowania
            Intent intent = new Intent(UserActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Znajdź komponenty UI
        textQuote = findViewById(R.id.textQuote);
        textWelcome = findViewById(R.id.textWelcome);
        textWorkStatus = findViewById(R.id.textWorkStatus);
        textWorkTime = findViewById(R.id.textWorkTime);
        textStartTimeInfo = findViewById(R.id.textStartTimeInfo);
        textHolidayInfo = findViewById(R.id.textHolidayInfo);
        btnToggleWork = findViewById(R.id.btnToggleWork);
        btnLogout = findViewById(R.id.btnLogout);
        // Ustaw powitanie
        textWelcome.setText("Witaj, " + username + "!");

        // Pobierz informacje o użytkowniku
        currentUser = databaseClient.getAppDatabase().userDao().getUserByUsername(username);

        // Sprawdź czy użytkownik już pracuje
        if ("working".equals(currentUser.getStatus())) {
            isWorking = true;
            currentSession = databaseClient.getAppDatabase().workSessionDao().getActiveSessionForUser(userId);
            if (currentSession != null) {
                startTimeMillis = currentSession.getStartTime().getTime();
                updateUIForWorkStarted();
            }
        }

        // Skonfiguruj aktualizację timera
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isWorking) {
                    long millis = System.currentTimeMillis() - startTimeMillis;
                    int hours = (int) (millis / (1000 * 60 * 60));
                    int minutes = (int) (millis / (1000 * 60)) % 60;
                    int seconds = (int) (millis / 1000) % 60;

                    String timeText = String.format(Locale.getDefault(),
                            "Czas pracy: %02d:%02d:%02d", hours, minutes, seconds);
                    textWorkTime.setText(timeText);

                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        // Obsługa przycisku rozpoczęcia/zakończenia pracy
        btnToggleWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isWorking) {
                    startWork();
                } else {
                    stopWork();
                }
            }
        });

        // Obsługa przycisku wylogowania
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Wyczyść dane zalogowanego użytkownika
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();

                // Przekieruj do ekranu logowania
                Intent intent = new Intent(UserActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Pobierz informacje o świętach z API
        fetchHolidayInfo();
        fetchMotivationalQuote();
    }

    private void startWork() {
        // Aktualizuj status użytkownika
        currentUser.setStatus("working");
        databaseClient.getAppDatabase().userDao().updateUser(currentUser);

        // Utwórz nową sesję pracy
        Date startTime = new Date();
        startTimeMillis = startTime.getTime();
        currentSession = new WorkSession(userId, startTime);
        long sessionId = databaseClient.getAppDatabase().workSessionDao().insertWorkSession(currentSession);

        isWorking = true;
        updateUIForWorkStarted();

        // Rozpocznij timer
        timerHandler.postDelayed(timerRunnable, 0);

        Toast.makeText(this, "Rozpoczęto pracę", Toast.LENGTH_SHORT).show();
    }

    private void stopWork() {
        // Aktualizuj status użytkownika
        currentUser.setStatus("offline");
        databaseClient.getAppDatabase().userDao().updateUser(currentUser);

        // Zakończ sesję pracy
        if (currentSession != null) {
            currentSession.setEndTime(new Date());
            databaseClient.getAppDatabase().workSessionDao().updateWorkSession(currentSession);
        }

        isWorking = false;
        timerHandler.removeCallbacks(timerRunnable);
        updateUIForWorkStopped();

        Toast.makeText(this, "Zakończono pracę", Toast.LENGTH_SHORT).show();
    }

    private void updateUIForWorkStarted() {
        textWorkStatus.setText("Status: Pracujesz");
        btnToggleWork.setText("Zakończ pracę");
        textWorkTime.setVisibility(View.VISIBLE);
        textStartTimeInfo.setVisibility(View.VISIBLE);

        // Ustaw tekst czasu rozpoczęcia
        if (currentSession != null && currentSession.getStartTime() != null) {
            String startTimeText = "Rozpoczęto pracę o: " +
                    hourFormat.format(currentSession.getStartTime());
            textStartTimeInfo.setText(startTimeText);
        }
    }

    private void updateUIForWorkStopped() {
        textWorkStatus.setText("Status: Nie pracujesz");
        btnToggleWork.setText("Zacznij pracę");
        textWorkTime.setVisibility(View.GONE);
        textStartTimeInfo.setVisibility(View.GONE);
    }

    private void fetchHolidayInfo() {
        // Pobierz aktualny rok
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // Utwórz klienta API
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // Wykonaj żądanie do API
        Call<List<HolidayResponse>> call = apiService.getPublicHolidays(currentYear, "PL");
        call.enqueue(new Callback<List<HolidayResponse>>() {
            @Override
            public void onResponse(Call<List<HolidayResponse>> call, Response<List<HolidayResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<HolidayResponse> holidays = response.body();

                    if (holidays.isEmpty()) {
                        textHolidayInfo.setText("Brak informacji o świętach");
                    } else {
                        // Znajdź najbliższe święto
                        HolidayResponse nextHoliday = findNextHoliday(holidays);
                        if (nextHoliday != null) {
                            textHolidayInfo.setText("Najbliższe święto: " +
                                    nextHoliday.getLocalName() + " (" + nextHoliday.getDate() + ")");
                        } else {
                            textHolidayInfo.setText("Brak nadchodzących świąt w tym roku");
                        }
                    }
                } else {
                    textHolidayInfo.setText("Nie udało się pobrać informacji o świętach");
                }
            }

            @Override
            public void onFailure(Call<List<HolidayResponse>> call, Throwable t) {
                textHolidayInfo.setText("Błąd połączenia z API: " + t.getMessage());
            }
        });
    }

    private HolidayResponse findNextHoliday(List<HolidayResponse> holidays) {
        Date today = new Date();
        HolidayResponse nextHoliday = null;
        long minDaysDiff = Long.MAX_VALUE;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (HolidayResponse holiday : holidays) {
            try {
                Date holidayDate = dateFormat.parse(holiday.getDate());
                if (holidayDate != null && holidayDate.after(today)) {
                    long diffInMillies = holidayDate.getTime() - today.getTime();
                    long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

                    if (diffInDays < minDaysDiff) {
                        minDaysDiff = diffInDays;
                        nextHoliday = holiday;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return nextHoliday;
    }

    private void fetchMotivationalQuote() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://zenquotes.io/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        QuoteApi quoteApi = retrofit.create(QuoteApi.class);

        quoteApi.getRandomQuote().enqueue(new Callback<List<Quote>>() {
            @Override
            public void onResponse(Call<List<Quote>> call, Response<List<Quote>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Quote quote = response.body().get(0);
                    String message = "\"" + quote.getQ() + "\"\n- " + quote.getA();
                    textQuote.setText(message);
                }
            }

            @Override
            public void onFailure(Call<List<Quote>> call, Throwable t) {
                textQuote.setText("Nie udało się pobrać cytatu");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isWorking) {
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
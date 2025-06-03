package com.example.pracaobecnosc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";

    private Button btnStartWork;
    private Button btnStopWork;
    private Button btnShowAllUsers;
    private Button btnShowWorkingUsers;
    private Button btnShowOfflineUsers;
    private TextView tvWorkStatus;
    private TextView tvWorkTime;
    private ListView lvUsers;

    private User currentUser;
    private DatabaseClient databaseClient;
    private WorkSession currentWorkSession;

    private boolean isWorking = false;
    private long startTime = 0;
    private Thread timerThread;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: AdminActivity uruchomiona");
        setContentView(R.layout.activity_admin);

        Toast.makeText(this, "Panel administratora", Toast.LENGTH_SHORT).show();

        // Inicjalizacja bazy danych
        databaseClient = DatabaseClient.getInstance(this);
        Log.d(TAG, "onCreate: DatabaseClient zainicjalizowany");

        // Inicjalizacja widoków
        btnStartWork = findViewById(R.id.btnStartWork);
        btnStopWork = findViewById(R.id.btnStopWork);
        btnShowAllUsers = findViewById(R.id.btnShowAllUsers);
        btnShowWorkingUsers = findViewById(R.id.btnShowWorkingUsers);
        btnShowOfflineUsers = findViewById(R.id.btnShowOfflineUsers);
        tvWorkStatus = findViewById(R.id.tvWorkStatus);
        tvWorkTime = findViewById(R.id.tvWorkTime);
        lvUsers = findViewById(R.id.lvUsers);

        Log.d(TAG, "onCreate: Widoki zainicjalizowane");

        // Pobranie danych użytkownika z intentu
        String username = getIntent().getStringExtra("username");
        Log.d(TAG, "onCreate: Otrzymano username z intentu: " + username);

        if (username == null || username.trim().isEmpty()) {
            Log.e(TAG, "onCreate: Brak username w intentcie!");
            Toast.makeText(this, "Błąd: Brak nazwy użytkownika", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Pobierz użytkownika z bazy danych w osobnym wątku
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentUser = databaseClient.getAppDatabase().userDao().getUserByUsername(username);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (currentUser == null) {
                                Log.e(TAG, "Nie znaleziono użytkownika o nazwie: " + username);
                                Toast.makeText(AdminActivity.this, "Nie znaleziono użytkownika!", Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }

                            Log.d(TAG, "Znaleziono użytkownika: " + currentUser.getUsername() + ", typ: " + currentUser.getType());

                            // Sprawdź czy to admin
                            if (!"admin".equals(currentUser.getType())) {
                                Log.e(TAG, "Użytkownik nie jest administratorem! Typ: " + currentUser.getType());
                                Toast.makeText(AdminActivity.this, "Dostęp tylko dla administratorów!", Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }

                            // Kontynuuj inicjalizację po pobraniu użytkownika
                            checkWorkStatus();
                            setupClickListeners();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Błąd podczas pobierania użytkownika: " + e.getMessage());
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AdminActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }
            }
        }).start();
    }

    private void setupClickListeners() {
        btnStartWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWork();
            }
        });

        btnStopWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWork();
            }
        });

        btnShowAllUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAllUsers();
            }
        });

        btnShowWorkingUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWorkingUsers();
            }
        });

        btnShowOfflineUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOfflineUsers();
            }
        });

        Log.d(TAG, "Listenery przycisków skonfigurowane");
    }

    private void checkWorkStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Sprawdzenie, czy użytkownik już rozpoczął pracę
                    final WorkSession activeSession = databaseClient.getAppDatabase().workSessionDao().getActiveSessionForUser(currentUser.getId());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (activeSession != null) {
                                Log.d(TAG, "Znaleziono aktywną sesję dla użytkownika");
                                isWorking = true;
                                currentWorkSession = activeSession;
                                startTime = currentWorkSession.getStartTime().getTime();
                                updateUI();
                                startTimer();
                            } else {
                                Log.d(TAG, "Brak aktywnej sesji dla użytkownika");
                                btnStartWork.setEnabled(true);
                                btnStopWork.setEnabled(false);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Błąd podczas sprawdzania statusu pracy: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startWork() {
        if (!isWorking) {
            Log.d(TAG, "Rozpoczynanie pracy...");
            isWorking = true;
            startTime = System.currentTimeMillis();

            // Tworzenie nowej sesji pracy
            currentWorkSession = new WorkSession();
            currentWorkSession.setUserId(currentUser.getId());
            currentWorkSession.setStartTime(new Date(startTime));

            // Zapisanie do bazy danych
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        long sessionId = databaseClient.getAppDatabase().workSessionDao().insertWorkSession(currentWorkSession);
                        currentWorkSession.setId(sessionId);
                        Log.d(TAG, "Utworzono nową sesję pracy z ID: " + sessionId);

                        // Aktualizacja statusu użytkownika
                        currentUser.setStatus("working");
                        databaseClient.getAppDatabase().userDao().updateUser(currentUser);
                        Log.d(TAG, "Zaktualizowano status użytkownika na: working");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                                startTimer();
                                Toast.makeText(AdminActivity.this, "Praca rozpoczęta!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Błąd podczas rozpoczynania pracy: " + e.getMessage());
                        e.printStackTrace();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isWorking = false;
                                Toast.makeText(AdminActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void stopWork() {
        if (isWorking && currentWorkSession != null) {
            Log.d(TAG, "Kończenie pracy...");
            isWorking = false;
            stopTimer();

            // Aktualizacja sesji pracy
            long endTime = System.currentTimeMillis();
            currentWorkSession.setEndTime(new Date(endTime));

            // Zapisanie do bazy danych
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        databaseClient.getAppDatabase().workSessionDao().updateWorkSession(currentWorkSession);
                        Log.d(TAG, "Zaktualizowano sesję pracy z ID: " + currentWorkSession.getId());

                        // Aktualizacja statusu użytkownika
                        currentUser.setStatus("offline");
                        databaseClient.getAppDatabase().userDao().updateUser(currentUser);
                        Log.d(TAG, "Zaktualizowano status użytkownika na: offline");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                                Toast.makeText(AdminActivity.this, "Praca zakończona!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Błąd podczas kończenia pracy: " + e.getMessage());
                        e.printStackTrace();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AdminActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void updateUI() {
        if (isWorking) {
            btnStartWork.setEnabled(false);
            btnStopWork.setEnabled(true);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String startTimeFormatted = sdf.format(new Date(startTime));
            tvWorkStatus.setText("Praca rozpoczęta o godzinie: " + startTimeFormatted);

            Log.d(TAG, "UI zaktualizowany - pracuje");
        } else {
            btnStartWork.setEnabled(true);
            btnStopWork.setEnabled(false);
            tvWorkStatus.setText("Nie pracujesz obecnie");
            tvWorkTime.setText("");

            Log.d(TAG, "UI zaktualizowany - nie pracuje");
        }
    }

    private void startTimer() {
        if (!isTimerRunning) {
            Log.d(TAG, "Uruchamianie timera");
            isTimerRunning = true;
            timerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isTimerRunning) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long elapsedTime = System.currentTimeMillis() - startTime;
                                updateTimerText(elapsedTime);
                            }
                        });

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Timer przerwany: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            });
            timerThread.start();
        }
    }

    private void stopTimer() {
        Log.d(TAG, "Zatrzymywanie timera");
        isTimerRunning = false;
        if (timerThread != null) {
            timerThread.interrupt();
            timerThread = null;
        }
    }

    private void updateTimerText(long elapsedTime) {
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = (elapsedTime / (1000 * 60 * 60));

        tvWorkTime.setText(String.format(Locale.getDefault(), "Przepracowany czas: %02d:%02d:%02d", hours, minutes, seconds));
    }

    private void showAllUsers() {
        Log.d(TAG, "Pobieranie listy wszystkich użytkowników");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<User> allUsers = databaseClient.getAppDatabase().userDao().getAllUsers();
                    Log.d(TAG, "Pobrano " + allUsers.size() + " użytkowników");
                    displayUserList(allUsers, "Wszyscy pracownicy");
                } catch (Exception e) {
                    Log.e(TAG, "Błąd podczas pobierania użytkowników: " + e.getMessage());
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AdminActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void showWorkingUsers() {
        Log.d(TAG, "Pobieranie listy pracujących użytkowników");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<User> workingUsers = databaseClient.getAppDatabase().userDao().getUsersByStatus("working");
                    Log.d(TAG, "Pobrano " + workingUsers.size() + " pracujących użytkowników");
                    displayUserList(workingUsers, "Pracujący pracownicy");
                } catch (Exception e) {
                    Log.e(TAG, "Błąd podczas pobierania pracujących użytkowników: " + e.getMessage());
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AdminActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void showOfflineUsers() {
        Log.d(TAG, "Pobieranie listy nieobecnych użytkowników");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<User> offlineUsers = databaseClient.getAppDatabase().userDao().getOfflineUsers();
                    Log.d(TAG, "Pobrano " + offlineUsers.size() + " nieobecnych użytkowników");
                    displayUserList(offlineUsers, "Nieobecni pracownicy");
                } catch (Exception e) {
                    Log.e(TAG, "Błąd podczas pobierania nieobecnych użytkowników: " + e.getMessage());
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AdminActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void displayUserList(final List<User> users, final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<String> userDisplayList = new ArrayList<>();
                for (User user : users) {
                    userDisplayList.add(user.getUsername() + " - " + user.getStatus());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminActivity.this,
                        android.R.layout.simple_list_item_1, userDisplayList);
                lvUsers.setAdapter(adapter);

                Toast.makeText(AdminActivity.this, title + ": " + users.size(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Wyświetlono listę użytkowników: " + title);
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: AdminActivity niszczona");
        stopTimer();
        super.onDestroy();
    }
}
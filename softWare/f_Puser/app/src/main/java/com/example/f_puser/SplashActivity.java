package com.example.f_puser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private String mainCollection = "1";
    final String currentVersion = "4"; // حدديه يدوي من السورس


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("isFirstRun_" + currentVersion, true);


        if (isFirstRun) {
            Intent intent = new Intent(SplashActivity.this, smartAssistant.class);
            intent.putExtra("firstTime", true);
            intent.putExtra("versionKey", "isFirstRun_" + currentVersion); // ده المهم
            startActivity(intent);
            finish();
            return;
        }


        // باقي اللوجيك بعد أول مرة بس
        if (!isInternetAvailable()) {
            goToOffline();
            return;
        }

        checkAppVersion();
    }



    private void goToOffline() {
        startActivity(new Intent(SplashActivity.this, offLine.class));
        finish();
    }

    private void checkAppVersion() {
        DatabaseReference versionRef = FirebaseDatabase.getInstance()
                .getReference(mainCollection)
                .child("upDate").child("appVersion");

        versionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String latestVersion = snapshot.getValue(String.class);
                if (latestVersion != null && !latestVersion.equals(currentVersion)) {
                    showUpdateDialog();
                } else {
                    continueAppLogic();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                continueAppLogic();
            }
        });
    }


    private void goToLogin() {
        startActivity(new Intent(SplashActivity.this, login.class));
        finish();
    }

    private void checkUserPeriod(String kidId) {
        DatabaseReference userPeriodRef = FirebaseDatabase.getInstance()
                .getReference(mainCollection)
                .child("data")
                .child(kidId)
                .child("userEndPeriod");

        userPeriodRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String endPeriodStr = snapshot.getValue(String.class);
                if (isPeriodOver(endPeriodStr)) {
                    goToPayPeriod();
                } else {
                    goToMain();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                goToLogin();
            }
        });
    }

    private void goToPayPeriod() {
        startActivity(new Intent(SplashActivity.this, payPeriod.class));
        finish();
    }

    private void goToMain() {
        startActivity(new Intent(SplashActivity.this, report.class));
        finish();
    }



    private void continueAppLogic() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String kidId = sharedPreferences.getString("kidId_selected", null);


        if (kidId == null) {
            startActivity(new Intent(SplashActivity.this, login.class));
            finish();
            return;
        }

        DatabaseReference userPeriodRef = FirebaseDatabase.getInstance()
                .getReference(mainCollection)
                .child("data")
                .child(kidId)
                .child("userEndPeriod");

        userPeriodRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String endPeriodStr = snapshot.getValue(String.class);
                if (isPeriodOver(endPeriodStr)) {
                    Intent intent = new Intent(SplashActivity.this, payPeriod.class);
                    intent.putExtra("kidId", kidId);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(SplashActivity.this, report.class));
                }
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                startActivity(new Intent(SplashActivity.this, login.class));
                finish();
            }
        });
    }


    private void showUpdateDialog() {
        DatabaseReference updateRef = FirebaseDatabase.getInstance()
                .getReference(mainCollection)
                .child("upDate");

        updateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String newVersionURL = snapshot.child("newVersionURL").getValue(String.class);
                    String latestVersion = snapshot.child("appVersion").getValue(String.class);

                    if (newVersionURL != null && !newVersionURL.trim().isEmpty() && latestVersion != null) {
                        String message = "النسخة الحالية: " + currentVersion
                                + "\nالنسخة الجديدة: " + latestVersion
                                + "\nيجب التحديث للاستمرار.";

                        new androidx.appcompat.app.AlertDialog.Builder(SplashActivity.this)
                                .setTitle("تحديث متاح")
                                .setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton("تحديث", (dialog, which) -> {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(newVersionURL));
                                    startActivity(intent);
                                    finish();
                                })
                                .setNegativeButton("خروج", (dialog, which) -> {
                                    finish();
                                })
                                .show();
                    } else {
                        Toast.makeText(SplashActivity.this, "بيانات التحديث غير مكتملة", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SplashActivity.this, "فشل في قراءة بيانات التحديث", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    private boolean isPeriodOver(String endDateStr) {
        if (endDateStr == null || endDateStr.isEmpty()) return false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date endDate = dateFormat.parse(endDateStr);
            Date today = Calendar.getInstance().getTime();
            return endDate != null && endDate.before(today);
        } catch (ParseException e) {
            return false;
        }
    }
}

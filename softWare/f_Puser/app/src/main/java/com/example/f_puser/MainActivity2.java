package com.example.f_puser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

public class MainActivity2 extends AppCompatActivity {
    private String mainCollection = "1";

    TextView textView, KidName;
    DatabaseReference databaseRefer;
    Button logoutButton, order, buttonToReport, contant,gpt,off;
    private SharedPreferences sharedPreferences;
    private MediaPlayer mediaPlayer;
    private String kidId;

    @Override
    protected void onStart() {
        super.onStart();

        // استخدام ValueEventListener للانتظار حتى يتم قراءة اسم الطفل
        DatabaseReference kidNameRef = FirebaseDatabase.getInstance()
                .getReference(mainCollection)
                .child("data")
                .child(kidId)
                .child("kidName");
        kidNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String kidName = snapshot.getValue(String.class);
                    KidName.setText(kidName);
                } else {
                    // إذا لم يتم العثور على اسم الطفل
                    Toast.makeText(MainActivity2.this, "اسم الطفل غير موجود في قاعدة البيانات", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity2.this, "حدث خطأ أثناء قراءة اسم الطفل", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        order = findViewById(R.id.order);
//        textView = findViewById(R.id.textView);
        logoutButton = findViewById(R.id.button2);
        KidName = findViewById(R.id.KidName);
        contant = findViewById(R.id.contant);
        gpt = findViewById(R.id.GPT);
        off=findViewById(R.id.offLine);
        gpt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, smartAssistant.class);
                startActivity(intent);
            }
        });

        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, offLine.class);
                startActivity(intent);
            }
        });


        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        kidId = sharedPreferences.getString("kidId_selected", null);

        if (kidId == null) {
            Toast.makeText(this, "لم يتم العثور على kidId. يرجى إعادة تسجيل الدخول.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity2.this, login.class);
            startActivity(intent);
            finish();
            return;
        }

        buttonToReport = findViewById(R.id.buttonToReport);

        Button addKidButton = findViewById(R.id.addKidButton);
        addKidButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, login.class);
            startActivity(intent);
            finish(); // أو لأ حسب ما تحبي
        });

        Button switchKidButton = findViewById(R.id.switchKidButton);
        switchKidButton.setOnClickListener(v -> {
            int kidsCount = sharedPreferences.getInt("kidsCount", 0);

            if (kidsCount == 0) {
                Toast.makeText(this, "لا يوجد طلاب للتبديل", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] kidNames = new String[kidsCount];
            for (int i = 0; i < kidsCount; i++) {
                kidNames[i] = sharedPreferences.getString("kidName_" + i, "طالب");
            }

            new AlertDialog.Builder(this)
                    .setTitle("اختار الطالب")
                    .setItems(kidNames, (dialog, which) -> {
                        String selectedId = sharedPreferences.getString("kidId_" + which, null);
                        sharedPreferences.edit().putString("kidId_selected", selectedId).apply();
                        recreate(); // إعادة تحميل الصفحة بالطالب الجديد
                    })
                    .show();
        });




        contant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, Contant.class);
                intent.putExtra("kidId", kidId); // إرسال kidId إلى النشاط التالي
                startActivity(intent);
            }
        });

        buttonToReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (kidId != null) {
                    Intent intent = new Intent(MainActivity2.this, report.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity2.this, "لم يتم العثور على kidId. يرجى إعادة تسجيل الدخول.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        databaseRefer = FirebaseDatabase.getInstance().getReference(mainCollection).child("endPeriod");
        databaseRefer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // استخراج بيانات نهاية الفترة
                String endPeriodValue = dataSnapshot.getValue(String.class);

                if (endPeriodValue != null) {
                    // التحقق مما إذا كان التاريخ في الماضي
                    checkEndPeriod(endPeriodValue);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // التعامل مع الخطأ
                Log.e("FirebaseError", "Database error: " + databaseError.getMessage());
            }
        });

        // إعداد MediaPlayer لتشغيل الصوت
        mediaPlayer = MediaPlayer.create(this, R.raw.bell);

//        databaseRefer = FirebaseDatabase.getInstance().getReference(mainCollection).child("data")
//                .child(kidId).child("userEndPeriod");
//        databaseRefer.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // استخراج بيانات نهاية الفترة
//                String userEndPeriodValue = dataSnapshot.getValue(String.class);
//
//                if (userEndPeriodValue != null) {
//                    // التحقق مما إذا كان التاريخ في الماضي
//                    checkUserEndPeriod(userEndPeriodValue);
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                // التعامل مع الخطأ
//                Log.e("FirebaseError", "Database error: " + databaseError.getMessage());
//            }
//        });

        databaseRefer.child(mainCollection).child("data").child(kidId).child("isInClass").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isInClass = dataSnapshot.getValue(Boolean.class);
                    if (isInClass != null) {
                        textView.setText(isInClass ? "في المدرسة" : "ليس في المدرسة");
                        textView.setTextColor(isInClass ? Color.GREEN : Color.RED);
                        order.setVisibility(isInClass ? View.VISIBLE : View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                textView.setText("Failed to load data");
                textView.setTextColor(Color.RED);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("kidId_selected"); // امسح المفتاح الصح
                editor.apply();

                Intent intent = new Intent(MainActivity2.this, login.class);
                startActivity(intent);
                finish();
            }
        });


        order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (kidId == null) {
                    Toast.makeText(MainActivity2.this, "لم يتم العثور على kidId. يرجى إعادة تسجيل الدخول.", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("orders");

                ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long lastNumber = 0;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                long keyNumber = Long.parseLong(child.getKey());
                                if (keyNumber > lastNumber) {
                                    lastNumber = keyNumber;
                                }
                            } catch (NumberFormatException e) {
                                // تجاهل أي كي غريب
                            }
                        }

                        long newKey = lastNumber + 1;

                        DatabaseReference newOrderRef = ordersRef.child(String.valueOf(newKey));

                        String kidNameValue = KidName.getText().toString();

                        newOrderRef.child("kidId").setValue(kidId);
                        newOrderRef.child("type").setValue("remote");
                        newOrderRef.child("kidName").setValue(kidNameValue)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirebaseUpdate", "Order placed successfully.");
                                    Toast.makeText(MainActivity2.this, "Order placed successfully.", Toast.LENGTH_SHORT).show();

                                    MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity2.this, R.raw.bell);
                                    mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                    mediaPlayer.start();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirebaseUpdate", "Failed to place order.", e);
                                    Toast.makeText(MainActivity2.this, "Failed to place order.", Toast.LENGTH_SHORT).show();
                                });

                        DatabaseReference deliveryAlarmRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("deliveryAlarm");
                        deliveryAlarmRef.setValue(true)
                                .addOnSuccessListener(aVoid -> Log.d("FirebaseUpdate", "Delivery alarm set to true successfully."))
                                .addOnFailureListener(e -> Log.e("FirebaseUpdate", "Failed to set delivery alarm.", e));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity2.this, "حدث خطأ في قراءة البيانات.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void checkEndPeriod(String endPeriodStr) {
        // إعداد التنسيق
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            // تحويل النص إلى تاريخ
            Date endDate = dateFormat.parse(endPeriodStr);
            Date currentDate = Calendar.getInstance().getTime();

            // التحقق مما إذا كان التاريخ في الماضي
            if (endDate != null && endDate.before(currentDate)) {
                // الانتقال إلى نشاط payPeriod
                Intent payPeriodIntent = new Intent(MainActivity2.this, endPeriod.class);
                startActivity(payPeriodIntent);
                finish(); // إنهاء النشاط الحالي
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

//    private void checkUserEndPeriod(String endPeriodStr) {
//        // إعداد التنسيق
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        try {
//            // تحويل النص إلى تاريخ
//            Date endDate = dateFormat.parse(endPeriodStr);
//            Date currentDate = Calendar.getInstance().getTime();
//
//            // التحقق مما إذا كان التاريخ في الماضي
//            if (endDate != null && endDate.before(currentDate)) {
//                // الانتقال إلى نشاط payPeriod
//                Intent payPeriodIntent = new Intent(MainActivity2.this, payPeriod.class);
//                startActivity(payPeriodIntent);
//                finish(); // إنهاء النشاط الحالي
//            }
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

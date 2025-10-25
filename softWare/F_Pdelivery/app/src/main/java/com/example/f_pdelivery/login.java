package com.example.f_pdelivery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class login extends AppCompatActivity {
    private String mainCollection = "1";


    private EditText nameEditText;
    private EditText idEditText;
//    private String currentVersion = "1"; // حددي هنا رقم النسخة الحالية يدويًا

    private Button button;

    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        continueApp();
//    }


//    private void checkAppVersion() {
//        DatabaseReference versionRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("appVersion");
//
//        versionRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    String firebaseVersion = snapshot.getValue(String.class);
//
//                    if (!currentVersion.equals(firebaseVersion)) {
//                        new AlertDialog.Builder(login.this)
//                                .setTitle("تحديث مطلوب")
//                                .setMessage("فيه نسخة أحدث من التطبيق. لازم تحدث علشان تقدر تستخدمه.")
//                                .setCancelable(false)
//                                .setPositiveButton("تحميل التحديث", (dialog, which) -> {
//                                    String downloadUrl = "https://t.me/+iU2p-U3Pin83Y2M0";
//                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
//                                    startActivity(browserIntent);
//                                    finish();
//                                })
//                                .show();
//                    } else {
//                        continueApp();
//                    }
//                } else {
//                    continueApp(); // لو مفيش قيمة، كمل عادي
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(login.this, "فشل في التحقق من إصدار التطبيق", Toast.LENGTH_SHORT).show();
//                continueApp();
//            }
//        });
//    }


//    private void continueApp() {
//        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
//        if (sharedPreferences.contains("kidId")) {
//            if (isInternetConnected()) {
//                Intent intent = new Intent(login.this, MainActivity2.class);
//                startActivity(intent);
//                finish();
//            } else {
//                showNoInternetDialog();
//            }
//            return;
//        }
//
//        setContentView(R.layout.activity_main);
//
//        nameEditText = findViewById(R.id.name);
//        idEditText = findViewById(R.id.id);
//        button = findViewById(R.id.button);
//
//        databaseReference = FirebaseDatabase.getInstance().getReference(mainCollection).child("users");
//
//        button.setOnClickListener(v -> {
//            String name = nameEditText.getText().toString().trim();
//            String id = idEditText.getText().toString().trim();
//
//            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(id)) {
//                Toast.makeText(login.this, "برجاء إدخال جميع الحقول", Toast.LENGTH_SHORT).show();
//            } else {
//                if (isInternetConnected()) {
//                    authenticateUser(name, id);
//                } else {
//                    showNoInternetDialog();
//                }
//            }
//        });
//    }
//
//
//
//    private void authenticateUser(String name, String id) {
//        Query query = databaseReference.orderByChild("id").equalTo(id);
//        query.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    boolean isAuthenticated = false;
//                    String kidId = null;
//                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
//                        String dbName = userSnapshot.child("name").getValue(String.class);
//
//                        // تعديل طريقة التحقق من الاسم
//                        if (dbName != null && dbName.trim().equalsIgnoreCase(name.trim())) {
//                            isAuthenticated = true;
//                            kidId = userSnapshot.child("KidId").getValue(String.class);
//                            break;
//                        }
//                    }
//                    if (isAuthenticated && kidId != null) {
//                        int count = sharedPreferences.getInt("kidsCount", 0);
//                        boolean alreadyExists = false;
//
//                        for (int i = 0; i < count; i++) {
//                            String existingId = sharedPreferences.getString("kidId_" + i, "");
//                            if (kidId.equals(existingId)) {
//                                alreadyExists = true;
//                                break;
//                            }
//                        }
//
//                        if (alreadyExists) {
//                            Toast.makeText(login.this, "هذا الطالب موجود بالفعل", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//
//                        // ماكانش متسجل قبل كده
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        editor.putString("kidId_" + count, kidId);
//                        editor.putString("kidName_" + count, name);
//                        editor.putInt("kidsCount", count + 1);
//                        editor.putString("kidId_selected", kidId);
//                        editor.apply();
//
//                        Intent intent = new Intent(login.this, report.class);
//                        startActivity(intent);
//                        finish();
//                    }
//                    else {
//                        Toast.makeText(login.this, "خطأ في الاسم", Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    Toast.makeText(login.this, "خطأ في الرقم القومي", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(login.this, "حدث خطأ في الاتصال بقاعدة البيانات", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private boolean isInternetConnected() {
//        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
//    }
//
//    private void showNoInternetDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage("النت مش شغال شغل عشان تعدي ( :")
//                .setPositiveButton("هشغلو و اجيلك", (dialog, which) -> {
//                    finish();
//                    dialog.dismiss();
//                })
//                .show();
    }
}

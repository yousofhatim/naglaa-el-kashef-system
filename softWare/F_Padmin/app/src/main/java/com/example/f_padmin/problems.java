package com.example.f_padmin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class problems extends AppCompatActivity {
    private String mainCollection = "1";

    private LinearLayout container;
    private DatabaseReference complaintsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ScrollView رئيسي
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Container ديناميك
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 30, 30, 30);
        scrollView.addView(container);

        setContentView(scrollView);

        complaintsRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("FeedbackCritical");

        loadComplaints();
    }

    private void loadComplaints() {
        complaintsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                container.removeAllViews();

                for (DataSnapshot complaintSnap : snapshot.getChildren()) {
                    String key = complaintSnap.getKey();
                    String complaint = complaintSnap.child("complaintMessage").getValue(String.class);
                    String studentName = complaintSnap.child("studentName").getValue(String.class);
                    String phone = complaintSnap.child("parentPhone").getValue(String.class);
                    String date = complaintSnap.child("date").getValue(String.class);
                    String time = complaintSnap.child("time").getValue(String.class);

                    addComplaintCard(key, complaint, studentName, phone, date, time);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(problems.this, "فشل تحميل الشكاوى", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addComplaintCard(String key, String complaint, String studentName, String phone, String date, String time) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundColor(Color.parseColor("#FFF0F0"));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 40);
        card.setLayoutParams(cardParams);

        // شكوى
        TextView complaintText = new TextView(this);
        complaintText.setText("📌 " + complaint);
        complaintText.setTextSize(17);
        complaintText.setTextColor(Color.BLACK);
        card.addView(complaintText);

        // اسم الطالب
        TextView studentText = new TextView(this);
        studentText.setText("👦 الطالب: " + (studentName != null ? studentName : "غير معروف"));
        studentText.setTextColor(Color.DKGRAY);
        card.addView(studentText);

        // رقم الهاتف
        TextView phoneText = new TextView(this);
        phoneText.setText("📞 ولي الأمر: " + (phone != null ? phone : "غير معروف"));
        phoneText.setTextColor(Color.BLUE);
        phoneText.setOnClickListener(v -> {
            if (phone != null && phone.length() > 5) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(callIntent);
            }
        });
        card.addView(phoneText);

        // التاريخ والوقت
        TextView timeText = new TextView(this);
        timeText.setText("🗓️ " + date + "   🕒 " + time);
        timeText.setTextColor(Color.GRAY);
        card.addView(timeText);

        // زر الحذف
        Button deleteBtn = new Button(this);
        deleteBtn.setText("🗑️ حذف الشكوى");
        deleteBtn.setOnClickListener(v -> {
            complaintsRef.child(key).removeValue();
            container.removeView(card);
        });
        card.addView(deleteBtn);

        container.addView(card);
    }
}

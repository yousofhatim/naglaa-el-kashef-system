package com.example.f_padmin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class totalInClass extends AppCompatActivity {
    private String mainCollection = "1";
    private TextView totalInClass;
    private LinearLayout forTextViewOutOfSystem;
    private LinearLayout currentDetailsLayout;  // لإضافة التفاصيل
    private DatabaseReference databaseReference;  // مرجع قاعدة البيانات

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_in_class);

        totalInClass = findViewById(R.id.totalInClass);
        forTextViewOutOfSystem = findViewById(R.id.forTextViewOutOfSystem);

        // الإشارة إلى قاعدة بيانات Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference().child(mainCollection).child("data");

        // الاستماع إلى التغييرات في قاعدة البيانات
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                forTextViewOutOfSystem.removeAllViews(); // تنظيف الأسماء القديمة

                List<Kid> kidsList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Boolean isInClass = snapshot.child("isInClass").getValue(Boolean.class);
                    String kidName = snapshot.child("kidName").getValue(String.class);
                    String kidId = snapshot.getKey();

                    if (isInClass != null && isInClass) {
                        count++;
                        kidsList.add(new Kid(kidName, kidId));
                    }
                }

                // فرز القائمة أبجدياً بناءً على الأسماء
                Collections.sort(kidsList, new Comparator<Kid>() {
                    @Override
                    public int compare(Kid kid1, Kid kid2) {
                        return kid1.getKidName().compareTo(kid2.getKidName());
                    }
                });

                // إضافة الأسماء المرتبة إلى الواجهة
                for (Kid kid : kidsList) {
                    addKidNameTextView(kid.getKidName(), kid.getKidId());
                }

                totalInClass.setText("عدد المتواجدين: " + count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // التعامل مع الأخطاء هنا
            }
        });
    }

    private void addKidNameTextView(String kidName, String kidId) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 10, 10, 10); // القيم: (يمين, أعلى, يسار, أسفل)

        TextView textView = new TextView(this);
        textView.setLayoutParams(params);
        textView.setText(kidName);
        textView.setTextSize(20);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setBackgroundColor(Color.YELLOW);

        final float scale = getResources().getDisplayMetrics().density;
        int marginInPixels = (int) (10 * scale + 0.5f);
        params.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels);

        // تطبيق الـ LayoutParams على TextView
        textView.setLayoutParams(params);

        // إضافة مستمع للضغط
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKidDetails(kidId, forTextViewOutOfSystem, textView);
            }
        });

        // إضافة TextView إلى LinearLayout
        forTextViewOutOfSystem.addView(textView);
    }

    private void showKidDetails(String kidId, LinearLayout layout, TextView textView) {
        // مسح أي تفاصيل سابقة
        if (currentDetailsLayout != null) {
            layout.removeView(currentDetailsLayout);
        }

        // إنشاء LinearLayout جديد لعرض التفاصيل
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        detailsLayout.setBackgroundColor(Color.GRAY);  // تعيين لون الخلفية إلى اللون الرمادي

        // استرداد بيانات الطفل من المسار data/(wanted kidId)
        databaseReference.child(kidId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String key = childSnapshot.getKey();
                    Object value = childSnapshot.getValue();

                    TextView detailTextView = new TextView(totalInClass.this);
                    detailTextView.setText(key + ": " + value.toString());
                    detailTextView.setTextSize(18);
                    detailTextView.setGravity(Gravity.CENTER);

                    // جعل رقم الهاتف قابلاً للضغط
                    if ("phoneFather".equals(key)) {
                        detailTextView.setTextColor(Color.BLUE);
                        detailTextView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + value.toString()));
                                startActivity(intent);
                            }
                        });
                    }

                    if ("phoneMother".equals(key)) {
                        detailTextView.setTextColor(Color.BLUE);
                        detailTextView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + value.toString()));
                                startActivity(intent);
                            }
                        });
                    }

                    if ("phone3".equals(key)) {
                        detailTextView.setTextColor(Color.BLUE);
                        detailTextView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + value.toString()));
                                startActivity(intent);
                            }
                        });
                    }

                    detailsLayout.addView(detailTextView);
                }
                // إضافة detailsLayout بعد textView
                int index = layout.indexOfChild(textView);
                layout.addView(detailsLayout, index + 1);

                // تعيين currentDetailsLayout للـ detailsLayout الحالي
                currentDetailsLayout = detailsLayout;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Database error: " + databaseError.getMessage());
            }
        });
    }

    // كلاس مساعد لتخزين بيانات الأطفال
    private class Kid {
        private String kidName;
        private String kidId;

        public Kid(String kidName, String kidId) {
            this.kidName = kidName;
            this.kidId = kidId;
        }

        public String getKidName() {
            return kidName;
        }

        public String getKidId() {
            return kidId;
        }
    }
}

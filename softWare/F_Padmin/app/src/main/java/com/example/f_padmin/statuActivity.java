package com.example.f_padmin;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public class statuActivity extends AppCompatActivity {
    private String mainCollection = "1";

    private LinearLayout forTextViewRegular;
    private LinearLayout forTextViewIrregular;
    private LinearLayout forTextViewOutOfSystem;
    private TextView regularCountTextView;
    private TextView irregularCountTextView;
    private TextView outOfSystemCountTextView;
    private DatabaseReference databaseReference;
    private LinearLayout currentDetailsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statu);

        // ربط العناصر من الـ XML
        forTextViewRegular = findViewById(R.id.forTextViewRegular);
        forTextViewIrregular = findViewById(R.id.forTextViewIrregular);
        forTextViewOutOfSystem = findViewById(R.id.forTextViewOutOfSystem);

        // النصوص الرئيسية لعرض عدد الطلاب لكل فئة
        regularCountTextView = findViewById(R.id.regularCountTextView);
        irregularCountTextView = findViewById(R.id.irregularCountTextView);
        outOfSystemCountTextView = findViewById(R.id.outOfSystemCountTextView);

        // الوصول إلى قاعدة البيانات
        databaseReference = FirebaseDatabase.getInstance().getReference(mainCollection).child(mainCollection).child("data");

        // استرداد البيانات
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // إعادة ضبط التعداد
                int regularCount = 0;
                int irregularCount = 0;
                int outOfSystemCount = 0;

                // إزالة أي عناصر سابقة من الـ LinearLayouts
                forTextViewRegular.removeAllViews();
                forTextViewIrregular.removeAllViews();
                forTextViewOutOfSystem.removeAllViews();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // استخراج بيانات الطفل
                    String kidId = snapshot.getKey();
                    String kidName = snapshot.child("kidName").getValue(String.class);
                    String lastDateStr = snapshot.child("lastDate").getValue(String.class);

                    // التحقق من وجود lastDate قبل المتابعة
                    if (lastDateStr == null) {
                        continue; // تجاهل الطالب إذا كان lastDate غير موجود
                    }

                    // حساب الفرق الزمني
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    try {
                        Date lastDate = dateFormat.parse(lastDateStr);
                        Date currentDate = Calendar.getInstance().getTime();

                        long diffInMillis = currentDate.getTime() - lastDate.getTime();
                        long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                        // تحديد الفئة المناسبة بناءً على الفرق الزمني
                        if (diffInDays < 7) {
                            addTextView(forTextViewRegular, kidName, kidId);
                            regularCount++;
                        } else if (diffInDays < 25) {
                            addTextView(forTextViewIrregular, kidName, kidId);
                            irregularCount++;
                        } else {
                            addTextView(forTextViewOutOfSystem, kidName, kidId);
                            outOfSystemCount++;
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                // تحديث النصوص الرئيسية بعدد الطلاب في كل فئة
                regularCountTextView.setText("منتظم (" + regularCount + ")");
                irregularCountTextView.setText("غير منتظم (" + irregularCount + ")");
                outOfSystemCountTextView.setText("خارج المنظومة (" + outOfSystemCount + ")");

                // تخزين العدد في SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("attendance_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("total_kids", regularCount + irregularCount);
                editor.apply();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // التعامل مع الخطأ
                Log.e("FirebaseError", "Database error: " + databaseError.getMessage());
            }
        });
    }

    // إضافة TextView جديد إلى التخطيط المحدد
    private void addTextView(LinearLayout layout, String text, String kidId) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(20);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(Color.YELLOW);

        // إنشاء `LayoutParams` لضبط الهوامش
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        // تحويل 10dp إلى pixels
        final float scale = getResources().getDisplayMetrics().density;
        int marginInPixels = (int) (10 * scale + 0.5f);
        params.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels);

        // تطبيق الـ LayoutParams على TextView
        textView.setLayoutParams(params);

        // إضافة مستمع للضغط
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKidDetails(kidId, layout, textView);
            }
        });

        // إضافة TextView إلى LinearLayout
        layout.addView(textView);
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

                    TextView detailTextView = new TextView(statuActivity.this);
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
}

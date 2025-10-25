package com.example.f_padmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class activate extends AppCompatActivity {
    private String mainCollection = "1";

    private LinearLayout forTextViewNoLastDate;
    private EditText idEditText;
    private TextView nameTextView, errorTextView, totalTextView;
    private Button okButton;

    private DatabaseReference databaseReference;

    private List<Kid> kidsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activate);

        // ربط العناصر من الواجهة
        forTextViewNoLastDate = findViewById(R.id.forTextViewNoLastDate);
        idEditText = findViewById(R.id.id);
        nameTextView = findViewById(R.id.name);
        errorTextView = findViewById(R.id.error); // ربط TextView الخطأ
        totalTextView = findViewById(R.id.total); // ربط TextView العدد الكلي
        okButton = findViewById(R.id.ok);

        // الحصول على مرجع قاعدة البيانات
        databaseReference = FirebaseDatabase.getInstance().getReference(mainCollection);

        // التحقق من قيمة الشايلد "failed" في الوقت الفعلي
        databaseReference.child("failed").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean failed = dataSnapshot.getValue(Boolean.class);
                if (failed != null && failed) {
                    errorTextView.setText("حدث خطأ");
                    errorTextView.setVisibility(View.VISIBLE);
                } else {
                    errorTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(activate.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // تهيئة قائمة الأطفال
        kidsList = new ArrayList<>();

        // البحث في قاعدة البيانات
        fetchDataFromDatabase();

        // إعداد مستمع للزر "OK"
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFirebaseData();
            }
        });
    }

    private void fetchDataFromDatabase() {
        databaseReference.child("data").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    if (!childSnapshot.hasChild("lastDate")) {
                        Object kidNameObj = childSnapshot.child("kidName").getValue();
                        Object fingerIdObj = childSnapshot.child("fingerId").getValue();

                        if (kidNameObj != null && fingerIdObj != null) {
                            String kidName = kidNameObj.toString();
                            String fingerId = fingerIdObj.toString();
                            kidsList.add(new Kid(kidName, fingerId));
                        }
                    }
                }

                // ترتيب قائمة الأطفال أبجديًا
                Collections.sort(kidsList, new Comparator<Kid>() {
                    @Override
                    public int compare(Kid k1, Kid k2) {
                        return k1.getKidName().compareTo(k2.getKidName());
                    }
                });

                // إضافة الأطفال إلى الـ LinearLayout
                for (Kid kid : kidsList) {
                    addKidNameTextView(kid.getKidName(), kid.getFingerId());
                }

                // تحديث العدد الكلي بعد إضافة الأطفال
                updateTotalTextView();

                // مراقبة التغيرات على الشايلد "lastDate" في الوقت الفعلي
                listenForLastDateChanges();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(activate.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addKidNameTextView(String kidName, String fingerId) {
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
                idEditText.setText(fingerId);
                nameTextView.setText(kidName);
            }
        });

        // إضافة TextView إلى LinearLayout
        forTextViewNoLastDate.addView(textView);
        textView.setTag(fingerId); // وضع fingerId كـ tag لتحديد TextView لاحقاً

        // تحديث العدد الكلي بعد إضافة TextView
        updateTotalTextView();
    }

    private void updateTotalTextView() {
        int count = forTextViewNoLastDate.getChildCount();
        totalTextView.setText(" العدد : " + count);
    }

    private void updateFirebaseData() {
        String fingerId = idEditText.getText().toString();
        String kidName = nameTextView.getText().toString();

        if (fingerId.isEmpty() || kidName.isEmpty()) {
            Toast.makeText(this, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference childReference = databaseReference;
        int fingerIdValue = Integer.parseInt(fingerId);

        childReference.child("id").setValue(fingerIdValue);

        childReference.child("add").setValue(true);
        childReference.child("failed").setValue(false);
    }

    private void listenForLastDateChanges() {
        databaseReference.child("data").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                if (dataSnapshot.hasChild("lastDate")) {
                    String fingerId = dataSnapshot.getKey();
                    if (fingerId != null) {
                        removeKidNameTextView(fingerId);
                    }
                }
            }

            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                // لا شيء
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // لا شيء
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                // لا شيء
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(activate.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeKidNameTextView(String fingerId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < forTextViewNoLastDate.getChildCount(); i++) {
                    View view = forTextViewNoLastDate.getChildAt(i);
                    if (view instanceof TextView && fingerId.equals(view.getTag())) {
                        forTextViewNoLastDate.removeView(view);
                        // تنظيف EditText و TextView التي تعرض الاسم
                        if (idEditText.getText().toString().equals(fingerId)) {
                            idEditText.setText("");
                            nameTextView.setText("");

                        }
                        updateTotalTextView();
                        break;
                    }
                }
            }
        });
    }

    private static class Kid {
        private String kidName;
        private String fingerId;

        public Kid(String kidName, String fingerId) {
            this.kidName = kidName;
            this.fingerId = fingerId;
        }

        public String getKidName() {
            return kidName;
        }

        public String getFingerId() {
            return fingerId;
        }
    }
}

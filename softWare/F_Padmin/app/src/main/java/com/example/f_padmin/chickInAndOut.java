package com.example.f_padmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class chickInAndOut extends AppCompatActivity {
    private String mainCollection = "1";

    private TextView dayEditText, monthEditText, yearEditText;
    private TextView nameOfDayTextView;
    private LinearLayout linearLayoutHorizontal;
    private Calendar calendar;

    @Override
    protected void onStart() {
        super.onStart();
        updateDayName();
        updateDateFields();
        fetchDataFromFirebase();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chick_in_and_out);

        // تهيئة الواجهات
        dayEditText = findViewById(R.id.day);
        monthEditText = findViewById(R.id.mounth);
        yearEditText = findViewById(R.id.year);
        nameOfDayTextView = findViewById(R.id.nameOfDay);
        linearLayoutHorizontal = findViewById(R.id.linearLayoutHorizontal);

        Button buttonPlus = findViewById(R.id.buttonPlus);
        Button buttonMinus = findViewById(R.id.buttonMinus);
        Button buttonPlusMonth = findViewById(R.id.buttonPlusMounth);
        Button buttonMinusMonth = findViewById(R.id.buttonMinusMounth);

        // تعبئة الحقول بتاريخ اليوم الحالي
        calendar = Calendar.getInstance();
        updateDateFields();

        // تحديث اسم اليوم
        updateDayName();

        // مستمعي الضغط للأزرار
        buttonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                updateDateFields();
                updateDayName();
                fetchDataFromFirebase();
            }
        });

        buttonMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                updateDateFields();
                updateDayName();
                fetchDataFromFirebase();
            }
        });

        buttonPlusMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.MONTH, 1);
                updateDateFields();
                updateDayName();
                fetchDataFromFirebase();
            }
        });

        buttonMinusMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.MONTH, -1);
                updateDateFields();
                updateDayName();
                fetchDataFromFirebase();
            }
        });
    }

    private void updateDateFields() {
        dayEditText.setText(String.format(Locale.ENGLISH, "%02d", calendar.get(Calendar.DAY_OF_MONTH)));
        monthEditText.setText(String.format(Locale.ENGLISH, "%02d", calendar.get(Calendar.MONTH) + 1));
        yearEditText.setText(String.format(Locale.ENGLISH, "%04d", calendar.get(Calendar.YEAR)));
    }

    private void updateDayName() {
        String dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        nameOfDayTextView.setText(dayName);
    }

    private void fetchDataFromFirebase() {
        String year = yearEditText.getText().toString().trim();
        String month = monthEditText.getText().toString().trim();
        String day = dayEditText.getText().toString().trim();

        DatabaseReference dateReference = FirebaseDatabase.getInstance().getReference(mainCollection)
                .child("dateData").child(year).child(month).child(day);

        dateReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<StudentData> studentDataList = new ArrayList<>();

                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    String name = studentSnapshot.getKey();
                    String checkIn = studentSnapshot.child("checkIn").getValue(String.class);
                    String checkOut = studentSnapshot.child("checkOut").getValue(String.class);

                    if (name != null && checkIn != null) {
                        studentDataList.add(new StudentData(name, checkIn, checkOut));
                    }
                }

                // فرز القائمة حسب الاسماء
                Collections.sort(studentDataList, new Comparator<StudentData>() {
                    @Override
                    public int compare(StudentData o1, StudentData o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });

                // تحديث الواجهة مع البيانات المفرزة
                displayData(studentDataList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(chickInAndOut.this, "فشل في جلب البيانات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayData(ArrayList<StudentData> studentDataList) {
        // مسح التخطيط الأفقي لعرض البيانات الجديدة
        linearLayoutHorizontal.removeAllViews();

        // إعداد تنسيق الوقت
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        for (StudentData studentData : studentDataList) {
            // حساب الفارق الزمني إذا كان وقت الانصراف موجودًا
            String totalTime = "";
            try {
                if (studentData.getCheckOut() != null) {
                    Date checkInTime = format.parse(studentData.getCheckIn());
                    Date checkOutTime = format.parse(studentData.getCheckOut());

                    long difference = checkOutTime.getTime() - checkInTime.getTime();
                    long diffHours = difference / (60 * 60 * 1000) % 24;
                    long diffMinutes = difference / (60 * 1000) % 60;
                    long diffSeconds = difference / 1000 % 60;

                    totalTime = String.format(Locale.ENGLISH, "%02d:%02d:%02d", diffHours, diffMinutes, diffSeconds);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // إعداد التخطيط العمودي لكل طالب
            LinearLayout verticalLayout = new LinearLayout(this);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);
            verticalLayout.setBackgroundColor(Color.YELLOW);

            // إعداد LayoutParams ل LinearLayout مع إضافة مسافة بين كل `LinearLayout`
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            layoutParams.setMargins(10, 0, 10, 0); // إضافة مسافة 10dp على اليمين واليسار بين `LinearLayout`
            verticalLayout.setLayoutParams(layoutParams);

            // تقسيم الاسم الرباعي إلى سطرين في نفس `TextView`
            String fullName = studentData.getName();
            String nameWithNewLine = fullName;

            // إذا كان الاسم يحتوي على 4 كلمات على الأقل، نضيف سطر جديد بين الكلمتين الثانية والثالثة
            String[] nameParts = fullName.split(" ");
            if (nameParts.length >= 4) {
                nameWithNewLine = nameParts[0] + " " + nameParts[1] + "\n" + nameParts[2] + " " + nameParts[3];
            }

            // إعداد TextView لعرض الاسم مع سطر جديد
            TextView nameTextView = new TextView(this);
            nameTextView.setText(nameWithNewLine);
            nameTextView.setTextSize(17);
            nameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0, // الارتفاع صفر لتفعيل الوزن
                    1 // الوزن يساوي 1 لتوزيع المساحة بالتساوي
            ));

            // إعداد TextView لوقت الحضور
            TextView checkInTextView = new TextView(this);
            checkInTextView.setText(studentData.getCheckIn());
            checkInTextView.setTextSize(17);
            checkInTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0, // الارتفاع صفر لتفعيل الوزن
                    1 // الوزن يساوي 1 لتوزيع المساحة بالتساوي
            ));

            // إضافة TextView لوقت الانصراف فقط إذا كان موجودًا
            TextView checkOutTextView = null;
            if (studentData.getCheckOut() != null) {
                checkOutTextView = new TextView(this);
                checkOutTextView.setText(studentData.getCheckOut());
                checkOutTextView.setTextSize(17);
                checkOutTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0, // الارتفاع صفر لتفعيل الوزن
                        1 // الوزن يساوي 1 لتوزيع المساحة بالتساوي
                ));
            }

            // إضافة TextView لفترة التواجد فقط إذا كان وقت الانصراف موجودًا
            TextView totalTimeTextView = null;
            if (studentData.getCheckOut() != null) {
                totalTimeTextView = new TextView(this);
                totalTimeTextView.setText(totalTime);
                totalTimeTextView.setTextSize(17);
                totalTimeTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0, // الارتفاع صفر لتفعيل الوزن
                        1 // الوزن يساوي 1 لتوزيع المساحة بالتساوي
                ));
            }

            // إضافة الـ TextViews إلى التخطيط العمودي
            verticalLayout.addView(nameTextView);
            verticalLayout.addView(checkInTextView);
            if (checkOutTextView != null) verticalLayout.addView(checkOutTextView);
            if (totalTimeTextView != null) verticalLayout.addView(totalTimeTextView);

            // إضافة التخطيط العمودي إلى التخطيط الأفقي
            linearLayoutHorizontal.addView(verticalLayout);
        }
    }

    // فئة لتخزين بيانات الطالب
    private static class StudentData {
        private String name;
        private String checkIn;
        private String checkOut;

        public StudentData(String name, String checkIn, String checkOut) {
            this.name = name;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
        }

        public String getName() {
            return name;
        }

        public String getCheckIn() {
            return checkIn;
        }

        public String getCheckOut() {
            return checkOut;
        }
    }
}

package com.example.f_padmin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class underReviewStudens extends AppCompatActivity {
    private String mainCollection = "1";


    private LinearLayout detailsContainer;
    private TextView totalCountText;
    private DatabaseReference underReviewRef;
    private DatabaseReference usersRef;
    private DatabaseReference currentStudentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_under_review_studens);


        detailsContainer = findViewById(R.id.details_container);
        totalCountText = findViewById(R.id.total_count);

        underReviewRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("UnderReview");
        usersRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("users");
        currentStudentsRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("currentStudents");

        loadStudents();
    }

    private void loadStudents() {
        underReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    count++;
                    String entryId = studentSnapshot.getKey();
                    Map<String, Object> data = (Map<String, Object>) studentSnapshot.getValue();

                    TextView textView = new TextView(underReviewStudens.this);
                    String studentName = data.get("studentName") != null ? data.get("studentName").toString() : "اسم غير معروف";
//                    String studententryId = data.get(entryId) != null ? data.get("studentNID").toString() : "بدون رقم";
                    textView.setText(studentName + " - " + entryId);
                    textView.setTextSize(20);
                    textView.setBackgroundColor(Color.YELLOW);
                    textView.setGravity(Gravity.CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    int margin = (int) (10 * getResources().getDisplayMetrics().density);
                    params.setMargins(margin, margin, margin, margin);
                    textView.setLayoutParams(params);

                    LinearLayout studentLayout = new LinearLayout(underReviewStudens.this);
                    studentLayout.setOrientation(LinearLayout.VERTICAL);
                    studentLayout.setBackgroundColor(Color.LTGRAY);
                    studentLayout.setPadding(10, 10, 10, 10);
                    studentLayout.setVisibility(View.GONE);

                    textView.setOnClickListener(v -> {
                        if (studentLayout.getVisibility() == View.GONE) {
                            studentLayout.setVisibility(View.VISIBLE);
                        } else {
                            studentLayout.setVisibility(View.GONE);
                        }
                    });

                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue() != null ? entry.getValue().toString() : "غير متوفر";

                        if (key.toLowerCase().contains("photo")) {
                            TextView label = new TextView(underReviewStudens.this);
                            label.setText(key);
                            label.setGravity(Gravity.CENTER);
                            label.setTextSize(16);

                            ImageView imageView = new ImageView(underReviewStudens.this);
                            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    400
                            );
                            imageParams.setMargins(0, 20, 0, 20);
                            imageView.setLayoutParams(imageParams);
                            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                            Glide.with(underReviewStudens.this).load(value).into(imageView);

                            imageView.setOnClickListener(v -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(underReviewStudens.this);

                                LinearLayout layout = new LinearLayout(underReviewStudens.this);
                                layout.setBackgroundColor(Color.BLACK);
                                layout.setPadding(20, 20, 20, 20);
                                layout.setGravity(Gravity.CENTER);

                                ImageView fullImageView = new ImageView(underReviewStudens.this);
                                fullImageView.setAdjustViewBounds(true);
                                fullImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                                Glide.with(underReviewStudens.this).load(value).into(fullImageView);

                                layout.addView(fullImageView,
                                        new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                );

                                builder.setView(layout);
                                builder.setPositiveButton("إغلاق", (dialog, which) -> dialog.dismiss());
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            });


                            studentLayout.addView(label);
                            studentLayout.addView(imageView);
                        } else {
                            TextView detail = new TextView(underReviewStudens.this);
                            detail.setText(key + ": " + value);
                            detail.setTextSize(16);
                            detail.setGravity(Gravity.CENTER);

                            if (key.toLowerCase().contains("phone")) {
                                detail.setTextColor(Color.BLUE);
                                detail.setOnClickListener(v -> {
                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                    intent.setData(Uri.parse("tel:" + value));
                                    startActivity(intent);
                                });
                            }

                            studentLayout.addView(detail);
                        }
                    }



                    Button acceptBtn = new Button(underReviewStudens.this);
                    acceptBtn.setText("قبول");
                    acceptBtn.setOnClickListener(v -> confirmAction("تأكيد القبول؟", () -> acceptStudent(entryId, data)));

                    Button rejectBtn = new Button(underReviewStudens.this);
                    rejectBtn.setText("رفض");
                    rejectBtn.setOnClickListener(v -> confirmAction("تأكيد الرفض؟", () -> rejectStudent(entryId)));

                    studentLayout.addView(acceptBtn);
                    studentLayout.addView(rejectBtn);

                    detailsContainer.addView(textView);
                    detailsContainer.addView(studentLayout);
                }
                totalCountText.setText("عدد الطلبات: " + count);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "Error loading data: " + error.getMessage());
            }
        });
    }

    private void confirmAction(String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("نعم", (dialog, which) -> onConfirm.run())
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void acceptStudent(String entryId, Map<String, Object> data) {
        // استخراج kidId
        String kidId = entryId;
//        if (!data.containsKey("studentName") || !data.containsKey("NationalID")) {
//            Toast.makeText(this, "البيانات ناقصة ولا يمكن قبول الطالب", Toast.LENGTH_SHORT).show();
//            return;
//        }

        String studentName = data.get("studentName").toString();
        String nationalID = data.get("NationalID").toString();

        // استخراج اسم الأب من اسم الطالب
        final String[] fatherName = {""};

        String[] nameParts = studentName.split(" ");
        if (nameParts.length > 1) {
            for (int i = 1; i < nameParts.length; i++) {
                fatherName[0] += nameParts[i] + " ";
            }
            fatherName[0] = fatherName[0].trim();
        }


        // تحميل كل أرقام البصمات الموجودة أولًا
        FirebaseDatabase.getInstance().getReference("fengerModule").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<Integer> existingFingerIds = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Integer fid = snap.child("fingerId").getValue(Integer.class);
                    if (fid != null) existingFingerIds.add(fid);
                }

                // إيجاد أول رقم بصمة متاح
                int nextFingerId = findNextAvailableFingerId(existingFingerIds);

                // حفظ البيانات في نفس المسارات
                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                // data/kidId
                DatabaseReference dataRef = rootRef.child("data").child(kidId);
                dataRef.child("isInClass").setValue(false);
                dataRef.child("kidId").setValue(kidId);
                dataRef.child("fingerId").setValue(nextFingerId);
                dataRef.child("kidName").setValue(studentName);
                dataRef.child("NationalID").setValue(nationalID);
                dataRef.child("fatherName").setValue(fatherName);

                if (data.containsKey("motherPhone"))
                    dataRef.child("phoneMother").setValue(data.get("motherPhone").toString());
                if (data.containsKey("fatherPhone"))
                    dataRef.child("phoneFather").setValue(data.get("fatherPhone").toString());
                if (data.containsKey("emergencyPhone"))
                    dataRef.child("phone3").setValue(data.get("emergencyPhone").toString());

                // users/nationalID
                DatabaseReference usersRef = rootRef.child("users").child(nationalID);
                usersRef.child("id").setValue(nationalID);
                usersRef.child("name").setValue(studentName);
                usersRef.child("KidId").setValue(kidId);
                usersRef.child("isAccepted").setValue(true);

                // fengerModule/fingerId
                DatabaseReference fingerRef = rootRef.child("fengerModule").child(String.valueOf(nextFingerId));
                fingerRef.child("fingerId").setValue(nextFingerId);
                fingerRef.child("kidId").setValue(kidId);

                // names/studentName
                rootRef.child("names").child(studentName).setValue(String.valueOf(nextFingerId));

                // حذف الطالب من UnderReview
                underReviewRef.child(entryId).removeValue();

                // إعادة تحميل الصفحة
                recreate();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(underReviewStudens.this, "فشل في تحميل أرقام البصمات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int findNextAvailableFingerId(ArrayList<Integer> existingFingerIds) {
        if (existingFingerIds.isEmpty()) return 1;

        Collections.sort(existingFingerIds);
        int next = 1;
        for (int id : existingFingerIds) {
            if (id != next) return next;
            next++;
        }
        return existingFingerIds.get(existingFingerIds.size() - 1) + 1;
    }



    private void rejectStudent(String entryId) {
        underReviewRef.child(entryId).removeValue();
        recreate();
    }
}
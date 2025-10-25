package com.example.f_padmin;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CurrentStudents extends AppCompatActivity {

    private String mainCollection = "1";

    private LinearLayout container;
    private DatabaseReference currentStudentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_students);

        container = findViewById(R.id.details_container);
        currentStudentsRef = FirebaseDatabase.getInstance().getReference().child(mainCollection).child("currentStudents");

//        currentStudentsRef = FirebaseDatabase.getInstance().getReference("currentStudents");

        loadStudents();
    }

    private void loadStudents() {
        currentStudentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String kidId = studentSnapshot.getKey();
                    String kidName = studentSnapshot.child("kidName").getValue(String.class);

                    TextView textView = new TextView(CurrentStudents.this);
                    textView.setText(kidName);
                    textView.setTextSize(20);
                    textView.setPadding(20, 20, 20, 20);
                    textView.setBackgroundColor(Color.LTGRAY);

                    textView.setOnClickListener(v -> showStudentDialog(studentSnapshot));

                    container.addView(textView);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "فشل تحميل الطلبة: " + error.getMessage());
            }
        });
    }

    private void showStudentDialog(DataSnapshot snapshot) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.student_form_a4);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // ربط العناصر بالـ IDs
        ((TextView) dialog.findViewById(R.id.student_name)).setText(getValue(snapshot, "kidName"));
        ((TextView) dialog.findViewById(R.id.student_national_id)).setText(getValue(snapshot, "NationalID"));
        ((TextView) dialog.findViewById(R.id.father_name)).setText(getValue(snapshot, "fatherName"));
        ((TextView) dialog.findViewById(R.id.father_job)).setText(getValue(snapshot, "fatherJob"));
        ((TextView) dialog.findViewById(R.id.father_phone)).setText(getValue(snapshot, "phoneFather"));
        ((TextView) dialog.findViewById(R.id.motherName)).setText(getValue(snapshot, "motherName"));
        ((TextView) dialog.findViewById(R.id.mother_job)).setText(getValue(snapshot, "motherJob"));
        ((TextView) dialog.findViewById(R.id.mother_phone)).setText(getValue(snapshot, "phoneMother"));
        ((TextView) dialog.findViewById(R.id.address)).setText(getValue(snapshot, "location"));
        ((TextView) dialog.findViewById(R.id.code)).setText(getValue(snapshot, "kidId"));
        ((TextView) dialog.findViewById(R.id.level)).setText(getValue(snapshot, "level"));
        ((TextView) dialog.findViewById(R.id.date)).setText(getValue(snapshot, "registerDate"));
        ((TextView) dialog.findViewById(R.id.parent_goal)).setText(getValue(snapshot, "parentGoal"));
        ((TextView) dialog.findViewById(R.id.how_know_us)).setText(getValue(snapshot, "howKnowUs"));
        ((TextView) dialog.findViewById(R.id.education_stage)).setText(getValue(snapshot, "eduStage"));
        ((TextView) dialog.findViewById(R.id.student_age)).setText(getValue(snapshot, "age"));

        ImageView studentPhoto = dialog.findViewById(R.id.studentPhoto);

        String photoURL = getValue(snapshot, "studentPhotoURL");
        if (!photoURL.equals("—")) {
            Glide.with(this)
                    .load(photoURL)
                    .into(studentPhoto);
        }

        dialog.show();
    }

    private String getValue(DataSnapshot snapshot, String key) {
        if (snapshot.hasChild(key)) {
            Object value = snapshot.child(key).getValue();
            return value != null ? value.toString() : "—";
        } else {
            return "—";
        }
    }
}

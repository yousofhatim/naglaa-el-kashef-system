package com.example.f_padmin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class newQuestions extends AppCompatActivity {
    private String mainCollection = "1";

    private LinearLayout container;
    private DatabaseReference questionsRef;
    private DatabaseReference promptRef;
    private static final String TAG = "NewQuestionsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_questions);

        container = findViewById(R.id.questions_container);
        questionsRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("NewQuestions");
        promptRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("Prompt").child("apiPrompt");

        loadQuestions();
    }

    private void loadQuestions() {
        questionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                container.removeAllViews();

                for (DataSnapshot questionSnap : snapshot.getChildren()) {
                    String questionKey = questionSnap.getKey();
                    String questionText = questionSnap.getValue(String.class);

                    Log.d(TAG, "جاري تحميل السؤال: " + questionText);

                    if (questionText != null && questionText.length() > 0) {
                        addQuestionView(questionKey, questionText);
                    } else {
                        Log.w(TAG, "تم تجاهل سؤال null أو فاضي. المفتاح: " + questionKey);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(newQuestions.this, "فشل تحميل الاستفسارات", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "فشل قراءة الأسئلة: " + error.getMessage());
            }
        });
    }

    private void addQuestionView(String key, String question) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundColor(0xFFEDEDED);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 30);
        card.setLayoutParams(params);

        TextView questionView = new TextView(this);
        questionView.setText("سؤال: " + question);
        questionView.setTextSize(18);
        questionView.setPadding(0, 0, 0, 10);

        EditText answerInput = new EditText(this);
        answerInput.setHint("اكتب الرد هنا");

        Button answerBtn = new Button(this);
        answerBtn.setText("رد");
        answerBtn.setOnClickListener(v -> {
            String answer = answerInput.getText().toString().trim();
            if (answer.isEmpty()) {
                Toast.makeText(this, "اكتب رد الأول", Toast.LENGTH_SHORT).show();
                return;
            }

            addReplyToPrompt(question, answer); // تحديث البرومبت
            questionsRef.child(key).removeValue(); // حذف السؤال
            container.removeView(card); // حذف من الواجهة
        });

        Button deleteBtn = new Button(this);
        deleteBtn.setText("حذف");
        deleteBtn.setOnClickListener(v -> {
            questionsRef.child(key).removeValue(); // حذف السؤال
            container.removeView(card); // حذف من الواجهة
        });

        card.addView(questionView);
        card.addView(answerInput);
        card.addView(answerBtn);
        card.addView(deleteBtn);

        container.addView(card);
    }

    private void addReplyToPrompt(String question, String answer) {
        promptRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String oldPrompt = snapshot.getValue(String.class);
                if (oldPrompt == null) oldPrompt = "";

                // نضيف الرد مباشرة بعد السؤال في نفس السطر
                String formatted = question.trim();
                if (!formatted.endsWith("؟") && !formatted.endsWith("?")) {
                    formatted += "؟";
                }
                String updated = oldPrompt + "\n\n" + formatted + " " + answer;

                promptRef.setValue(updated).addOnSuccessListener(unused -> {
                    Toast.makeText(newQuestions.this, "تم إضافة الرد للبرومبت", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(newQuestions.this, "فشل تحديث البرومبت", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(newQuestions.this, "حصلت مشكلة أثناء تحديث البرومبت", Toast.LENGTH_SHORT).show();
            }
        });
    }

}

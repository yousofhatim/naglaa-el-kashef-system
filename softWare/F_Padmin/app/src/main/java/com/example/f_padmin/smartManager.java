package com.example.f_padmin;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class smartManager extends AppCompatActivity {
    private String mainCollection = "1";

    private EditText promptEditText;
    private Button saveBtn;
    private DatabaseReference promptRef;
    private String currentPrompt = "";
    private Switch autoChatSwitch;
    private DatabaseReference autoChatRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_manager);

        promptEditText = findViewById(R.id.promptEditText);
        saveBtn = findViewById(R.id.saveBtn);

        promptRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("Prompt").child("apiPrompt");

        autoChatSwitch = findViewById(R.id.autoChatSwitch);
        autoChatRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("autoChatOn");

// قراءة حالة البوت
        autoChatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean isOn = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    autoChatSwitch.setChecked(isOn);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(smartManager.this, "فشل في قراءة حالة البوت", Toast.LENGTH_SHORT).show();
            }
        });

// عند التغيير في الزر
        autoChatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoChatRef.setValue(isChecked)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, isChecked ? "✅ تم تشغيل البوت" : "⛔ تم إيقاف البوت", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "فشل في تحديث حالة البوت", Toast.LENGTH_SHORT).show();
                    });
        });


        // قراءة البيانات من Firebase
        promptRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentPrompt = snapshot.getValue(String.class);
                    promptEditText.setText(currentPrompt);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(smartManager.this, "فشل في تحميل البيانات", Toast.LENGTH_SHORT).show();
            }
        });

        // عند الضغط على زر الحفظ
        saveBtn.setOnClickListener(v -> {
            savePrompt();
        });
    }

    private void savePrompt() {
        String newPrompt = promptEditText.getText().toString().trim();
        if (!newPrompt.equals(currentPrompt)) {
            promptRef.setValue(newPrompt)
                    .addOnSuccessListener(unused -> {
                        currentPrompt = newPrompt;
                        Toast.makeText(this, "تم حفظ التعديل", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "فشل في الحفظ", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePrompt(); // حفظ تلقائي عند الخروج
    }
}

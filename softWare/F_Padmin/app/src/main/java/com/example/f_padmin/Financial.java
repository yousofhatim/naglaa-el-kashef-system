package com.example.f_padmin;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Financial extends AppCompatActivity {
    private String mainCollection = "1";

    private TextView balanceTextView;
    private DatabaseReference balanceRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financial);

        // تهيئة العناصر
        balanceTextView = findViewById(R.id.textView4);

        // الاتصال بفاير بيز
        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        balanceRef = database.getReference("Balance");
        balanceRef = database.getReference(mainCollection).child("Balance");

        // استقبال التحديثات
        balanceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long balance = dataSnapshot.getValue(Long.class); // اقرأ كـ Long

                if (balance != null) {
                    balanceTextView.setText("الرصيد الحالي: " + balance.toString()); // حول الـ Long إلى String
                } else {
                    balanceTextView.setText("الرصيد الحالي: 0");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                balanceTextView.setText("خطأ في الاتصال");
            }
        });
    }


}
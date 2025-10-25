package com.example.f_padmin;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class updatePeriod extends AppCompatActivity {
    private String mainCollection = "1";
    private LinearLayout forTextViewOutOfSystem;
    private LinearLayout subscribersLayout;
    private DatabaseReference databaseReference;
    private DatabaseReference balanceRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_period);

        forTextViewOutOfSystem = findViewById(R.id.forTextViewOutOfSystem);
        subscribersLayout = findViewById(R.id.Subscriberss);
        databaseReference = FirebaseDatabase.getInstance().getReference(mainCollection).child("data");
        balanceRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("Balance");

        checkAndUpdateUsers();
    }

    private void checkAndUpdateUsers() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                forTextViewOutOfSystem.removeAllViews();
                subscribersLayout.removeAllViews();

                TreeMap<String, Map.Entry<String, String>> expiredSubscribers = new TreeMap<>();
                TreeMap<String, Map.Entry<String, String>> activeSubscribers = new TreeMap<>();

                for (DataSnapshot keySnapshot : dataSnapshot.getChildren()) {
                    String userKey = keySnapshot.getKey();
                    String userEndPeriod = keySnapshot.child("userEndPeriod").getValue(String.class);
                    String kidName = keySnapshot.child("kidName").getValue(String.class);
                    String invoice = keySnapshot.child("invoice").getValue(String.class);

                    boolean isExpired = true;
                    if (userEndPeriod != null) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                            Calendar endDate = Calendar.getInstance();
                            endDate.setTime(sdf.parse(userEndPeriod));
                            isExpired = Calendar.getInstance().after(endDate);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (kidName != null) {
                        Map.Entry<String, String> entry = new java.util.AbstractMap.SimpleEntry<>(
                                userKey,
                                invoice
                        );

                        if (isExpired) {
                            expiredSubscribers.put(kidName, entry);
                        } else {
                            activeSubscribers.put(kidName, entry);
                        }
                    }
                }

                // إضافة المشتركين المنتهية اشتراكاتهم
                for (Map.Entry<String, Map.Entry<String, String>> entry : expiredSubscribers.entrySet()) {
                    addExpiredKidToLayout(
                            entry.getKey(),
                            entry.getValue().getKey(),
                            entry.getValue().getValue()
                    );
                }

                // إضافة المشتركين النشطين
                for (Map.Entry<String, Map.Entry<String, String>> entry : activeSubscribers.entrySet()) {
                    addActiveKidToLayout(
                            entry.getKey(),
                            entry.getValue().getKey(),
                            entry.getValue().getValue()
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void addExpiredKidToLayout(String kidName, String key, String invoice) {
        LinearLayout kidLayout = new LinearLayout(this);
        kidLayout.setOrientation(LinearLayout.HORIZONTAL);
        kidLayout.setBackgroundColor(Color.YELLOW);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(15, 15, 15, 15);
        kidLayout.setLayoutParams(layoutParams);

        TextView textView = new TextView(this);
        textView.setText(kidName);
        textView.setTextSize(18);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        EditText invoiceEdit = new EditText(this);
        invoiceEdit.setHint("المبلغ");
        invoiceEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        invoiceEdit.setText(invoice != null ? invoice : "");

        Button renewButton = new Button(this);
        renewButton.setText("تجديد");
        renewButton.setOnClickListener(v -> {
            String invoiceValue = invoiceEdit.getText().toString();
            showRenewDialog(key, kidLayout, invoiceValue);
        });

        kidLayout.addView(textView);
        kidLayout.addView(invoiceEdit);
        kidLayout.addView(renewButton);

        forTextViewOutOfSystem.addView(kidLayout);
    }

    private void addActiveKidToLayout(String kidName, String userKey, String invoice) {
        LinearLayout kidLayout = new LinearLayout(this);
        kidLayout.setOrientation(LinearLayout.HORIZONTAL);
        kidLayout.setBackgroundColor(Color.parseColor("#E8F5E9"));
        kidLayout.setOnClickListener(v -> showUserSubscriptions(userKey));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(15, 15, 15, 15);
        kidLayout.setLayoutParams(layoutParams);

        TextView nameTextView = new TextView(this);
        nameTextView.setText(kidName);
        nameTextView.setTextSize(18);
        nameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView statusTextView = new TextView(this);
        statusTextView.setText("نشط");
        statusTextView.setTextColor(Color.GREEN);
        statusTextView.setTextSize(16);

        kidLayout.addView(nameTextView);
        kidLayout.addView(statusTextView);

        subscribersLayout.addView(kidLayout);
    }

    private void showUserSubscriptions(String userKey) {
        DatabaseReference userSubscriptionsRef = databaseReference.child(userKey).child("Subscription_date");
        userSubscriptionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder subscriptions = new StringBuilder("سجل الاشتراكات:\n\n");

                // إذا لم توجد اشتراكات
                if(!dataSnapshot.exists()) {
                    subscriptions.append("لا يوجد سجل اشتراكات حتى الآن");
                }

                // جمع البيانات وتنسيقها
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String date = dateSnapshot.getKey();
                    String amount = dateSnapshot.getValue(String.class);

                    // إضافة التاريخ والمبلغ في نفس السطر مع تنسيق
                    subscriptions.append("📅 ")
                            .append(date)
                            .append("  -  💵 ")
                            .append(amount)
                            .append(" جنيه")
                            .append("\n\n");
                }

                new AlertDialog.Builder(updatePeriod.this)
                        .setTitle("تفاصيل الاشتراكات")
                        .setMessage(subscriptions.toString())
                        .setPositiveButton("تم", null)
                        .show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // معالجة الأخطاء
            }
        });
    }
    private void showRenewDialog(String key, LinearLayout kidLayout, String invoiceValue) {
        new AlertDialog.Builder(this)
                .setTitle("تجديد الاشتراك")
                .setMessage("هل تريد تجديد الاشتراك للعميل؟")
                .setPositiveButton("نعم", (dialog, which) -> {
                    renewSubscription(key, invoiceValue);
                    forTextViewOutOfSystem.removeView(kidLayout);
                })
                .setNegativeButton("لا", null)
                .show();
    }

    private void renewSubscription(String userKey, String invoiceValue) {
        // تحديث تاريخ الانتهاء
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        calendar.add(Calendar.MONTH, 1);
        String newEndPeriod = sdf.format(calendar.getTime());

        DatabaseReference userRef = databaseReference.child(userKey);
        userRef.child("userEndPeriod").setValue(newEndPeriod);
        userRef.child("invoice").setValue(invoiceValue);

        // إضافة لتاريخ الاشتراكات الخاص بالمستخدم
        if (!invoiceValue.isEmpty()) {
            try {
                String today = sdf.format(Calendar.getInstance().getTime());
                userRef.child("Subscription_date").child(today).setValue(invoiceValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // تحديث الرصيد العام
        if (!invoiceValue.isEmpty()) {
            try {
                int amount = Integer.parseInt(invoiceValue);
                balanceRef.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Integer currentBalance = mutableData.getValue(Integer.class);
                        if (currentBalance == null) currentBalance = 0;
                        mutableData.setValue(currentBalance + amount);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                        if (error != null) {
                            error.toException().printStackTrace();
                        }
                    }
                });
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}
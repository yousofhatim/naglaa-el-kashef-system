package com.example.f_padmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class payPeriod extends AppCompatActivity {
    private String mainCollection = "1";
    private TextView paymentMessage;
    private ProgressBar progressBar;
    private Button copyButton;
    private Button whatsappButton;
    private int totalKidsCount;
    private int payAmount;
    private String payNum;
    private String acceptNum;
    private DatabaseReference endPeriodReference;
    private boolean isActivityDestroyed = false;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_period);

        paymentMessage = findViewById(R.id.paymentMessage);
        progressBar = findViewById(R.id.progressBar);
        copyButton = findViewById(R.id.copyButton);
        whatsappButton = findViewById(R.id.whatsappButton);

        // إخفاء محتويات الواجهة
        paymentMessage.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        copyButton.setVisibility(View.INVISIBLE);
        whatsappButton.setVisibility(View.INVISIBLE);

        // استرجاع بيانات الأطفال
        retrieveDataAndCalculateCounts();

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyPayNumberToClipboard();
            }
        });

        whatsappButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWhatsApp();
            }
        });

        // مراقبة التغييرات في تاريخ نهاية الفترة
        endPeriodReference = FirebaseDatabase.getInstance().getReference(mainCollection).child("endPeriod");
        endPeriodReference.addValueEventListener(endPeriodListener);
    }

    private void retrieveDataAndCalculateCounts() {
        DatabaseReference dataReference = FirebaseDatabase.getInstance().getReference(mainCollection).child("data");
        dataReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int regularCount = 0;
                int irregularCount = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String lastDateStr = snapshot.child("lastDate").getValue(String.class);

                    if (lastDateStr != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        try {
                            Date lastDate = dateFormat.parse(lastDateStr);
                            Date currentDate = Calendar.getInstance().getTime();

                            long diffInMillis = currentDate.getTime() - lastDate.getTime();
                            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                            if (diffInDays < 7) {
                                regularCount++;
                            } else if (diffInDays < 25) {
                                irregularCount++;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                totalKidsCount = regularCount + irregularCount;
                retrieveAndCalculatePayAmount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // التعامل مع الخطأ
                progressBar.setVisibility(View.INVISIBLE);
                paymentMessage.setVisibility(View.VISIBLE);
                paymentMessage.setText("حدث خطأ في استرجاع البيانات. يرجى المحاولة مرة أخرى.");
            }
        });
    }

    private void retrieveAndCalculatePayAmount() {
        DatabaseReference priceReference = FirebaseDatabase.getInstance().getReference(mainCollection).child("price");
        priceReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String priceValue = dataSnapshot.getValue(String.class);

                if (priceValue != null) {
                    payAmount = totalKidsCount * Integer.parseInt(priceValue);
                    checkEndPeriodAndUpdateUI();
                } else {
                    // التعامل مع الحالة التي يكون فيها سعر الاشتراك غير متاح
                    progressBar.setVisibility(View.INVISIBLE);
                    paymentMessage.setVisibility(View.VISIBLE);
                    paymentMessage.setText("سعر الاشتراك غير متاح.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // التعامل مع الخطأ
                progressBar.setVisibility(View.INVISIBLE);
                paymentMessage.setVisibility(View.VISIBLE);
                paymentMessage.setText("حدث خطأ في استرجاع سعر الاشتراك. يرجى المحاولة مرة أخرى.");
            }
        });
    }

    private void checkEndPeriodAndUpdateUI() {
        endPeriodReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String endPeriodValue = dataSnapshot.getValue(String.class);

                if (endPeriodValue != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    try {
                        Date endDate = dateFormat.parse(endPeriodValue);
                        Date currentDate = Calendar.getInstance().getTime();

                        if (endDate != null && endDate.before(currentDate)) {
                            if (payAmount == 0) {
                                navigateToMainActivity();
                            } else {
                                displayPaymentMessage();
                            }
                        } else {
                            navigateToMainActivity();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        showError("حدث خطأ في تحليل التاريخ. يرجى المحاولة مرة أخرى.");
                    }
                } else {
                    showError("تاريخ نهاية الفترة غير متاح.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showError("حدث خطأ في استرجاع تاريخ نهاية الفترة. يرجى المحاولة مرة أخرى.");
            }
        });
    }

    private void displayPaymentMessage() {
        // استرجاع رقم الدفع من قاعدة البيانات
        DatabaseReference payNumRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("payNum");
        payNumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                payNum = dataSnapshot.getValue(String.class);
                progressBar.setVisibility(View.INVISIBLE);
                paymentMessage.setVisibility(View.VISIBLE);
                copyButton.setVisibility(View.VISIBLE);
                whatsappButton.setVisibility(View.VISIBLE);
                if (payNum != null) {
                    paymentMessage.setText("يرجى دفع " + payAmount + " على الرقم " + payNum);
                    retrieveAcceptNum();  // استرجاع acceptNum بعد استرجاع payNum
                } else {
                    paymentMessage.setText("يرجى دفع " + payAmount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showError("حدث خطأ في استرجاع رقم الدفع. يرجى المحاولة مرة أخرى.");
            }
        });
    }

    private void retrieveAcceptNum() {
        // استرجاع رقم acceptNum من قاعدة البيانات
        DatabaseReference acceptNumRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("acceptNum");
        acceptNumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                acceptNum = dataSnapshot.getValue(String.class);
                if (acceptNum == null) {
                    Toast.makeText(payPeriod.this, "رقم الاتصال غير متاح", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(payPeriod.this, "حدث خطأ في استرجاع رقم الاتصال. يرجى المحاولة مرة أخرى.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMainActivity() {
        if (!isActivityDestroyed && !hasNavigated) {
            hasNavigated = true;
            Intent mainIntent = new Intent(payPeriod.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.INVISIBLE);
        paymentMessage.setVisibility(View.VISIBLE);
        paymentMessage.setText(message);
    }

    private void copyPayNumberToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("payNumber", payNum);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "تم نسخ الرقم إلى الحافظة", Toast.LENGTH_SHORT).show();
    }

    private void openWhatsApp() {
        if (acceptNum != null) {
            try {
                String url = "https://api.whatsapp.com/send?phone=" + acceptNum;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "واتساب غير مثبت", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "رقم الاتصال غير متاح", Toast.LENGTH_SHORT).show();
        }
    }

    // مستمع لتاريخ نهاية الفترة
    private final ValueEventListener endPeriodListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            String endPeriodValue = dataSnapshot.getValue(String.class);
            if (endPeriodValue != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date endDate = dateFormat.parse(endPeriodValue);
                    Date currentDate = Calendar.getInstance().getTime();

                    if (endDate != null && !endDate.before(currentDate)) {
                        navigateToMainActivity();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            showError("حدث خطأ في استرجاع تاريخ نهاية الفترة. يرجى المحاولة مرة أخرى.");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (endPeriodReference != null) {
            endPeriodReference.removeEventListener(endPeriodListener);
        }
        isActivityDestroyed = true;
        hasNavigated = false;
    }
}

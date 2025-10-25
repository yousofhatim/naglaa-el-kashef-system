package com.example.f_padmin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class InvoicesActivity extends AppCompatActivity {
    private String mainCollection = "1";

    private LinearLayout containerLayout;
    private TextView statsText;

    int totalStudents = 0;
    int totalInvoices = 0;

    private static class Student {
        String key;
        String name;
        String invoice;
        String payNum;

        Student(String key, String name, String invoice, String payNum) {
            this.key = key;
            this.name = name;
            this.invoice = invoice;
            this.payNum = payNum;
        }
    }

    private GradientDrawable createRoundedDrawable(String hexColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(hexColor));
        drawable.setCornerRadius(radius);
        return drawable;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#121212"));

        statsText = new TextView(this);
        statsText.setText("جاري التحميل...");
        statsText.setTextSize(20);
        statsText.setTextColor(Color.WHITE);
        statsText.setPadding(20, 40, 20, 40);
        statsText.setGravity(Gravity.CENTER);
        statsText.setBackgroundColor(Color.parseColor("#1F1F1F"));

        mainLayout.addView(statsText);

        ScrollView scrollView = new ScrollView(this);
        containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(containerLayout);
        mainLayout.addView(scrollView);

        setContentView(mainLayout);

        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("data");

        dataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                containerLayout.removeAllViews();
                totalStudents = 0;
                totalInvoices = 0;

                HashMap<String, List<Student>> groupedByInvoice = new HashMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    String kidName = String.valueOf(child.child("kidName").getValue());

                    String invoiceStrRaw = child.child("invoice").getValue() != null
                            ? String.valueOf(child.child("invoice").getValue()) : "";
                    String invoiceStr = parseOrZero(invoiceStrRaw);

                    String payNumStr = child.child("payNum").getValue() != null
                            ? String.valueOf(child.child("payNum").getValue()) : "";

                    groupedByInvoice.computeIfAbsent(invoiceStr, k -> new ArrayList<>())
                            .add(new Student(key, kidName, invoiceStr, payNumStr));
                }


                List<String> invoiceKeys = new ArrayList<>(groupedByInvoice.keySet());
                Collections.sort(invoiceKeys, (a, b) -> Integer.parseInt(b) - Integer.parseInt(a));

                HorizontalScrollView groupsScroll = new HorizontalScrollView(InvoicesActivity.this);
                LinearLayout groupsLayout = new LinearLayout(InvoicesActivity.this);
                groupsLayout.setOrientation(LinearLayout.HORIZONTAL);
                groupsScroll.addView(groupsLayout);

                containerLayout.addView(groupsScroll);

                for (String invoiceAmount : invoiceKeys) {
                    List<Student> group = groupedByInvoice.get(invoiceAmount);
                    group.sort(Comparator.comparing(s -> s.name)); // ترتيب أبجدي داخل كل مجموعة
                    int groupCount = group.size();

                    LinearLayout verticalGroup = new LinearLayout(InvoicesActivity.this);
                    verticalGroup.setOrientation(LinearLayout.VERTICAL);
                    verticalGroup.setPadding(20, 20, 20, 20);
                    verticalGroup.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    TextView sectionTitle = new TextView(InvoicesActivity.this);
                    sectionTitle.setText(invoiceAmount + " جنيه\nعدد: " + groupCount);
                    sectionTitle.setTextSize(16);
                    sectionTitle.setTypeface(null, Typeface.BOLD);
                    sectionTitle.setTextColor(Color.WHITE);
                    sectionTitle.setGravity(Gravity.CENTER);
                    sectionTitle.setPadding(10, 10, 10, 20);
                    verticalGroup.addView(sectionTitle);

                    for (Student s : group) {
                        int invoiceValue;
                        try {
                            invoiceValue = Integer.parseInt(s.invoice);
                        } catch (NumberFormatException e) {
                            invoiceValue = 0;
                        }

                        totalStudents++;
                        totalInvoices += invoiceValue;

                        LinearLayout studentCard = new LinearLayout(InvoicesActivity.this);
                        studentCard.setOrientation(LinearLayout.VERTICAL);
                        studentCard.setBackgroundColor(Color.parseColor("#2B2F3A"));
                        studentCard.setPadding(30, 30, 30, 30);
                        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        blockParams.setMargins(10, 10, 10, 10);
                        studentCard.setLayoutParams(blockParams);

                        TextView nameView = new TextView(InvoicesActivity.this);
                        nameView.setText("الطالب: " + s.name);
                        nameView.setTextSize(18);
                        nameView.setTypeface(null, Typeface.BOLD);
                        nameView.setTextColor(Color.WHITE);
                        nameView.setPadding(0, 0, 0, 20);

                        View divider = new View(InvoicesActivity.this);
                        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                        divider.setBackgroundColor(Color.GRAY);

                        LinearLayout rowLayout = new LinearLayout(InvoicesActivity.this);
                        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                        rowLayout.setPadding(0, 20, 0, 0);

                        EditText invoiceEdit = new EditText(InvoicesActivity.this);
                        invoiceEdit.setHint("المبلغ");
                        invoiceEdit.setText(s.invoice);
                        invoiceEdit.setTextSize(16);
                        invoiceEdit.setTextColor(Color.WHITE);
                        invoiceEdit.setHintTextColor(Color.LTGRAY);
                        invoiceEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
                        invoiceEdit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                        invoiceEdit.setBackground(createRoundedDrawable("#3A3F4B", 25));

                        EditText payNumEdit = new EditText(InvoicesActivity.this);
                        payNumEdit.setHint("رقم الدفع");
                        payNumEdit.setText(s.payNum);
                        payNumEdit.setTextSize(16);
                        payNumEdit.setTextColor(Color.WHITE);
                        payNumEdit.setHintTextColor(Color.LTGRAY);
                        payNumEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
                        LinearLayout.LayoutParams payParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                        payParams.setMarginStart(20);
                        payNumEdit.setLayoutParams(payParams);
                        payNumEdit.setBackground(createRoundedDrawable("#3A3F4B", 25));

                        Button saveBtn = new Button(InvoicesActivity.this);
                        saveBtn.setText("حفظ");
                        saveBtn.setTextSize(16);
                        saveBtn.setTextColor(Color.WHITE);
                        saveBtn.setBackground(createRoundedDrawable("#4CAF50", 25));
                        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        buttonParams.setMarginStart(20);
                        saveBtn.setLayoutParams(buttonParams);

                        saveBtn.setOnClickListener(v -> {
                            String newInvoice = invoiceEdit.getText().toString();
                            String newPayNum = payNumEdit.getText().toString();

                            DatabaseReference thisRef = dataRef.child(s.key);
                            thisRef.child("invoice").setValue(newInvoice);
                            thisRef.child("payNum").setValue(newPayNum);

                            Toast.makeText(InvoicesActivity.this, "تم الحفظ", Toast.LENGTH_SHORT).show();
                        });

                        rowLayout.addView(invoiceEdit);
                        rowLayout.addView(payNumEdit);
                        rowLayout.addView(saveBtn);

                        studentCard.addView(nameView);
                        studentCard.addView(divider);
                        studentCard.addView(rowLayout);
                        verticalGroup.addView(studentCard);
                    }

                    groupsLayout.addView(verticalGroup);
                }

                statsText.setText("عدد الطلبة: " + totalStudents + " | إجمالي المبالغ: " + totalInvoices + " جنيه");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(InvoicesActivity.this, "فشل في تحميل البيانات", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String parseOrZero(String val) {
        if (val == null || val.trim().isEmpty()) {
            return "0";
        }
        try {
            Integer.parseInt(val.trim());
            return val.trim();
        } catch (NumberFormatException e) {
            return "0";
        }
    }
    private int safeParseInt(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


}

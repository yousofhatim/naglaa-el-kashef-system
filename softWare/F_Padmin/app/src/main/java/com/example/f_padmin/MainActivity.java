package com.example.f_padmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private String mainCollection = "1";

    private Button GPT, YKcos, delegatess, userControl, financial;
    private LinearLayout subButtonContainer;
    private final Map<String, Class<?>> activityMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط العناصر
        GPT = findViewById(R.id.GPT);
        YKcos = findViewById(R.id.YKcos);
        delegatess = findViewById(R.id.delegatess);
        userControl = findViewById(R.id.userControl);
        financial = findViewById(R.id.financial);
        subButtonContainer = findViewById(R.id.subButtonContainer);

        // ربط أسماء الأزرار بالأنشطة
        activityMap.put("تشغيل أوتوماتيك", smartManager.class);
        activityMap.put("ثغرات المؤسسة", problems.class);
        activityMap.put("اسألة جديده", newQuestions.class);
        activityMap.put("تسجيل", addActivity.class);
//        activityMap.put("المحادثات", ChatActivity.class);
//        activityMap.put("إعدادات الواي فاي", WifiSettingsActivity.class);
//        activityMap.put("حظر المستخدمين", BlockUsersActivity.class);
//        activityMap.put("شروط الاستخدام", TermsActivity.class);
        activityMap.put("حالة المشتركين", statuActivity.class);
        activityMap.put("طلبات الاستلام", subscriberData.class);
        activityMap.put("الحضور والانصراف", chickInAndOut.class);
        activityMap.put("تقرير تفصيلي", report.class);
        activityMap.put("المتواجدون حاليا", totalInClass.class);
        activityMap.put("الشحن والتجديد", updatePeriod.class);
        activityMap.put("تعديل المستويات", editLevels.class);
        activityMap.put("إضافة محتوى", addContant.class);
        activityMap.put("طلبات الالتحاق", underReviewStudens.class);
        activityMap.put("التقارير المالية", CurrentStudents.class);
        activityMap.put("كل الطلبة في قاعدة البيانات", allStudents.class);


//        activityMap.put("المدفوعات", PaymentsActivity.class);
        activityMap.put("الفواتير", InvoicesActivity.class);
//        activityMap.put("التقارير المالية", FinancialReportsActivity.class);

        GPT.setOnClickListener(v -> {
            String[] options = {"تشغيل أوتوماتيك","طلبات الالتحاق" ,"ثغرات المؤسسة","اسألة جديده"};
            displaySubButtons(options);
        });

        YKcos.setOnClickListener(v -> {
            String[] options = {"تسجيل", "المحادثات", "إعدادات الواي فاي", "حظر المستخدمين", "شروط الاستخدام"};
            displaySubButtons(options);
        });

        delegatess.setOnClickListener(v -> {
            String[] options = {"حالة المشتركين", "طلبات الاستلام", "الحضور والانصراف", "تقرير تفصيلي", "المتواجدون حاليا","كل الطلبة في قاعدة البيانات"};
            displaySubButtons(options);
        });

        userControl.setOnClickListener(v -> {
            String[] options = {"الشحن والتجديد", "تعديل المستويات", "إضافة محتوى"};
            displaySubButtons(options);
        });

        financial.setOnClickListener(v -> {
            String[] options = {"المدفوعات", "الفواتير", "التقارير المالية"};
            displaySubButtons(options);
        });
    }

    private void displaySubButtons(String[] labels) {
        subButtonContainer.removeAllViews();

        for (String label : labels) {
            Button button = new Button(this);
            button.setText(label);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 16, 0, 16); // تباعد رأسي بين الأزرار
            button.setLayoutParams(params);

            button.setBackground(getDrawable(R.drawable.btn_secondary)); // زرار فرعي شكله مختلف
            button.setTextColor(getResources().getColor(android.R.color.black));
            button.setTextSize(16);

            button.setOnClickListener(v -> {
                if (activityMap.containsKey(label)) {
                    Intent intent = new Intent(this, activityMap.get(label));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "لا يوجد شاشة مخصصة لـ " + label, Toast.LENGTH_SHORT).show();
                }
            });

            subButtonContainer.addView(button);
        }
    }
}

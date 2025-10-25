package com.example.f_puser;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class levels extends AppCompatActivity {
    private String mainCollection = "1";


    private LinearLayout levelButtonsContainer, detailContainer;
    private TextView levelTitle, levelGoals;
    private View currentSelected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_levels);

//        levelButtonsContainer = findViewById(R.id.levelButtonsContainer);
//        detailContainer = findViewById(R.id.detailContainer);
//        levelTitle = findViewById(R.id.levelTitle);
//        levelGoals = findViewById(R.id.levelGoals);
//
//        // إنشاء الدوائر
//        for (int i = 26; i >= 1; i--) {
//            final int level = i;
//
//            View circle = new View(this);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
//            params.setMargins(0, 12, 0, 12);
//            circle.setLayoutParams(params);
//            circle.setBackgroundResource(R.drawable.circle_shape); // شكل دائري غامق
//
//            circle.setOnClickListener(v -> {
//                // تكبير الدائرة
//                if (currentSelected != null) {
//                    currentSelected.setScaleX(1f);
//                    currentSelected.setScaleY(1f);
//                }
//
//                v.setScaleX(1.3f);
//                v.setScaleY(1.3f);
//                currentSelected = v;
//
//                // عرض البيانات
//                levelTitle.setText("المستوى " + level);
//                levelGoals.setText(getDummyData(level));
//            });
//
//            levelButtonsContainer.addView(circle);
//        }
//    }
//
//    private String getDummyData(int level) {
//        return "- كلمات إنجليزية جديدة\n"
//                + "- نطق الحروف\n"
//                + "- قراءة جملة بسيطة\n"
//                + "- نشاط تفاعلي لمستوى " + level;
    }
}

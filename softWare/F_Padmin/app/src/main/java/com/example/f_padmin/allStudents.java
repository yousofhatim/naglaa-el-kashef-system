package com.example.f_padmin;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class allStudents extends AppCompatActivity {

    private String mainCollection = "1";
    private LinearLayout mainLayout;
    private LinearLayout containerLayout;
    private TextView countTextView;
    private ScrollView mainScroll;

    private class Student {
        String name;
        String id;

        Student(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // الـ LinearLayout الرئيسي لكل الشاشة
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.WHITE);
        mainLayout.setPadding(16, 16, 16, 16);

        // TextView أعلى الشاشة لعرض العدد
        countTextView = new TextView(this);
        countTextView.setTextSize(20);
        countTextView.setTextColor(Color.BLACK);
        countTextView.setPadding(10, 20, 10, 20);
        countTextView.setBackgroundColor(Color.LTGRAY);
        countTextView.setText("العدد الكلي: ...");
        mainLayout.addView(countTextView);
        Button parentsButton = new Button(this);
        parentsButton.setText("أولياء الأمور المعروفين");
        parentsButton.setBackgroundColor(Color.parseColor("#FFCC00"));
        parentsButton.setTextColor(Color.BLACK);

        parentsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showParentsDialog();
            }
        });

        mainLayout.addView(parentsButton);

        // ScrollView
        mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT
        ));

        // LinearLayout جوه ScrollView
        containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        containerLayout.setPadding(10, 10, 10, 10);

        mainScroll.addView(containerLayout);
        mainLayout.addView(mainScroll);

        setContentView(mainLayout);

        fetchAllStudents();
    }

    private void showParentsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("أولياء الأمور المعروفين");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(20, 20, 20, 20);

        scrollView.addView(dialogLayout);

        // مكان كتابة الاسم الجديد
        EditText newParentInput = new EditText(this);
        newParentInput.setHint("اسم ولي الأمر الجديد");
        dialogLayout.addView(newParentInput);

        // زرار الإضافة
        Button addButton = new Button(this);
        addButton.setText("إضافة ولي أمر جديد");
        dialogLayout.addView(addButton);

        builder.setView(scrollView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // أول ما يفتح الديالوج، يجيب من فايربيز
        fetchAndShowParents(dialogLayout);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String parentName = newParentInput.getText().toString().trim();
                if (!parentName.isEmpty()) {
                    addNewParentToFirebase(parentName, dialog);
                } else {
                    Toast.makeText(allStudents.this, "اكتب اسم ولي الأمر", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void fetchAndShowParents(LinearLayout dialogLayout) {
        DatabaseReference fengerModuleRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("parents")
                .child("fengerModule");

        fengerModuleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> parentsList = new ArrayList<>();
                for (DataSnapshot parentSnap : snapshot.getChildren()) {
                    parentsList.add(parentSnap);
                }

                Collections.sort(parentsList, (a, b) -> {
                    String nameA = a.child("parentName").getValue(String.class);
                    String nameB = b.child("parentName").getValue(String.class);
                    if (nameA == null) return 1;
                    if (nameB == null) return -1;
                    return nameA.compareToIgnoreCase(nameB);
                });

                for (DataSnapshot parentSnap : parentsList) {
                    String parentName = parentSnap.child("parentName").getValue(String.class);
                    Long fingerId = parentSnap.child("fingerId").getValue(Long.class);
                    String parentKey = parentSnap.getKey();

                    if (parentName != null && fingerId != null) {
                        TextView parentText = new TextView(allStudents.this);
                        parentText.setText(parentName + " - ID: " + fingerId);
                        parentText.setTextSize(16);
                        parentText.setPadding(10, 10, 10, 10);
                        parentText.setTextColor(Color.BLACK);
                        parentText.setBackgroundColor(Color.parseColor("#EEEEEE"));
                        dialogLayout.addView(parentText);
                        // زر تعيين الفينجر ID
                        Button assignBtn = new Button(allStudents.this);
                        assignBtn.setText("تفعيل للطالب");
                        assignBtn.setTextSize(12);
                        assignBtn.setBackgroundColor(Color.GREEN);
                        assignBtn.setTextColor(Color.WHITE);
                        assignBtn.setPadding(10, 5, 10, 5);

                        assignBtn.setOnClickListener(v -> {
                            // افتحي ديالوج تطلبي فيه ID الطالب

                                    assignParentIdToStudent(fingerId);



                        });

                        dialogLayout.addView(assignBtn);


                        // الطلاب المنتسبين
                        boolean hasStudents = false;

                        for (DataSnapshot child : parentSnap.getChildren()) {
                            String key = child.getKey();
                            if (key != null && key.startsWith("kidName")) {
                                hasStudents = true;
                                String studentName = child.getValue(String.class);
                                String number = key.replace("kidName", "");
                                String studentIdKey = "kidId" + number;
                                String studentId = parentSnap.child(studentIdKey).getValue(String.class);

                                LinearLayout studentRow = new LinearLayout(allStudents.this);
                                studentRow.setOrientation(LinearLayout.HORIZONTAL);
                                studentRow.setPadding(20, 5, 10, 5);

                                TextView kidView = new TextView(allStudents.this);
                                kidView.setText("• " + studentName);
                                kidView.setTextSize(14);
                                kidView.setTextColor(Color.DKGRAY);
                                kidView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                                studentRow.addView(kidView);

                                Button removeBtn = new Button(allStudents.this);
                                removeBtn.setText("فك الانتساب");
                                removeBtn.setTextSize(12);
                                removeBtn.setBackgroundColor(Color.RED);
                                removeBtn.setTextColor(Color.WHITE);
                                removeBtn.setOnClickListener(v -> {
                                    unassignStudentFromParent(parentKey, number);
                                });

                                studentRow.addView(removeBtn);
                                dialogLayout.addView(studentRow);
                            }
                        }

                        if (!hasStudents) {
                            TextView noKids = new TextView(allStudents.this);
                            noKids.setText("لا يوجد طلاب منتسبين");
                            noKids.setTextSize(14);
                            noKids.setPadding(30, 5, 10, 10);
                            noKids.setTextColor(Color.GRAY);
                            dialogLayout.addView(noKids);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(allStudents.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void unassignStudentFromParent(String parentKey, String number) {
        DatabaseReference parentRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("parents")
                .child("fengerModule")
                .child(parentKey);

        parentRef.child("kidName" + number).removeValue();
        parentRef.child("kidId" + number).removeValue();

        Toast.makeText(this, "تم فك الانتساب", Toast.LENGTH_SHORT).show();
    }

    private void addNewParentToFirebase(String parentName, AlertDialog dialog) {
        DatabaseReference fengerModuleRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("parents")
                .child("fengerModule");

        // نجيب آخر رقم
        fengerModuleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int maxKey = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        int key = Integer.parseInt(child.getKey());
                        if (key > maxKey) maxKey = key;
                    } catch (NumberFormatException ignored) {}
                }

                int newKey = maxKey + 1;

                // نضيف
                DatabaseReference newParentRef = fengerModuleRef.child(String.valueOf(newKey));
                newParentRef.child("parentName").setValue(parentName);
                newParentRef.child("fingerId").setValue(newKey);

                Toast.makeText(allStudents.this, "تمت إضافة ولي الأمر بنجاح", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(allStudents.this, "خطأ في الاتصال", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void fetchAllStudents() {
        DatabaseReference dataRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("data");

        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Student> studentList = new ArrayList<>();

                for (DataSnapshot kidSnapshot : snapshot.getChildren()) {
                    String kidName = kidSnapshot.child("kidName").getValue(String.class);
                    String kidId = kidSnapshot.child("kidId").getValue(String.class);

                    if (kidName != null && kidId != null) {
                        studentList.add(new Student(kidName, kidId));
                    }
                }

                // Sort alphabetically
                Collections.sort(studentList, new Comparator<Student>() {
                    @Override
                    public int compare(Student s1, Student s2) {
                        return s1.name.compareToIgnoreCase(s2.name);
                    }
                });

                containerLayout.removeAllViews();

                for (Student student : studentList) {
                    addStudentTextView(student.name, student.id);
                }

                // تحديث العدد
                countTextView.setText("العدد الكلي: " + studentList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(allStudents.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addStudentTextView(String kidName, final String kidId) {
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(10, 10, 10, 10);

        TextView textView = new TextView(this);
        textView.setText(kidName);
        textView.setTextSize(18);
        textView.setTextColor(Color.BLACK);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button button = new Button(this);
        button.setText("انتساب");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showParentsSelectionDialog(kidName, kidId);
            }
        });

        rowLayout.addView(textView);
        rowLayout.addView(button);
        containerLayout.addView(rowLayout);
    }

    private void showParentsSelectionDialog(String studentName, String studentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر ولي الأمر");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(20, 20, 20, 20);
        scrollView.addView(dialogLayout);
        builder.setView(scrollView);

        AlertDialog dialog = builder.create();
        dialog.show();

        DatabaseReference fengerModuleRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("parents")
                .child("fengerModule");

        fengerModuleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> parentsList = new ArrayList<>();
                for (DataSnapshot parentSnap : snapshot.getChildren()) {
                    parentsList.add(parentSnap);
                }

                // ترتيب أبجدي
                Collections.sort(parentsList, (a, b) -> {
                    String nameA = a.child("parentName").getValue(String.class);
                    String nameB = b.child("parentName").getValue(String.class);
                    if (nameA == null) return 1;
                    if (nameB == null) return -1;
                    return nameA.compareToIgnoreCase(nameB);
                });

                for (DataSnapshot parentSnap : parentsList) {
                    String parentKey = parentSnap.getKey();
                    String parentName = parentSnap.child("parentName").getValue(String.class);
                    String fingerId = parentSnap.child("fingerId").getValue(Long.class) + "";

                    if (parentName == null || fingerId == null) continue;

                    LinearLayout parentBlock = new LinearLayout(allStudents.this);
                    parentBlock.setOrientation(LinearLayout.VERTICAL);
                    parentBlock.setPadding(10, 10, 10, 10);
                    parentBlock.setBackgroundColor(Color.parseColor("#EEEEEE"));

                    // اسم ولي الأمر
                    TextView tv = new TextView(allStudents.this);
                    tv.setText(parentName + " - ID: " + fingerId);
                    tv.setTextSize(16);
                    tv.setTextColor(Color.BLACK);
                    tv.setOnClickListener(v -> {
                        dialog.dismiss();
                        showConfirmationDialog(studentName, studentId, parentKey, parentName + " - ID: " + fingerId);
                    });
                    parentBlock.addView(tv);

                    // الطلاب المنتسبين
                    List<String> studentLines = new ArrayList<>();
                    for (DataSnapshot child : parentSnap.getChildren()) {
                        String key = child.getKey();
                        if (key != null && key.startsWith("kidName")) {
                            String studentValue = child.getValue(String.class);
                            studentLines.add("• " + studentValue);
                        }
                    }

                    if (studentLines.isEmpty()) {
                        TextView noKids = new TextView(allStudents.this);
                        noKids.setText("لا يوجد طلاب منتسبين");
                        noKids.setTextSize(14);
                        noKids.setPadding(20, 5, 10, 10);
                        noKids.setTextColor(Color.GRAY);
                        parentBlock.addView(noKids);
                    } else {
                        for (String student : studentLines) {
                            TextView kidView = new TextView(allStudents.this);
                            kidView.setText(student);
                            kidView.setTextSize(14);
                            kidView.setPadding(30, 2, 10, 2);
                            kidView.setTextColor(Color.DKGRAY);
                            parentBlock.addView(kidView);
                        }
                    }

                    dialogLayout.addView(parentBlock);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(allStudents.this, "خطأ في التحميل", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmationDialog(String studentName, String studentId, String parentKey, String parentDisplay) {
        new AlertDialog.Builder(this)
                .setTitle("تأكيد الانتساب")
                .setMessage("هل تريد بالفعل نسب الطالب \"" + studentName + "\" إلى ولي الأمر \"" + parentDisplay + "\" ؟")
                .setPositiveButton("نعم", (dialog, which) -> {
                    addStudentToParent(studentName, studentId, parentKey);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void addStudentToParent(String studentName, String studentId, String parentKey) {
        DatabaseReference parentRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("parents")
                .child("fengerModule")
                .child(parentKey);

        parentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int maxIndex = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    if (key != null && key.startsWith("kidId")) {
                        try {
                            int index = Integer.parseInt(key.replace("kidId", ""));
                            if (index > maxIndex) maxIndex = index;
                        } catch (NumberFormatException ignored) {}
                    }
                }

                int newIndex = maxIndex + 1;
                parentRef.child("kidId" + newIndex).setValue(studentId);
                parentRef.child("kidName" + newIndex).setValue(studentName);

                Toast.makeText(allStudents.this, "تم نسب الطالب بنجاح", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(allStudents.this, "خطأ في الكتابة", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void assignParentIdToStudent( Long fingerId) {
        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("parents");

        studentRef.child("id").setValue(fingerId);
        studentRef.child("add").setValue(true);

        Toast.makeText(this, "تم تعيين الفينجر ID للطالب", Toast.LENGTH_SHORT).show();
    }



}

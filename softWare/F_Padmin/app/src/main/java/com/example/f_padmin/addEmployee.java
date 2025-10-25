package com.example.f_padmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class addEmployee extends AppCompatActivity {

    private String mainCollection = "1";

    Button ok, again, go;
    EditText kidID, fingerId, fatherId, kidName, phone, phone2;
    DatabaseReference databaseRefer;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri capturedImageUri;
    private Bitmap capturedImageBitmap;
    private StorageReference storageReference;



    interface OnIdGenerated {
        void onReady(String kidId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);
//        kidID = findViewById(R.id.kidID);
//        fingerId = findViewById(R.id.fingerId);
//        ok = findViewById(R.id.ok);
//        go = findViewById(R.id.go);
//        again = findViewById(R.id.again);
//        fatherId = findViewById(R.id.fatherId);
//        kidName = findViewById(R.id.kidName);
//        phone = findViewById(R.id.phone);
//        phone2 = findViewById(R.id.phone2);
////        phone3 = findViewById(R.id.phone3);
//        storageReference = FirebaseStorage.getInstance().getReference("studentPhotos");
//
//        Button studentPhotoBtn = findViewById(R.id.studentPhoto);
//        studentPhotoBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//                }
//            }
//        });
//
//
//
//        databaseRefer = FirebaseDatabase.getInstance().getReference(mainCollection);
//        previewStudentId(); // ✅ يعرض الـ ID الجاهز من غير ما يزود العداد
//        updateFingerID();   // ✅ يعرض أقرب بصمة متاحة
//
//
//        // مراقبة حالة الزر again
//        DatabaseReference databaseReference = databaseRefer.child("failed");
//        databaseReference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                Boolean failed = dataSnapshot.getValue(Boolean.class);
//                if (failed != null && !failed) {
//                    again.setVisibility(View.GONE);
//                } else {
//                    again.setVisibility(View.VISIBLE);
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                // التعامل مع الخطأ
//            }
//        });
//
//        again.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                databaseRefer.child("add").setValue(true);
//                databaseRefer.child("failed").setValue(false);
//            }
//        });
//
//        ok.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String fingerIdText = fingerId.getText().toString();
//                String fatherIdValue = fatherId.getText().toString();
//                String kidNameValue = kidName.getText().toString();
//                String phoneValue = phone.getText().toString();
//                String phoneValue2 = phone2.getText().toString();
//
//                if (fingerIdText.isEmpty() || fatherIdValue.isEmpty() || kidNameValue.isEmpty()) {
//                    Toast.makeText(addEmployee.this, "يرجى ملء جميع الحقول المطلوبة", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                try {
//                    int fingerIdValue = Integer.parseInt(fingerIdText);
//
//                    generateStudentId(new addActivity.OnIdGenerated() {
//                        @Override
//                        public void onReady(String kidIdValue) {
//                            saveDataToFirebase(kidIdValue, fingerIdValue, fatherIdValue, kidNameValue, phoneValue, phoneValue2, phoneValue3);
//
//                            fatherId.setText("");
//                            kidName.setText("");
//                            phone.setText("");
//                            phone2.setText("");
//
//                            updateFingerID();
//
//                            // توليد ID جديد بعد الحفظ
//                            previewStudentId(); // فقط عرض آخر ID متاح بدون ما يتحجز
//
//                        }
//                    });
//
//                } catch (NumberFormatException e) {
//                    Toast.makeText(addEmployee.this, "الرجاء إدخال رقم صحيح لـ Finger ID", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//
//        go.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent mainIntent = new Intent(addEmployee.this, activate.class);
//                startActivity(mainIntent);
//            }
//        });
//
//        // توليد رقم بصمة تلقائي (من الأرقام المتاحة في fengerModule)
//    }
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
//            Bundle extras = data.getExtras();
//            capturedImageBitmap = (Bitmap) extras.get("data");
//            Toast.makeText(this, "تم التقاط الصورة بنجاح، سيتم رفعها عند الحفظ", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//
//
//    private int findNextAvailableFingerId(ArrayList<Integer> existingFingerIds) {
//        if (existingFingerIds.isEmpty()) {
//            return 1;
//        }
//
//        Collections.sort(existingFingerIds);
//
//        int missingFingerId = 1;
//        for (int i = 0; i < existingFingerIds.size(); i++) {
//            if (existingFingerIds.get(i) != missingFingerId) {
//                return missingFingerId;
//            }
//            missingFingerId++;
//        }
//
//        return existingFingerIds.get(existingFingerIds.size() - 1) + 1;
//    }
//
//    private void updateFingerID() {
//        databaseRefer.child("fengerModule").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                ArrayList<Integer> existingFingerIds = new ArrayList<>();
//
//                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                    Integer fingerId = snapshot.child("fingerId").getValue(Integer.class);
//                    if (fingerId != null) {
//                        existingFingerIds.add(fingerId);
//                    }
//                }
//
//                int nextFingerId = findNextAvailableFingerId(existingFingerIds);
//
//                // تعيين الرقم التالي في الـ EditText
//                fingerId.setText(String.valueOf(nextFingerId));
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                // التعامل مع الخطأ
//            }
//        });
//    }
//
//    private void previewStudentId() {
//        DatabaseReference counterRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("studentCounter");
//        counterRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                String lastDate = snapshot.child("lastDate").getValue(String.class);
//                Integer count = snapshot.child("count").getValue(Integer.class);
//
//                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());
//
//                int currentCount = (lastDate != null && lastDate.equals(currentDate) && count != null) ? count : 0;
//                String previewId = currentDate + "-" + String.format(Locale.ENGLISH, "%02d", currentCount + 1);
//
//                kidID.setText(previewId);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(addEmployee.this, "فشل في قراءة رقم الطالب", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void generateStudentId(@NonNull addActivity.OnIdGenerated callback) {
//        DatabaseReference counterRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("studentCounter");
//        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());
//
//        counterRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
//            @NonNull
//            @Override
//            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
//                String lastDate = currentData.child("lastDate").getValue(String.class);
//                Integer count = currentData.child("count").getValue(Integer.class);
//
//                if (count == null || lastDate == null || !lastDate.equals(currentDate)) {
//                    count = 1;
//                } else {
//                    count++;
//                }
//
//                currentData.child("lastDate").setValue(currentDate);
//                currentData.child("count").setValue(count);
//                String newId = currentDate + "-" + String.format(Locale.ENGLISH, "%02d", count);
//                currentData.child("tempId").setValue(newId);
//
//                return com.google.firebase.database.Transaction.success(currentData);
//            }
//
//            @Override
//            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
//                if (committed && currentData != null) {
//                    String newKidId = currentData.child("tempId").getValue(String.class);
//                    if (newKidId != null) {
//                        kidID.setText(newKidId);
//                        callback.onReady(newKidId);  // ✅ كمل بعد توليد ID
//                    }
//                } else {
//                    Toast.makeText(addEmployee.this, "خطأ في توليد رقم الطالب", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }
//
//
//
//
//    private void saveDataToFirebase(String kidId, int fingerId, String fatherId, String kidName, String phone, String phone2, String phone3) {
//        if (kidId.isEmpty() || fatherId.isEmpty() || kidName.isEmpty()) {
//            return;
//        }
//
//        String[] nameParts = kidName.split(" ");
//        String fatherName = "";
//        if (nameParts.length > 1) {
//            for (int i = 1; i < nameParts.length; i++) {
//                fatherName += nameParts[i] + " ";
//            }
//            fatherName = fatherName.trim();
//        }
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.DAY_OF_MONTH, 1); // أول يوم من الشهر الحالي
//        calendar.add(Calendar.MONTH, 1);        // الشهر الجاي
//        String nextMonthStart = sdf.format(calendar.getTime());
//
//        databaseRefer.child("name=id").child(kidName).setValue(kidId);
//
//        databaseRefer.child("names").child(kidName).setValue(String.valueOf(fingerId));
//
//
//
//        databaseRefer.child("data").child(kidId).child("isInClass").setValue(false);
//        databaseRefer.child("data").child(kidId).child("kidId").setValue(kidId);
//        databaseRefer.child("data").child(kidId).child("fingerId").setValue(fingerId);
//        databaseRefer.child("data").child(kidId).child("kidName").setValue(kidName);
//        databaseRefer.child("data").child(kidId).child( "kidLevel").setValue(1);
//        databaseRefer.child("data").child(kidId).child("userEndPeriod").setValue(nextMonthStart);
//
//
//
//        if (!phone.isEmpty()) {
//            databaseRefer.child("data").child(kidId).child("phoneFather").setValue(phone);
//        }
//
//        if (!phone2.isEmpty()) {
//            databaseRefer.child("data").child(kidId).child("phoneMother").setValue(phone2);
//        }
//
//        if (!phone3.isEmpty()) {
//            databaseRefer.child("data").child(kidId).child("phone3").setValue(phone3);
//        }
//
//        databaseRefer.child("data").child(kidId).child("NationalID").setValue(fatherId);
//        databaseRefer.child("data").child(kidId).child("fatherName").setValue(fatherName);
//
//        databaseRefer.child("employees").child(fatherId).child("id").setValue(fatherId);
//        databaseRefer.child("employees").child(fatherId).child("name").setValue(kidName);
//        databaseRefer.child("employees").child(fatherId).child("KidId").setValue(kidId);
//
//        databaseRefer.child("fengerModule").child(String.valueOf(fingerId)).child("fingerId").setValue(fingerId);
//        databaseRefer.child("fengerModule").child(String.valueOf(fingerId)).child("kidId").setValue(kidId);
//
//        if (capturedImageBitmap != null) {
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            capturedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//            byte[] imageData = baos.toByteArray();
//
//            StorageReference photoRef = storageReference.child(kidId + ".jpg");
//            photoRef.putBytes(imageData).addOnSuccessListener(taskSnapshot -> {
//                photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
//                    databaseRefer.child("data").child(kidId).child("studentPhoto").child("studentPhoto").setValue(uri.toString());
//                    Toast.makeText(this, "تم رفع الصورة بنجاح", Toast.LENGTH_SHORT).show();
//                });
//            }).addOnFailureListener(e -> {
//                Toast.makeText(this, "فشل في رفع الصورة", Toast.LENGTH_SHORT).show();
//            });
//        }

    }
}

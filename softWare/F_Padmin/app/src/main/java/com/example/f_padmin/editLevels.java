// الكود بعد تعديل عرض المحتوى القديم من Firebase تلقائياً داخل ScrollView
package com.example.f_padmin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class editLevels extends AppCompatActivity {
    private String mainCollection = "1";

    private LinearLayout linearLayoutLevels;
    private DatabaseReference databaseReference;

    private String selectedKidId = null;
    private int selectedKidLevel = 0;
    private LinearLayout currentFileListLayout = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_levels);

        linearLayoutLevels = findViewById(R.id.linear_layout_levels);
        databaseReference = FirebaseDatabase.getInstance().getReference(mainCollection);

        fetchAndDisplayData();
    }

    private void fetchAndDisplayData() {
        databaseReference.child("data").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Kid> kids = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String kidId = snapshot.getKey();
                    String kidName = snapshot.child("kidName").getValue(String.class);
                    Integer kidLevel = snapshot.child("kidLevel").getValue(Integer.class);

                    if (kidName != null) {
                        kids.add(new Kid(kidId, kidName, kidLevel != null ? kidLevel : 0));
                    }
                }
                Collections.sort(kids);

                for (Kid kid : kids) {
                    addKidToLayout(kid.getId(), kid.getName(), kid.getLevel());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(editLevels.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addKidToLayout(final String kidId, final String kidName, int kidLevel) {
        LinearLayout outerLayout = new LinearLayout(this);
        outerLayout.setOrientation(LinearLayout.VERTICAL);
        outerLayout.setBackgroundColor(Color.YELLOW);
        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        outerParams.setMargins(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15));
        outerLayout.setLayoutParams(outerParams);
        outerLayout.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));

        TextView nameTextView = new TextView(this);
        nameTextView.setText(kidName);
        nameTextView.setGravity(Gravity.CENTER);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        outerLayout.addView(nameTextView);

        LinearLayout horizontalLayout = new LinearLayout(this);
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        horizontalLayout.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dpToPx(65), dpToPx(65));
        buttonParams.setMargins(dpToPx(10), 0, dpToPx(10), 0);

        Button promoteButton = new Button(this);
        promoteButton.setText("ترقية");
        promoteButton.setLayoutParams(buttonParams);

        final TextView levelTextView = new TextView(this);
        levelTextView.setText(String.valueOf(kidLevel));
        levelTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        levelTextView.setPadding(dpToPx(20), 0, dpToPx(20), 0);

        promoteButton.setOnClickListener(v -> {
            int newLevel = Integer.parseInt(levelTextView.getText().toString()) + 1;
            updateKidLevel(kidId, newLevel);
            levelTextView.setText(String.valueOf(newLevel));
        });

        horizontalLayout.addView(promoteButton);
        horizontalLayout.addView(levelTextView);

        Button demoteButton = new Button(this);
        demoteButton.setText("تخفيض");
        demoteButton.setLayoutParams(buttonParams);
        demoteButton.setOnClickListener(v -> {
            int newLevel = Math.max(Integer.parseInt(levelTextView.getText().toString()) - 1, 0);
            updateKidLevel(kidId, newLevel);
            levelTextView.setText(String.valueOf(newLevel));
        });

        horizontalLayout.addView(demoteButton);
        outerLayout.addView(horizontalLayout);

        Button uploadButton = new Button(this);
        uploadButton.setText("رفع ملف");
        uploadButton.setLayoutParams(buttonParams);
        outerLayout.addView(uploadButton);

        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        LinearLayout fileListLayout = new LinearLayout(this);
        fileListLayout.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(fileListLayout);
        outerLayout.addView(scrollView);

        // تحميل المحتوى الموجود مسبقاً
        databaseReference.child("data").child(kidId).child("development").child(String.valueOf(kidLevel))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot item : snapshot.getChildren()) {
                            String url = item.getValue(String.class);
                            if (url == null) continue;
                            if (url.endsWith(".mp4")) {
                                VideoView videoView = new VideoView(editLevels.this);
                                videoView.setVideoURI(Uri.parse(url));
                                videoView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(200), dpToPx(200)));
                                videoView.setOnPreparedListener(mp -> mp.setLooping(true));
                                videoView.start();
                                fileListLayout.addView(videoView);
                            } else {
                                ImageView imageView = new ImageView(editLevels.this);
                                imageView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(200), dpToPx(200)));
                                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                Glide.with(editLevels.this).load(url).into(imageView);
                                fileListLayout.addView(imageView);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(editLevels.this, "فشل تحميل محتوى الطالب", Toast.LENGTH_SHORT).show();
                    }
                });

        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "اختر فيديو أو صورة"), 1000);

            selectedKidId = kidId;
            selectedKidLevel = Integer.parseInt(levelTextView.getText().toString());
            currentFileListLayout = fileListLayout;
        });

        linearLayoutLevels.addView(outerLayout);
    }

    private void updateKidLevel(String kidId, int newLevel) {
        DatabaseReference kidReference = databaseReference.child("data").child(kidId);
        kidReference.child("kidLevel").setValue(newLevel);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            String path = "development/" + selectedKidLevel + "/" + System.currentTimeMillis();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(path);
            storageRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        DatabaseReference ref = databaseReference.child("data")
                                .child(selectedKidId)
                                .child("development")
                                .child(String.valueOf(selectedKidLevel))
                                .push();

                        ref.setValue(uri.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "تم رفع الملف وتسجيله بنجاح", Toast.LENGTH_SHORT).show();
                                    Log.d("FirebaseDebug", "تم رفع اللينك: " + uri.toString());

                                    if (uri.toString().endsWith(".mp4")) {
                                        VideoView videoView = new VideoView(this);
                                        videoView.setVideoURI(uri);
                                        videoView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(200), dpToPx(200)));
                                        videoView.setOnPreparedListener(mp -> mp.setLooping(true));
                                        videoView.start();
                                        currentFileListLayout.addView(videoView);
                                    } else {
                                        ImageView imageView = new ImageView(this);
                                        imageView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(200), dpToPx(200)));
                                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                        Glide.with(this).load(uri).into(imageView);
                                        currentFileListLayout.addView(imageView);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "تم رفع الملف لكن فشل تسجيله", Toast.LENGTH_SHORT).show();
                                });
                    }))
                    .addOnFailureListener(e -> Toast.makeText(this, "فشل في رفع الملف", Toast.LENGTH_SHORT).show());
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private static class Kid implements Comparable<Kid> {
        private String id;
        private String name;
        private int level;

        public Kid(String id, String name, int level) {
            this.id = id;
            this.name = name;
            this.level = level;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getLevel() { return level; }

        @Override
        public int compareTo(Kid other) {
            return this.name.compareTo(other.name);
        }
    }
}

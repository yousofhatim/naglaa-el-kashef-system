package com.example.f_puser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class Contant extends AppCompatActivity {
    private String mainCollection = "1";

    private LinearLayout videosLayout, pdfLayout, testLayout;
    private TextView levelTextView;

    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private int currentLevel = 0;
    private String kidId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contant);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // استلام قيمة kidId من الـ Intent
        Intent intent = getIntent();
        kidId = intent.getStringExtra("kidId");

        levelTextView = findViewById(R.id.textView2);
        videosLayout = findViewById(R.id.vedios);
        pdfLayout = findViewById(R.id.pdf);
        testLayout = findViewById(R.id.test);

        storageReference = FirebaseStorage.getInstance().getReference();

        // قراءة قيمة kidLevel باستخدام kidId
        databaseReference = FirebaseDatabase.getInstance().getReference(mainCollection).child("data").child(kidId).child("kidLevel");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currentLevel = dataSnapshot.getValue(Integer.class);
                    // عرض المستوى في TextView
                    levelTextView.setText("Level: " + currentLevel);
                    loadContent();
                } else {
                    Toast.makeText(Contant.this, "مستوى الطفل غير موجود في قاعدة البيانات", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Contant.this, "حدث خطأ أثناء قراءة مستوى الطفل", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadContent() {
        videosLayout.removeAllViews();
        pdfLayout.removeAllViews();
        testLayout.removeAllViews();

        DatabaseReference contentReference = FirebaseDatabase.getInstance().getReference(mainCollection).child("uploads").child(String.valueOf(currentLevel));
        contentReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Upload upload = postSnapshot.getValue(Upload.class);
                    if (upload.getType().equals("video")) {
                        addVideoToLayout(videosLayout, upload.getUrl());
                    } else if (upload.getType().equals("pdf")) {
                        addToLayout(pdfLayout, upload.getUrl());
                    } else if (upload.getType().equals("test")) {
                        addToLayout(testLayout, upload.getUrl());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Contant.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addVideoToLayout(LinearLayout layout, String url) {
        View view = getLayoutInflater().inflate(R.layout.video_item, layout, false);
        VideoView videoView = view.findViewById(R.id.video_view);
        ImageView thumbnail = view.findViewById(R.id.video_thumbnail);
        ImageView playButton = view.findViewById(R.id.play_button);

        // استخدام Glide لتحميل الصورة المصغرة للفيديو
        Glide.with(this)
                .asBitmap()
                .load(Uri.parse(url))
                .apply(new RequestOptions().frame(1000000)) // يمكن تعديل الوقت للحصول على الصورة المطلوبة
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        thumbnail.setImageBitmap(resource);
                    }
                });

        // عند النقر على الصورة المصغرة، تشغيل الفيديو
        thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thumbnail.setVisibility(View.GONE);
                playButton.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                videoView.setVideoURI(Uri.parse(url));
                videoView.start();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thumbnail.setVisibility(View.GONE);
                playButton.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                videoView.setVideoURI(Uri.parse(url));
                videoView.start();
            }
        });

        view.findViewById(R.id.play_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setDataAndType(Uri.parse(url), "video/mp4");
                startActivity(intent);
            }
        });

        layout.addView(view);
    }

    private void addToLayout(LinearLayout layout, String url) {
        TextView textView = new TextView(this);
        textView.setText(url);
        textView.setPadding(10, 10, 10, 10);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });
        layout.addView(textView);
    }

    public static class Upload {
        public String url;
        public String type;

        public Upload() {
            // Default constructor required for calls to DataSnapshot.getValue(Upload.class)
        }

        public Upload(String url, String type) {
            this.url = url;
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public String getType() {
            return type;
        }
    }
}

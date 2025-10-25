package com.example.f_padmin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.ArrayList;
import java.util.List;

public class addContant extends AppCompatActivity {
    private String mainCollection = "1";

    private static final int PICK_VIDEO_REQUEST = 1;
    private static final int PICK_PDF_REQUEST = 2;

    private Button addVideoButton, addPdfButton, promoteButton, demoteButton;
    private LinearLayout videosLayout, pdfLayout, testLayout;
    private TextView levelTextView;

    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private int currentLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contant);

        addVideoButton = findViewById(R.id.addVedio);
        addPdfButton = findViewById(R.id.addPdf);
        promoteButton = findViewById(R.id.promoteButton);
        demoteButton = findViewById(R.id.demoteButton);
        levelTextView = findViewById(R.id.levelTextView);
        videosLayout = findViewById(R.id.vedios);
        pdfLayout = findViewById(R.id.pdf);
        testLayout = findViewById(R.id.test);

        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference(mainCollection).child("uploads");

        addVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(PICK_VIDEO_REQUEST);
            }
        });

        addPdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(PICK_PDF_REQUEST);
            }
        });

        promoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLevel(1);
            }
        });

        demoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLevel(-1);
            }
        });

        loadContent();
    }

    private void openFileChooser(int requestCode) {
        Intent intent = new Intent();
        intent.setType(requestCode == PICK_VIDEO_REQUEST ? "video/*" : "application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            uploadFile(fileUri, requestCode);
        }
    }

    private void uploadFile(Uri fileUri, int requestCode) {
        final StorageReference fileReference = storageReference.child("uploads/" + currentLevel + "/" + System.currentTimeMillis()
                + (requestCode == PICK_VIDEO_REQUEST ? ".mp4" : ".pdf"));

        fileReference.putFile(fileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String fileId = databaseReference.child(String.valueOf(currentLevel)).push().getKey();
                                Upload upload = new Upload(uri.toString(), requestCode == PICK_VIDEO_REQUEST ? "video" : "pdf");
                                databaseReference.child(String.valueOf(currentLevel)).child(fileId).setValue(upload);

                                Toast.makeText(addContant.this, "Upload successful", Toast.LENGTH_LONG).show();
                                loadContent();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(addContant.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadContent() {
        videosLayout.removeAllViews();
        pdfLayout.removeAllViews();
        testLayout.removeAllViews();

        databaseReference.child(String.valueOf(currentLevel)).addValueEventListener(new ValueEventListener() {
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
                Toast.makeText(addContant.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
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

    private void changeLevel(int change) {
        currentLevel += change;
        if (currentLevel < 0) currentLevel = 0;
        levelTextView.setText(String.valueOf(currentLevel));
        loadContent();
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

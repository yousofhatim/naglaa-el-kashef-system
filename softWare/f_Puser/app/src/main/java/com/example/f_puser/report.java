package com.example.f_puser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.NetworkRequest;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
//import com.google.android.gms.nearby.Nearby;
//import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class report extends AppCompatActivity {
    private String mainCollection = "1";
    //    private ConnectionsClient connectionsClient;
    private final String SERVICE_ID = "com.example.f_puser.SERVICE";

    private DatabaseReference databaseRefer;
    private SharedPreferences sharedPreferences;
    private MediaPlayer mediaPlayer;

    private String kidId;

    private TextView textView, kidIdTextView,
            KidName, monthh, yearr, textResponse;
    private Button  order;
    private LinearLayout linearLayoutHorizontal;

    public String Address = "192.168.43.225";
    public String Port = "8080";
    public String ssid = "1";
    public String password = "123456789";

    private String kiidName;

    ImageView profileImage;
    TextView pickupTime;


    int totalLevels = 26;
    int currentLevel = 4;

    TextView lastSelectedLevel = null;
    int lastSelectedLevelNumber = -1;
    boolean isActivityVisible = false;


    @Override
    protected void onStart() {
        super.onStart();
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        kidId = sharedPreferences.getString("kidId_selected", null);
        if (kidId == null || kidId.isEmpty()) {
            Toast.makeText(this, "لا يوجد معرف الطالب", Toast.LENGTH_SHORT).show();
            return;
        }

        // استخدام ValueEventListener للانتظار حتى يتم قراءة اسم الطفل
        DatabaseReference kidNameRef = FirebaseDatabase.getInstance()
                .getReference(mainCollection)
                .child("data")
                .child(kidId)
                .child("kidName");
        kidNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String kidName = snapshot.getValue(String.class);
                    KidName.setText(kidName+"-"+kidId);
                    kiidName=kidName;
                    // بعد قراءة اسم الطفل، استدعاء الدالة للبحث عن الحضور
                    searchAttendance();
                } else {
                    // إذا لم يتم العثور على اسم الطفل
                    Toast.makeText(report.this, "اسم الطفل غير موجود في قاعدة البيانات", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(report.this, "حدث خطأ أثناء قراءة اسم الطفل", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        pickupTime = findViewById(R.id.switchKidButton);


//        mainScroll.addView(mainVerticalLayout);
//        setContentView(mainScroll);



        // 1. Nearby + MediaPlayer + SharedPreferences
//        connectionsClient = Nearby.getConnectionsClient(this);
        mediaPlayer = MediaPlayer.create(this, R.raw.bell);
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        kidId = sharedPreferences.getString("kidId_selected", null);

        DatabaseReference levelRef = FirebaseDatabase.getInstance()
                .getReference(mainCollection)
                .child("data")
                .child(kidId)
                .child("kidLevel");

        levelRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long levelLong = snapshot.getValue(Long.class);
                    if (levelLong != null) {
                        currentLevel = levelLong.intValue();
                        setupLevels();  // نعيد رسم المستويات بعد التحديث
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(report.this, "فشل في تحميل المستوى", Toast.LENGTH_SHORT).show();
            }
        });




        profileImage = findViewById(R.id.profileImage);
        // 2. ربط عناصر الواجهة

        // داخل onCreate بعد profileImage = findViewById...
        DatabaseReference photoRef = FirebaseDatabase.getInstance()
                .getReference(mainCollection)
                .child("data")
                .child(kidId)
                .child("studentPhoto")
                .child("studentPhoto");

        photoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String photoUrl = snapshot.getValue(String.class);
                    Glide.with(report.this)
                            .load(photoUrl)
                            .placeholder(R.drawable.nk)   // صورة مؤقتة
                            .into(profileImage);
                } else {
                    Toast.makeText(report.this, "مفيش صورة للطالب", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(report.this, "فشل تحميل صورة الطالب", Toast.LENGTH_SHORT).show();
            }
        });


        order = findViewById(R.id.order);

//        textResponse = findViewById(R.id.response);
        textView = findViewById(R.id.textView);
//        kidIdTextView = findViewById(R.id.kidIdTextView);
        KidName = findViewById(R.id.KidName);
//        lastDate2 = findViewById(R.id.lastDate2);
        monthh = findViewById(R.id.month);
        yearr = findViewById(R.id.year);
        linearLayoutHorizontal = findViewById(R.id.linearLayoutHorizontal);


        // 5. التحقق من Kid ID
        if (kidId == null) {
            startActivity(new Intent(report.this, login.class));
            finish();
            return;
        }

//        kidIdTextView.setText("كود الطالب: " + kidId);

        // 6. تحميل بيانات نهاية الفترة من المسار العام
        databaseRefer = FirebaseDatabase.getInstance().getReference(mainCollection).child("endPeriod");
        databaseRefer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String endPeriodValue = snapshot.getValue(String.class);
                if (endPeriodValue != null) checkEndPeriod(endPeriodValue);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });

        // 7. تحميل نهاية الفترة الخاصة بالطالب
        databaseRefer = FirebaseDatabase.getInstance().getReference(mainCollection).child("data").child(kidId).child("userEndPeriod");
        databaseRefer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String userEndPeriodValue = snapshot.getValue(String.class);
                if (userEndPeriodValue != null) checkUserEndPeriod(userEndPeriodValue);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });

        // 8. مراقبة حالة الطالب (In Class)
        databaseRefer = FirebaseDatabase.getInstance().getReference(mainCollection).child("data").child(kidId);
        databaseRefer.child("isInClass").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isInClass = snapshot.getValue(Boolean.class);
                if (isInClass != null) {
                    textView.setText(isInClass ? "في المؤسسة" : "خارج المؤسسة");
                    textView.setTextColor(isInClass ? Color.GREEN : Color.RED);
                    order.setVisibility(isInClass ? View.VISIBLE : View.GONE);
                    pickupTime.setVisibility(isInClass ? View.VISIBLE : View.GONE);

                    if (!isInClass) {
                        pickupTime.setText("");
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                textView.setText("Failed to load data");
                textView.setTextColor(Color.RED);
            }
        });

        // 9. آخر تاريخ حضور


        // 10. إعداد التاريخ الحالي
        Calendar calendar = Calendar.getInstance();
        monthh.setText(String.format(Locale.ENGLISH, "%02d", calendar.get(Calendar.MONTH) + 1));
        yearr.setText(String.format(Locale.ENGLISH, "%04d", calendar.get(Calendar.YEAR)));

        // 11. زر الأوردر: اتصال واي فاي + إرسال بيانات
        order.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiNetworkSpecifier wifiSpecifier = new WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(password)
                        .build();

                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(wifiSpecifier)
                        .build();

                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        cm.bindProcessToNetwork(network);

                        runOnUiThread(() -> {
                            new MyClientTask(Address, Integer.parseInt(Port)).execute();
                        });
                    }

                    @Override
                    public void onUnavailable() {
                        runOnUiThread(() -> Toast.makeText(report.this, "فشل الاتصال بنقطة", Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                Toast.makeText(report.this, "الإصدار لا يدعم الاتصال التلقائي", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLevels() {
//        LinearLayout levelsContainer = findViewById(R.id.levels);
//        levelsContainer.removeAllViews();
//
//        LinearLayout activitiesContainer = findViewById(R.id.activitiesContainer);
//        activitiesContainer.removeAllViews();

        final HorizontalScrollView activityScroll = new HorizontalScrollView(this);
        final LinearLayout activitiesLayout = new LinearLayout(this);
        activitiesLayout.setOrientation(LinearLayout.HORIZONTAL);
        activitiesLayout.setPadding(20, 20, 20, 20);
        activityScroll.setVisibility(View.GONE);
        activityScroll.addView(activitiesLayout);
//        activitiesContainer.addView(activityScroll);

        for (int i = 1; i <= totalLevels; i++) {
            TextView levelIcon = new TextView(this);
            levelIcon.setText(String.valueOf(i));
            levelIcon.setTextColor(Color.WHITE);
            levelIcon.setGravity(Gravity.CENTER);

            int size = (i == currentLevel) ? 140 : 100;
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(size, size);
            iconParams.setMargins(16, 16, 16, 16);
            levelIcon.setLayoutParams(iconParams);
            levelIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, (i == currentLevel) ? 18 : 14);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setStroke(4, Color.DKGRAY);
            bg.setColor(i == currentLevel ? Color.parseColor("#4CAF50") :
                    i < currentLevel ? Color.parseColor("#F44336") : Color.parseColor("#BDBDBD"));
            levelIcon.setBackground(bg);

            int levelNumber = i;

            if (levelNumber <= currentLevel) {
                levelIcon.setOnClickListener(v -> {
                    boolean sameLevelClicked = (levelNumber == lastSelectedLevelNumber);

                    if (sameLevelClicked && isActivityVisible) {
                        activityScroll.setVisibility(View.GONE);
                        isActivityVisible = false;
                        levelIcon.setBackground(bg);
                        lastSelectedLevel = null;
                        lastSelectedLevelNumber = -1;
                        return;
                    }

                    if (lastSelectedLevel != null) {
                        GradientDrawable prevBg = new GradientDrawable();
                        prevBg.setShape(GradientDrawable.OVAL);
                        prevBg.setStroke(4, Color.DKGRAY);
                        int prevNumber = lastSelectedLevelNumber;
                        prevBg.setColor(prevNumber == currentLevel ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
                        lastSelectedLevel.setBackground(prevBg);
                    }

                    lastSelectedLevel = levelIcon;
                    lastSelectedLevelNumber = levelNumber;

                    GradientDrawable selectedBg = new GradientDrawable();
                    selectedBg.setShape(GradientDrawable.OVAL);
                    selectedBg.setStroke(4, Color.DKGRAY);
                    selectedBg.setColor(Color.parseColor("#2196F3"));
                    levelIcon.setBackground(selectedBg);

                    activitiesLayout.removeAllViews();
                    activitiesLayout.setBackgroundColor(Color.parseColor("#2196F3"));

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(mainCollection)
                            .child("data").child(kidId)
                            .child("development").child(String.valueOf(levelNumber));

                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                Toast.makeText(report.this, "لا يوجد أنشطة لهذا المستوى", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            for (DataSnapshot snap : snapshot.getChildren()) {
                                String url = snap.getValue(String.class);
                                if (url == null) continue;

                                if (url.endsWith(".mp4")) {
                                    VideoView videoView = new VideoView(report.this);
                                    videoView.setVideoURI(Uri.parse(url));
                                    videoView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(200), dpToPx(200)));
                                    videoView.setOnPreparedListener(mp -> mp.setLooping(true));
                                    videoView.start();
                                    activitiesLayout.addView(videoView);
                                } else {
                                    ImageView imageView = new ImageView(report.this);
                                    imageView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(200), dpToPx(200)));
                                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    Glide.with(report.this).load(url).into(imageView);
                                    activitiesLayout.addView(imageView);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(report.this, "فشل في تحميل الأنشطة", Toast.LENGTH_SHORT).show();
                        }
                    });

                    activityScroll.setVisibility(View.VISIBLE);
                    isActivityVisible = true;
                });
            } else {
                levelIcon.setAlpha(0.5f);
                levelIcon.setClickable(false);
            }

//            levelsContainer.addView(levelIcon);
        }
    }


    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        String dstAddress;
        int dstPort;
        String response = "";

        MyClientTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            Socket socket = null;
            try {
                socket = new Socket(dstAddress, dstPort);

                // هنا تبعتي الرسالة اللي انتي عايزاها
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("my name is"+kidId);

                // بعدها تجهزي للرد
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                int bytesRead;
                InputStream inputStream = socket.getInputStream();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                response = byteArrayOutputStream.toString("UTF-8");

            } catch (UnknownHostException e) {
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                e.printStackTrace();
                response = "IOException: " + e.toString();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            textResponse.setText(response);
        }

    }

    // تعديل searchAttendance() بلون الخلفية حسب نوع اليوم
// عرض حضور كل يوم في الشهر الحالي
    private void searchAttendance() {
        final String year = yearr.getText().toString().trim();
        final String month = monthh.getText().toString().trim();

        linearLayoutHorizontal.removeAllViews();

        DatabaseReference monthRef = FirebaseDatabase.getInstance().getReference(mainCollection)
                .child("dateData").child(year).child(month);

        monthRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Calendar calendar = Calendar.getInstance();
                int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
                int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                for (int day = 1; day <= daysInMonth; day++) {
                    String dayString = String.format(Locale.ENGLISH, "%02d", day);

                    LinearLayout column = new LinearLayout(report.this);
                    column.setOrientation(LinearLayout.VERTICAL);
                    column.setPadding(16, 8, 16, 8);
                    column.setGravity(Gravity.CENTER);

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(8, 0, 8, 0);
                    column.setLayoutParams(layoutParams);
                    column.setBackgroundResource(R.drawable.bg_default);

                    TextView dayTitle = new TextView(report.this);
                    String arabicDayName = getArabicDayName(Integer.parseInt(year), Integer.parseInt(month), day);
                    dayTitle.setText("اليوم " + day + "\n" + arabicDayName);
                    dayTitle.setTypeface(Typeface.DEFAULT_BOLD);
                    dayTitle.setGravity(Gravity.CENTER);
                    column.addView(dayTitle);

                    if (day > currentDay) {
                        column.setBackgroundResource(R.drawable.bg_pending);
                        addCenteredText(column, "قريباً");
                    } else if (!snapshot.hasChild(dayString)) {
                        column.setBackgroundResource(R.drawable.bg_day_off);
                        addCenteredText(column, "إجازة");
                    } else {
                        DataSnapshot daySnap = snapshot.child(dayString);
                        if (!daySnap.hasChild(kidId)) {
                            column.setBackgroundResource(R.drawable.bg_error);
                            addCenteredText(column, "غياب");
                        } else {
                            DataSnapshot studentSnap = daySnap.child(kidId);
                            String checkIn = studentSnap.child("checkIn").getValue(String.class);
                            String checkOut = studentSnap.child("checkOut").getValue(String.class);

                            column.setBackgroundResource(R.drawable.bg_present);

                            if (checkIn != null) {
                                addCenteredText(column, "دخول: " + checkIn);

                                // لو ده اليوم الحالي
                                if (day == currentDay) {
                                    try {
                                        // أول حاجة: parse بالـ 24 ساعة
                                        SimpleDateFormat parseFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                                        Date checkInDate = parseFormat.parse(checkIn);

                                        // أضف 4 ساعات
                                        Calendar pickUpCalendar = Calendar.getInstance();
                                        pickUpCalendar.setTime(checkInDate);
                                        pickUpCalendar.add(Calendar.HOUR_OF_DAY, 4);

                                        // دلوقتي الفورمات للعرض بالـ 12 ساعة
                                        SimpleDateFormat displayFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
                                        String pickupTimeString = displayFormat.format(pickUpCalendar.getTime());

                                        pickupTime.setText("معاد الخروج\n" + pickupTimeString);

                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                        pickupTime.setText("خطأ في تحديد وقت الخروج");
                                    }
                                }

                            }

                            if (checkOut != null) {
                                addCenteredText(column, "خروج: " + checkOut);
                            }

                            // مدة الحضور
                            String duration = "";
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
                                Date inTime = sdf.parse(checkIn);
                                Date outTime;

                                if (checkOut != null && !checkOut.isEmpty()) {
                                    outTime = sdf.parse(checkOut);
                                } else {
                                    String nowStr = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
                                            .format(Calendar.getInstance().getTime());
                                    outTime = sdf.parse(nowStr);
                                }

                                long diff = outTime.getTime() - inTime.getTime();
                                long seconds = diff / 1000 % 60;
                                long minutes = diff / (1000 * 60) % 60;
                                long hours = diff / (1000 * 60 * 60);

                                duration = String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds);

                            } catch (Exception e) {
                                Log.e("duration-error", e.getMessage());
                            }

                            if (!duration.isEmpty()) {
                                addCenteredText(column, checkOut == null ? "المدة حتى الآن: " + duration : "المدة: " + duration);
                            }
                        }
                    }

                    linearLayoutHorizontal.addView(column);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(report.this, "فشل تحميل البيانات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCenteredText(LinearLayout layout, String text) {
        TextView tv = new TextView(report.this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);
    }

    private String getArabicDayName(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);
        String[] daysArabic = {"الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        return daysArabic[(dayOfWeek - 1) % 7];
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private LinearLayout createRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 8, 8, 8);
        return row;
    }

    private void addCell(LinearLayout row, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setPadding(8, 8, 8, 8);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setWidth(160); // ثبتي العرض عشان يتساوى في كل الأعمدة
        row.addView(tv);
    }

    private void addFixedLabel(LinearLayout row, String label) {
        TextView fixed = new TextView(this);
        fixed.setText(label);
        fixed.setTextSize(16);
        fixed.setTypeface(null, Typeface.BOLD);
        fixed.setTextColor(Color.WHITE);
        fixed.setPadding(8, 8, 8, 8);
        fixed.setGravity(Gravity.CENTER);
        fixed.setWidth(160); // نفس العرض
        row.addView(fixed);
    }

    private void checkEndPeriod(String endPeriodStr) {
        // إعداد التنسيق
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            // تحويل النص إلى تاريخ
            Date endDate = dateFormat.parse(endPeriodStr);
            Date currentDate = Calendar.getInstance().getTime();

            // التحقق مما إذا كان التاريخ في الماضي
            if (endDate != null && endDate.before(currentDate)) {
                // الانتقال إلى نشاط payPeriod

                Intent payPeriodIntent = new Intent(report.this, endPeriod.class);
                startActivity(payPeriodIntent);
                finish(); // إنهاء النشاط الحالي
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void checkUserEndPeriod(String endPeriodStr) {
        // إعداد التنسيق
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            // تحويل النص إلى تاريخ
            Date endDate = dateFormat.parse(endPeriodStr);
            Date currentDate = Calendar.getInstance().getTime();

            // التحقق مما إذا كان التاريخ في الماضي
            if (endDate != null && endDate.before(currentDate)) {
                // الانتقال إلى نشاط payPeriod

                Intent payPeriodIntent = new Intent(report.this, payPeriod.class);
                startActivity(payPeriodIntent);
                finish(); // إنهاء النشاط الحالي
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
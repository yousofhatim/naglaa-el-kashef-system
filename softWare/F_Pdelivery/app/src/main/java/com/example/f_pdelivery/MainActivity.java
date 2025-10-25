package com.example.f_pdelivery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private String mainCollection = "1";

//    private TextView logView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean serverRunning = true;
    private DatabaseReference databaseRefer,databaseRefe;
    private LinearLayout linearLayoutOrders;
    private MediaPlayer mediaPlayer;
    Button stop,go,btn_udp;


    private TextView totalInClass;
    private LinearLayout forTextViewOutOfSystem;
    private LinearLayout currentDetailsLayout;  // لإضافة التفاصيل
    private DatabaseReference databaseReference;  // مرجع قاعدة البيانات

//    TextView info, infoip, msg;
    String message = "";
    private Set<String> addedOrderIds = new HashSet<>();

    ServerSocket serverSocket;
    private List<Kid> allKidsInClass = new ArrayList<>();
    private List<Kid> allKidsNotInClass = new ArrayList<>();


    LinearLayout linearLayoutNotInclass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linearLayoutNotInclass = findViewById(R.id.linear_layout_notInclass);
        linearLayoutOrders = findViewById(R.id.linear_layout_orders);
        HorizontalScrollView indexMain = findViewById(R.id.indexMain);
        LinearLayout indexContainer = findViewById(R.id.indexContainer);

//        logView = findViewById(R.id.logView);
//        List<String> arabicLetters = Arrays.asList("الكل","ا","ب","ت","ث","ج","ح","خ","د","ذ","ر","ز","س","ش","ص","ض","ط","ظ","ع","غ","ف","ق","ك","ل","م","ن","ه","و","ي");

//        for (String letter : arabicLetters) {
//            Button button = new Button(this);
//            button.setText(letter);
//            button.setPadding(20, 10, 20, 10);
//            button.setBackgroundColor(Color.LTGRAY);
//
//            button.setOnClickListener(v -> filterByLetter(letter));
//            indexContainer.addView(button);
//        }

        mediaPlayer = MediaPlayer.create(this, R.raw.bell);


        // شغل السيرفر
        startUdpServer();


//        info = (TextView) findViewById(R.id.info);
//        infoip = (TextView) findViewById(R.id.infoip);
//        msg = (TextView) findViewById(R.id.msg);
        FirebaseDatabase.getInstance().setPersistenceEnabled(false);
        btn_udp= findViewById(R.id.udp);
        btn_udp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(MainActivity.this, UDP.class);
                startActivity(mainIntent);
            }
        });
//        infoip.setText(getIpAddress());


        totalInClass = findViewById(R.id.totalInClass);
        forTextViewOutOfSystem = findViewById(R.id.forTextViewOutOfSystem);

        // الإشارة إلى قاعدة بيانات Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference().child(mainCollection).child("data");

        // الاستماع إلى التغييرات في قاعدة البيانات
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;

                forTextViewOutOfSystem.removeAllViews();
                linearLayoutNotInclass.removeAllViews();

                List<Kid> kidsList = new ArrayList<>();
                List<Kid> kidsNotInClassList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Boolean isInClass = snapshot.child("isInClass").getValue(Boolean.class);
                    String kidName = snapshot.child("kidName").getValue(String.class);
                    String kidId = snapshot.getKey();

                    if (isInClass != null && isInClass) {
                        count++;
                        kidsList.add(new Kid(kidName, kidId));
                    } else if (isInClass != null && !isInClass) {
                        kidsNotInClassList.add(new Kid(kidName, kidId));
                    }
                }

                // sort اللي في المكان
                Collections.sort(kidsList, new Comparator<Kid>() {
                    @Override
                    public int compare(Kid kid1, Kid kid2) {
                        return kid1.getKidName().compareTo(kid2.getKidName());
                    }
                });

                // ضيف اللي في المكان
                for (Kid kid : kidsList) {
                    String kidId = kid.getKidId();

                    Calendar calendar = Calendar.getInstance();
                    String year = String.valueOf(calendar.get(Calendar.YEAR));
                    String month = String.format(Locale.ENGLISH, "%02d", calendar.get(Calendar.MONTH) + 1);
                    String day = String.format(Locale.ENGLISH, "%02d", calendar.get(Calendar.DAY_OF_MONTH));

                    DatabaseReference checkInRef = FirebaseDatabase.getInstance().getReference()
                            .child(mainCollection)
                            .child("dateData")
                            .child(year)
                            .child(month)
                            .child(day)
                            .child(kidId)
                            .child("checkIn");

                    checkInRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            String checkIn = dataSnapshot.getValue(String.class);
                            String durationText = "";

                            if (checkIn != null && !checkIn.isEmpty()) {
                                durationText = " | " + calculateDuration(checkIn);
                            }

                            addKidNameTextView(kid.getKidName() + durationText, kid.getKidId());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            addKidNameTextView(kid.getKidName(), kid.getKidId());
                        }
                    });
                }

                // sort اللي مش في المكان
                Collections.sort(kidsNotInClassList, new Comparator<Kid>() {
                    @Override
                    public int compare(Kid kid1, Kid kid2) {
                        return kid1.getKidName().compareTo(kid2.getKidName());
                    }
                });

                // ضيف اللي مش في المكان
                for (Kid kid : kidsNotInClassList) {
                    addKidNameTextViewNotInClass(kid.getKidName(), kid.getKidId());
                }

                totalInClass.setText("عدد المتواجدين: " + count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // التعامل مع الأخطاء هنا
            }
        });






        //  الكود الاصلي معلق بالاسفل

        mediaPlayer = MediaPlayer.create(this, R.raw.bell);

        stop = findViewById(R.id.stop);
        databaseRefe = FirebaseDatabase.getInstance().getReference().child("deliveryAlarm");
        go=findViewById(R.id.go);

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(MainActivity.this, activate.class);
                startActivity(mainIntent);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                databaseRefe.setValue(false);
            }
        });

        databaseRefe.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean deliveryAlarm = dataSnapshot.getValue(Boolean.class);

                if (deliveryAlarm != null && deliveryAlarm) {
                    if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        mediaPlayer.setLooping(true); // تكرار التشغيل
                    }
                } else {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause(); // أو mediaPlayer.stop() حسب احتياجاتك
                        mediaPlayer.setLooping(false); // إيقاف التكرار
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("deliveryAlarm", "Failed to read value.", databaseError.toException());
            }
        });


        databaseRefer = FirebaseDatabase.getInstance().getReference().child("orders");
        linearLayoutOrders = findViewById(R.id.linear_layout_orders);

        databaseRefer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                linearLayoutOrders.removeAllViews(); // Clear previous views

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String kidId = snapshot.getKey();
                    boolean order = snapshot.child("order").getValue(Boolean.class);

                    if (order) {
                        // Fetch kidName from another path
                        DatabaseReference kidNameRef = FirebaseDatabase.getInstance().getReference()
                                .child("data")
                                .child(kidId)
                                .child("kidName");

                        kidNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String kidName = dataSnapshot.getValue(String.class);

                                // Create a horizontal LinearLayout for each kid's order
                                LinearLayout horizontalLayout = new LinearLayout(MainActivity.this);
                                horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                layoutParams.setMargins(0, 10, 0, 10);
                                horizontalLayout.setLayoutParams(layoutParams);

                                // Create a TextView to display kid's name
                                TextView textView = new TextView(MainActivity.this);
                                textView.setText(kidName);
                                textView.setTextSize(20);
                                textView.setGravity(android.view.Gravity.CENTER);
                                textView.setBackgroundColor(Color.BLACK);
                                textView.setTextColor(Color.WHITE);
                                textView.setPadding(20, 20, 20, 20);
                                horizontalLayout.addView(textView);

                                // Create a Button next to the TextView
                                Button button = new Button(MainActivity.this);
                                button.setText("تم التسليم");


                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // Change order status to false
                                        snapshot.getRef().child("order").setValue(false);
                                    }
                                });
                                horizontalLayout.addView(button);

                                // Add the horizontal layout to the main vertical layout
                                linearLayoutOrders.addView(horizontalLayout);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e("subscriberData", "Failed to read kidName value.", databaseError.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("subscriberData", "Failed to read value.", databaseError.toException());
            }
        });
        listenToRemoteOrders();


    }

    private void filterByLetter(String letter) {
        forTextViewOutOfSystem.removeAllViews();
        linearLayoutNotInclass.removeAllViews();

        for (Kid kid : allKidsInClass) {
            if (letter.equals("الكل") || kid.getKidName().startsWith(letter)) {
                addKidNameTextView(kid.getKidName(), kid.getKidId());
            }
        }

        for (Kid kid : allKidsNotInClass) {
            if (letter.equals("الكل") || kid.getKidName().startsWith(letter)) {
                addKidNameTextViewNotInClass(kid.getKidName(), kid.getKidId());
            }
        }
    }


    private void updateOrdersCount() {
        TextView a = findViewById(R.id.a);

        int count = linearLayoutOrders.getChildCount();
        a.setText("مسموحلهم الخروج (" + count + ")");
    }

    private void updateNotInClassCount() {
        TextView d = findViewById(R.id.d);

        int count = linearLayoutNotInclass.getChildCount();
        d.setText("لازم يبصموا لو داخلين (" + count + ")");
    }

    private void listenToRemoteOrders() {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("orders");

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                linearLayoutOrders.removeAllViews();
                addedOrderIds.clear();

                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String orderKey = orderSnapshot.getKey();
                    String kidId = orderSnapshot.child("kidId").getValue(String.class);
                    String kidName = orderSnapshot.child("kidName").getValue(String.class);
                    String type = orderSnapshot.child("type").getValue(String.class);

                    if (kidId != null && kidName != null && !kidName.isEmpty() && type != null) {
                        if (type.equals("near")) {
                            addNearOrderToLayout(orderKey, kidName);
                        } else {
                            addRemoteOrderToLayout(orderKey, kidName);
                        }

                        handler.post(() -> {
                            if (mediaPlayer != null) mediaPlayer.start();
                        });
                    }
                }


                updateOrdersCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("خطأ في قراءة الطلبات: " + error.getMessage());
            }
        });
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) return false;

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }


    private void addRemoteOrderToLayout(String orderKey, String kidName) {
        runOnUiThread(() -> {
            if (addedOrderIds.contains(orderKey)) return;

            addedOrderIds.add(orderKey);

            LinearLayout horizontalLayout = new LinearLayout(MainActivity.this);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
            horizontalLayout.setPadding(20, 20, 20, 20);
            horizontalLayout.setBackgroundColor(Color.BLUE);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 10, 0, 10);
            horizontalLayout.setLayoutParams(layoutParams);

            TextView textView = new TextView(MainActivity.this);
            textView.setText(kidName);
            textView.setTextSize(20);
            textView.setTextColor(Color.WHITE);
            textView.setPadding(10, 10, 10, 10);
            textView.setGravity(Gravity.CENTER_VERTICAL);

            Button button = new Button(MainActivity.this);
            button.setText("تم التسليم");

            button.setOnClickListener(v -> {
                DatabaseReference orderRef = FirebaseDatabase.getInstance()
                        .getReference()
                        .child(mainCollection)
                        .child("orders")
                        .child(orderKey);

                orderRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("تم حذف الطلب");
                        linearLayoutOrders.removeView(horizontalLayout);
                        updateOrdersCount();
                    } else {
                        showToast("فشل في الحذف");
                    }
                });
            });

            horizontalLayout.addView(textView);
            horizontalLayout.addView(button);

            linearLayoutOrders.addView(horizontalLayout);
            updateOrdersCount();
        });
    }

    private void addNearOrderToLayout(String orderKey, String kidName) {
        runOnUiThread(() -> {
            if (addedOrderIds.contains(orderKey)) return;

            addedOrderIds.add(orderKey);

            LinearLayout horizontalLayout = new LinearLayout(MainActivity.this);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
            horizontalLayout.setPadding(20, 20, 20, 20);
            horizontalLayout.setBackgroundColor(Color.GREEN);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 10, 0, 10);
            horizontalLayout.setLayoutParams(layoutParams);

            TextView textView = new TextView(MainActivity.this);
            textView.setText(kidName);
            textView.setTextSize(20);
            textView.setTextColor(Color.BLACK);
            textView.setPadding(10, 10, 10, 10);
            textView.setGravity(Gravity.CENTER_VERTICAL);

            Button button = new Button(MainActivity.this);
            button.setText("تم التسليم");

            button.setOnClickListener(v -> {
                DatabaseReference ordersRef = FirebaseDatabase.getInstance()
                        .getReference()
                        .child(mainCollection)
                        .child("orders");

                ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> keysToDelete = new ArrayList<>();

                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            String name = orderSnap.child("kidName").getValue(String.class);
                            if (name != null && name.trim().equals(kidName.trim())) {
                                keysToDelete.add(orderSnap.getKey());
                            }
                        }

                        if (keysToDelete.isEmpty()) {
                            showToast("لا يوجد طلبات لحذفها");
                            return;
                        }

                        for (String key : keysToDelete) {
                            ordersRef.child(key).removeValue();
                        }

                        showToast("تم حذف كل الطلبات الخاصة بـ " + kidName);
                        linearLayoutOrders.removeView(horizontalLayout);
                        updateOrdersCount();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("خطأ أثناء الحذف: " + error.getMessage());
                    }
                });
            });

            horizontalLayout.addView(textView);
            horizontalLayout.addView(button);

            linearLayoutOrders.addView(horizontalLayout);
            updateOrdersCount();
        });
    }


    private void fetchKidNameAndAddOrder(String kidId) {
        DatabaseReference kidNameRef = FirebaseDatabase.getInstance().getReference()
                .child(mainCollection)
                .child("data")
                .child(kidId)
                .child("kidName");

        kidNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String kidName = dataSnapshot.getValue(String.class);

                if (kidName == null || kidName.isEmpty()) {
                    showToast("kidName مش موجود للكود: " + kidId);
                    return;
                }

                DatabaseReference nearOrdersRef = FirebaseDatabase.getInstance()
                        .getReference(mainCollection)
                        .child("orders");

                nearOrdersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long nextIndex = 1;

                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                try {
                                    long num = Long.parseLong(child.getKey());
                                    if (num >= nextIndex) {
                                        nextIndex = num + 1;
                                    }
                                } catch (NumberFormatException e) {
                                    // skip non-numeric keys
                                }
                            }
                        }

                        String newKey = String.valueOf(nextIndex);

                        DatabaseReference newOrderRef = nearOrdersRef.child(newKey);

                        newOrderRef.child("kidId").setValue(kidId);
                        newOrderRef.child("kidName").setValue(kidName);
                        newOrderRef.child("type").setValue("near")
                                .addOnSuccessListener(aVoid -> {
                                    showToast("تمت إضافة الطلب عن قرب برقم " + newKey);
                                    Log.d("FirebaseUpdate", "Near order placed successfully at " + newKey);
                                })
                                .addOnFailureListener(e -> {
                                    showToast("فشل في إضافة الطلب عن قرب");
                                    Log.e("FirebaseUpdate", "Failed to place near order.", e);
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("خطأ في قراءة الأوامر الحالية: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showToast("فشل في جلب البيانات: " + databaseError.getMessage());
            }
        });
    }

//    private void addOrderToLayout(String kidId, String kidName) {
//        runOnUiThread(() -> {
//            if (addedOrderIds.contains(kidId)) return;
//
//            addedOrderIds.add(kidId);
//
//            LinearLayout horizontalLayout = new LinearLayout(MainActivity.this);
//            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
//            horizontalLayout.setPadding(20, 20, 20, 20);
//            horizontalLayout.setBackgroundColor(Color.BLUE);
//
//            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//            );
//            layoutParams.setMargins(0, 10, 0, 10);
//            horizontalLayout.setLayoutParams(layoutParams);
//
//            TextView textView = new TextView(MainActivity.this);
//            textView.setText(kidName);
//            textView.setTextSize(20);
//            textView.setTextColor(Color.WHITE);
//            textView.setPadding(10, 10, 10, 10);
//            textView.setGravity(Gravity.CENTER_VERTICAL);
//
//            Button button = new Button(MainActivity.this);
//            button.setText("تم التسليم");
//
//            button.setOnClickListener(v -> {
//                DatabaseReference orderRef = FirebaseDatabase.getInstance()
//                        .getReference()
//                        .child(mainCollection)
//                        .child("orders")
//                        .child(kidId);
//
//                orderRef.removeValue();
//            });
//
//            horizontalLayout.addView(textView);
//            horizontalLayout.addView(button);
//
//            linearLayoutOrders.addView(horizontalLayout);
//            updateOrdersCount();
//
//        });
//    }



    private void startUdpServer() {
        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(8888);
//                appendLog("UDP Server started on port 8888");

                byte[] buffer = new byte[1024];

                while (serverRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    String sender = packet.getAddress().toString();

//                    appendLog("Received: " + message + " from " + sender);

                    // شغّل الجرس
                    handler.post(() -> {
                        if (mediaPlayer != null) {
                            mediaPlayer.start();
                        }
                    });

                    if (message.contains("|")) {
                        String[] parts = message.split("\\|", 2);
                        String receivedKidId = parts[0];
                        String receivedKidName = parts[1];
                        handleNearOrder(receivedKidId, receivedKidName);
                        // ابعتي له رد تأكيد
                        String ack = "✅ تم تسجيل طلب خروج للطالب: " + receivedKidName;
                        byte[] ackData = ack.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(
                                ackData,
                                ackData.length,
                                packet.getAddress(),
                                packet.getPort()
                        );
                        socket.send(ackPacket);

                    } else {
                        fetchKidNameAndAddOrder(message);
                    }

                    showToast("تم الاستلام: " + message);



                    if (message.equalsIgnoreCase("getIp")) {
                        String myIp = getMyIpAddress();
                        byte[] responseData = myIp.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                                responseData,
                                responseData.length,
                                packet.getAddress(),
                                packet.getPort()
                        );
                        socket.send(responsePacket);
//                        appendLog("Sent IP: " + myIp + " to " + sender);
                    }
                }

            } catch (IOException e) {
//                appendLog("Error: " + e.getMessage());
                showToast("خطأ في السيرفر: " + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }).start();
    }

    private void handleNearOrder(String kidId, String kidName) {
        runOnUiThread(() -> {
            // شيك لو الاسم ظاهر بالفعل في الواجهة
            for (int i = 0; i < linearLayoutOrders.getChildCount(); i++) {
                View view = linearLayoutOrders.getChildAt(i);
                if (view instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) view;
                    for (int j = 0; j < layout.getChildCount(); j++) {
                        View child = layout.getChildAt(j);
                        if (child instanceof TextView) {
                            String displayedName = ((TextView) child).getText().toString().trim();
                            if (displayedName.equals(kidName.trim())) {
                                showToast("الطلب موجود بالفعل في الواجهة");
                                return;
                            }
                        }
                    }
                }
            }

            // لو مش موجود في الواجهة، أضفه للـ Firebase
            DatabaseReference nearOrdersRef = FirebaseDatabase.getInstance()
                    .getReference(mainCollection)
                    .child("orders");

            nearOrdersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long nextIndex = 1;
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                long num = Long.parseLong(child.getKey());
                                if (num >= nextIndex) {
                                    nextIndex = num + 1;
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    String newKey = String.valueOf(nextIndex);
                    DatabaseReference newOrderRef = nearOrdersRef.child(newKey);
                    newOrderRef.child("kidId").setValue(kidId);
                    newOrderRef.child("kidName").setValue(kidName);
                    newOrderRef.child("type").setValue("near")
                            .addOnSuccessListener(aVoid -> showToast("تمت إضافة الطلب"))
                            .addOnFailureListener(e -> showToast("فشل في إضافة الطلب"));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showToast("خطأ في قراءة الطلبات: " + error.getMessage());
                }
            });
        });
    }


//    private void appendLog(String text) {
//        handler.post(() -> {
//            logView.append(text + "\n\n");
//        });
//    }

    private void showToast(String text) {
        handler.post(() -> {
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
        });
    }

    private String getMyIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            return "UnknownIP";
        }
        return "UnknownIP";
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        serverRunning = false;
//    }

    private void addKidNameTextViewNotInClass(String kidName, String kidId) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 10, 10, 10);

        TextView textView = new TextView(this);
        textView.setLayoutParams(params);
        textView.setText(kidName);
        textView.setTextSize(20);
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(Color.LTGRAY);

        final float scale = getResources().getDisplayMetrics().density;
        int marginInPixels = (int) (10 * scale + 0.5f);
        params.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels);
        textView.setLayoutParams(params);

        // هنا هنضيف نفس فكرة التفاصيل
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKidDetails(kidId, linearLayoutNotInclass, textView);
            }
        });

        linearLayoutNotInclass.addView(textView);
        updateNotInClassCount();

    }

    private String calculateDuration(String checkIn) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
            Date inTime = sdf.parse(checkIn);

            String nowStr = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
                    .format(Calendar.getInstance().getTime());
            Date nowTime = sdf.parse(nowStr);

            long diff = nowTime.getTime() - inTime.getTime();
            long seconds = diff / 1000 % 60;
            long minutes = diff / (1000 * 60) % 60;
            long hours = diff / (1000 * 60 * 60);

            return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds);
        } catch (Exception e) {
            Log.e("duration-error", e.getMessage());
            return "-";
        }
    }

    private void addKidNameTextView(String kidName, String kidId) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 10, 10, 10); // القيم: (يمين, أعلى, يسار, أسفل)

        TextView textView = new TextView(this);
        textView.setLayoutParams(params);
        textView.setText(kidName);
        textView.setTextSize(20);
        textView.setTextColor(Color.BLACK);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setBackgroundColor(Color.YELLOW);

        final float scale = getResources().getDisplayMetrics().density;
        int marginInPixels = (int) (10 * scale + 0.5f);
        params.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels);

        // تطبيق الـ LayoutParams على TextView
        textView.setLayoutParams(params);

        // إضافة مستمع للضغط
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKidDetails(kidId, forTextViewOutOfSystem, textView);
            }
        });
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showCheckOutDialog(kidId, kidName);
                return true;
            }
        });


        // إضافة TextView إلى LinearLayout
        forTextViewOutOfSystem.addView(textView);
    }
    private EditText createTimeEditText() {
        EditText editText = new EditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setHint("--");
        editText.setGravity(Gravity.CENTER);
        return editText;
    }


    private void showCheckOutDialog(String kidId, String kidName) {
        Calendar today = Calendar.getInstance();
        String year = String.valueOf(today.get(Calendar.YEAR));
        String month = String.format(Locale.ENGLISH, "%02d", today.get(Calendar.MONTH) + 1);
        String todayDayStr = String.format(Locale.ENGLISH, "%02d", today.get(Calendar.DAY_OF_MONTH));

        DatabaseReference monthRef = FirebaseDatabase.getInstance().getReference()
                .child(mainCollection)
                .child("dateData")
                .child(year)
                .child(month);

        List<String> availableDays = new ArrayList<>();
        final int[] currentIndex = {0};

        monthRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    if (daySnapshot.hasChild(kidId)) {
                        availableDays.add(daySnapshot.getKey());
                    }
                }

                Collections.sort(availableDays);

                if (availableDays.isEmpty()) {
                    Toast.makeText(MainActivity.this, "لا يوجد أيام مسجلة لهذا الطالب", Toast.LENGTH_LONG).show();
                    return;
                }

                // تحديد أقرب يوم للنهاردة
                currentIndex[0] = 0;
                for (int i = 0; i < availableDays.size(); i++) {
                    if (availableDays.get(i).compareTo(todayDayStr) <= 0) {
                        currentIndex[0] = i;
                    }
                }

                // تصميم الديالوج
                LinearLayout dialogLayout = new LinearLayout(MainActivity.this);
                dialogLayout.setOrientation(LinearLayout.VERTICAL);
                dialogLayout.setPadding(30, 30, 30, 30);

                TextView studentNameView = new TextView(MainActivity.this);
                studentNameView.setText(kidName);
                studentNameView.setTextSize(20);
                studentNameView.setGravity(Gravity.CENTER);
                studentNameView.setPadding(0, 0, 0, 20);
                dialogLayout.addView(studentNameView);

                LinearLayout navLayout = new LinearLayout(MainActivity.this);
                navLayout.setOrientation(LinearLayout.HORIZONTAL);
                navLayout.setGravity(Gravity.CENTER);

                Button prevButton = new Button(MainActivity.this);
                prevButton.setText("السابق");

                Button nextButton = new Button(MainActivity.this);
                nextButton.setText("التالي");

                TextView dateTextView = new TextView(MainActivity.this);
                dateTextView.setTextSize(18);
                dateTextView.setPadding(20, 0, 20, 0);

                navLayout.addView(prevButton);
                navLayout.addView(dateTextView);
                navLayout.addView(nextButton);

                dialogLayout.addView(navLayout);

                // وقت الدخول
                TextView labelInTitle = new TextView(MainActivity.this);
                labelInTitle.setText("وقت الدخول:");
                labelInTitle.setPadding(0, 20, 0, 5);
                dialogLayout.addView(labelInTitle);

                TextView labelInHeader = new TextView(MainActivity.this);
                labelInHeader.setText("الساعة   الدقيقة");
                labelInHeader.setGravity(Gravity.CENTER);
                dialogLayout.addView(labelInHeader);

                LinearLayout checkInLayout = new LinearLayout(MainActivity.this);
                checkInLayout.setOrientation(LinearLayout.HORIZONTAL);
                checkInLayout.setGravity(Gravity.CENTER);
                EditText inHour = createTimeEditText();
                EditText inMinute = createTimeEditText();
                checkInLayout.addView(inHour);
                checkInLayout.addView(new TextView(MainActivity.this) {{ setText(":"); }});
                checkInLayout.addView(inMinute);
                dialogLayout.addView(checkInLayout);

                Spinner inAmPmSpinner = new Spinner(MainActivity.this);
                ArrayAdapter<String> amPmAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, new String[]{"AM", "PM"});
                amPmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                inAmPmSpinner.setAdapter(amPmAdapter);
                dialogLayout.addView(inAmPmSpinner);

                // وقت الخروج
                TextView labelOutTitle = new TextView(MainActivity.this);
                labelOutTitle.setText("وقت الخروج:");
                labelOutTitle.setPadding(0, 20, 0, 5);
                dialogLayout.addView(labelOutTitle);

                TextView labelOutHeader = new TextView(MainActivity.this);
                labelOutHeader.setText("الساعة   الدقيقة");
                labelOutHeader.setGravity(Gravity.CENTER);
                dialogLayout.addView(labelOutHeader);

                LinearLayout checkOutLayout = new LinearLayout(MainActivity.this);
                checkOutLayout.setOrientation(LinearLayout.HORIZONTAL);
                checkOutLayout.setGravity(Gravity.CENTER);
                EditText outHour = createTimeEditText();
                EditText outMinute = createTimeEditText();
                checkOutLayout.addView(outHour);
                checkOutLayout.addView(new TextView(MainActivity.this) {{ setText(":"); }});
                checkOutLayout.addView(outMinute);
                dialogLayout.addView(checkOutLayout);

                Spinner outAmPmSpinner = new Spinner(MainActivity.this);
                outAmPmSpinner.setAdapter(amPmAdapter);
                dialogLayout.addView(outAmPmSpinner);

                Button confirmButton = new Button(MainActivity.this);
                confirmButton.setText("تأكيد");
                dialogLayout.addView(confirmButton);

                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setView(dialogLayout)
                        .setCancelable(true)
                        .create();
                dialog.show();

                // تحميل اليوم الحالي
                Runnable loadDataForCurrentIndex = () -> {
                    String selectedDay = availableDays.get(currentIndex[0]);
                    dateTextView.setText(year + "-" + month + "-" + selectedDay);

                    DatabaseReference dayRef = FirebaseDatabase.getInstance().getReference()
                            .child(mainCollection)
                            .child("dateData")
                            .child(year)
                            .child(month)
                            .child(selectedDay)
                            .child(kidId);

                    dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String checkIn = snapshot.child("checkIn").getValue(String.class);
                            String checkOut = snapshot.child("checkOut").getValue(String.class);

                            if (checkIn != null && checkIn.split(":").length >= 2) {
                                String[] result = convert24To12(checkIn);
                                String[] timeParts = result[0].split(":");
                                inHour.setText(timeParts[0]);
                                inMinute.setText(timeParts[1]);
                                inAmPmSpinner.setSelection(result[1].equalsIgnoreCase("PM") ? 1 : 0);
                            } else {
                                inHour.setText(""); inMinute.setText("");
                                inAmPmSpinner.setSelection(0);
                            }

                            if (checkOut != null && checkOut.split(":").length >= 2) {
                                String[] result = convert24To12(checkOut);
                                String[] timeParts = result[0].split(":");
                                outHour.setText(timeParts[0]);
                                outMinute.setText(timeParts[1]);
                                outAmPmSpinner.setSelection(result[1].equalsIgnoreCase("PM") ? 1 : 0);
                            } else {
                                outHour.setText(""); outMinute.setText("");
                                outAmPmSpinner.setSelection(0);
                            }

                            prevButton.setEnabled(currentIndex[0] > 0);
                            nextButton.setEnabled(currentIndex[0] < availableDays.size() - 1);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(MainActivity.this, "خطأ في القراءة", Toast.LENGTH_SHORT).show();
                        }
                    });
                };

                loadDataForCurrentIndex.run();

                prevButton.setOnClickListener(v -> {
                    if (currentIndex[0] > 0) {
                        currentIndex[0]--;
                        loadDataForCurrentIndex.run();
                    }
                });

                nextButton.setOnClickListener(v -> {
                    if (currentIndex[0] < availableDays.size() - 1) {
                        currentIndex[0]++;
                        loadDataForCurrentIndex.run();
                    }
                });

                confirmButton.setOnClickListener(v -> {
                    String checkInTime = convert12To24(
                            inHour.getText().toString(),
                            inMinute.getText().toString(),
                            "00",
                            inAmPmSpinner.getSelectedItem().toString()
                    );

                    String checkOutTime = convert12To24(
                            outHour.getText().toString(),
                            outMinute.getText().toString(),
                            "00",
                            outAmPmSpinner.getSelectedItem().toString()
                    );

                    if (!isValidTimeFormat(checkInTime) || !isValidTimeFormat(checkOutTime)) {
                        Toast.makeText(MainActivity.this, "صيغة وقت خاطئة", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String selectedDay = availableDays.get(currentIndex[0]);
                    DatabaseReference kidRef = FirebaseDatabase.getInstance().getReference().child(mainCollection);

                    kidRef.child("dateData").child(year).child(month).child(selectedDay).child(kidId).child("checkIn").setValue(checkInTime);
                    kidRef.child("dateData").child(year).child(month).child(selectedDay).child(kidId).child("checkOut").setValue(checkOutTime);
                    kidRef.child("data").child(kidId).child("isInClass").setValue(false);

                    Toast.makeText(MainActivity.this, "تم الحفظ", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "حدث خطأ", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String[] convert24To12(String time24) {
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH);
            Date date = sdf24.parse(time24);
            String time12 = sdf12.format(date);
            String[] parts = time12.split(" ");
            return new String[]{parts[0], parts[1]}; // [0]=hh:mm:ss, [1]=AM/PM
        } catch (Exception e) {
            return new String[]{"", "AM"};
        }
    }

    private String convert12To24(String hourStr, String minuteStr, String secondStr, String amPm) {
        int hour = Integer.parseInt(hourStr);
        int minute = Integer.parseInt(minuteStr);
        int second = Integer.parseInt(secondStr);

        if (amPm.equalsIgnoreCase("PM") && hour < 12) {
            hour += 12;
        }
        if (amPm.equalsIgnoreCase("AM") && hour == 12) {
            hour = 0;
        }

        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hour, minute, second);
    }

    private void openManualCheckOutDialog(String kidId, String datePath, String checkInTime) {
        // أبسط شكل ممكن
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("وقت الدخول: " + checkInTime);

        // محتوى النافذة
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText editText = new EditText(this);
        editText.setHint("ادخل وقت الخروج 24h مثل 15:07:28");
        editText.setInputType(android.text.InputType.TYPE_CLASS_DATETIME);
        layout.addView(editText);

        builder.setView(layout);

        builder.setPositiveButton("تأكيد", (dialog, which) -> {
            String checkOutTime = editText.getText().toString().trim();

            if (isValidTimeFormat(checkOutTime)) {
                // اكتب في Firebase
                DatabaseReference kidRef = FirebaseDatabase.getInstance().getReference()
                        .child(mainCollection);

                // isInClass = false
                kidRef.child("data").child(kidId).child("isInClass").setValue(false);

                // checkOut
                kidRef.child("dateData").child(datePath).child("checkOut").setValue(checkOutTime);

                Toast.makeText(MainActivity.this, "تم التحديث", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "صيغة وقت غير صحيحة", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private boolean isValidTimeFormat(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
            sdf.setLenient(false);
            sdf.parse(time);
            return true;
        } catch (Exception e) {
            return false;
        }
    }



    private void showKidDetails(String kidId, LinearLayout layout, TextView textView) {
        // لو التفاصيل الحالية معروضة تحت نفس الtextView، امسحها وخلاص
        if (currentDetailsLayout != null) {
            int index = layout.indexOfChild(textView);
            int currentIndex = layout.indexOfChild(currentDetailsLayout);

            if (currentIndex == index + 1) {
                layout.removeView(currentDetailsLayout);
                currentDetailsLayout = null;
                return;
            } else {
                layout.removeView(currentDetailsLayout);
            }
        }

        // إنشاء LinearLayout جديد للتفاصيل
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        detailsLayout.setBackgroundColor(Color.GRAY);
        int heightInPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                250,
                getResources().getDisplayMetrics()
        );

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                heightInPx,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL;

        detailsLayout.setLayoutParams(params);

        // تحميل بيانات الطفل
        databaseReference.child(kidId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String key = childSnapshot.getKey();
                    Object value = childSnapshot.getValue();

                    if (value == null) continue;

                    if ("studentPhoto".equals(key)) {
                        DataSnapshot photoChild = childSnapshot.child("studentPhoto");
                        if (photoChild.exists() && photoChild.getValue() != null) {
                            String photoUrl = photoChild.getValue(String.class);

                            ImageView studentImageView = new ImageView(MainActivity.this);
                            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    400
                            );
                            studentImageView.setLayoutParams(imgParams);
                            studentImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            Glide.with(MainActivity.this)
                                    .load(photoUrl)
                                    .into(studentImageView);

                            detailsLayout.addView(studentImageView);
                        }
                    }
                    else {
                        // عرض باقي التفاصيل كنص
                        TextView detailTextView = new TextView(MainActivity.this);
                        detailTextView.setText(key + ": " + value.toString());
                        detailTextView.setTextSize(18);
                        detailTextView.setGravity(Gravity.START);
                        detailTextView.setPadding(10, 10, 10, 10);

                        if (isPhoneKey(key)) {
                            detailTextView.setTextColor(Color.BLUE);
                            detailTextView.setOnClickListener(v -> {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + value.toString()));
                                startActivity(intent);
                            });
                        }

                        detailsLayout.addView(detailTextView);
                    }
                }

                // إضافة detailsLayout تحت الـ TextView اللي ضغطت عليه
                int index = layout.indexOfChild(textView);
                layout.addView(detailsLayout, index + 1);
                currentDetailsLayout = detailsLayout;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Database error: " + databaseError.getMessage());
            }
        });
    }
    private boolean isPhoneKey(String key) {
        return "phoneFather".equals(key) || "phoneMother".equals(key) || "phone3".equals(key);
    }

    // كلاس مساعد لتخزين بيانات الأطفال
    private class Kid {
        private String kidName;
        private String kidId;

        public Kid(String kidName, String kidId) {
            this.kidName = kidName;
            this.kidId = kidId;
        }

        public String getKidName() {
            return kidName;
        }

        public String getKidId() {
            return kidId;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }




    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }
}
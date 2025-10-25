package com.example.f_puser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class offLine extends AppCompatActivity {
    private String mainCollection = "1";

    private String ssid = "1";
    private String password = "123456789";
    private int Port = 8888;
    private String broadcastAddress = "192.168.43.255";  // ثابت للهوتسبوت
    private String kidName;

    private String kidId;
    private TextView kidIdTextView;
    private TextView statusTextView;
    private Button sendButton;

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Main layout عمودي
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);

        // عنوان ثابت فوق
        TextView titleText = new TextView(this);
        titleText.setText("واجهة إستلام الطلبة من المؤسسة");
        titleText.setTextSize(20);
        titleText.setPadding(16, 16, 16, 16);
        titleText.setGravity(Gravity.CENTER);
        mainLayout.addView(titleText);

        // ScrollView اللي هيشيل الأزرار + التعليمات
        ScrollView scrollView = new ScrollView(this);
        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(16, 16, 16, 16);

        // جلب الطلاب من SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        int kidsCount = prefs.getInt("kidsCount", 0);

        if (kidsCount == 0) {
            TextView noStudents = new TextView(this);
            noStudents.setText("لا يوجد طلاب مسجلين");
            noStudents.setPadding(16, 16, 16, 16);
            noStudents.setGravity(Gravity.CENTER);
            scrollContent.addView(noStudents);
        } else {
            for (int i = 0; i < kidsCount; i++) {
                String kidName = prefs.getString("kidName_" + i, "طالب");
                String kidIdLocal = prefs.getString("kidId_" + i, "بدون كود");

                Button sendButton = new Button(this);
                sendButton.setText("طلب إستلام: " + kidName);
                sendButton.setPadding(16, 16, 16, 16);
                scrollContent.addView(sendButton);

                String finalKidId = kidIdLocal;
                String finalKidName = kidName;

                sendButton.setOnClickListener(v -> {
                    kidId = finalKidId;
                    this.kidName = finalKidName;

                    if (isInternetAvailable()) {
                        placeRemoteOrder(kidId, kidName);
                    } else {
                        connectAndSendKidId();
                    }
                });
            }
        }

        // التعليمات تحت الأزرار في نفس الـ ScrollView
        TextView rools = new TextView(this);
        rools.setText(
                "فيه طريقتين لاستلام الطالب عن بعد او عن قرب:\n\n" +
                        "1- من المنزل أو عن بُعد:\n" +
                        "- الطالب اللي بيعرف يروح لوحده لازم يكون معاه إذن خروج من ولي أمره.\n" +
                        "- لازم يكون فيه إنترنت يعني تشغيل الواي فاي أو بيانات الهاتف علشان تقدر ترسل أمر الخروج للمؤسسة.\n\n" +
                        "2- عند باب المؤسسة:\n" +
                        "- ولي الأمر لازم يكون واقف قدام الباب.\n" +
                        "- الأفضل إغلاق بيانات الهاتف.\n" +
                        "- يضغط زر \"استلام الطالب\".\n" +
                        "- ينتظر خروج الطالب.\n" +
                        "- الطلب من قرب يعتبر الأكثر أمان."
        );
        rools.setTextSize(17);
        rools.setPadding(16, 16, 16, 16);
        rools.setGravity(Gravity.START);
        scrollContent.addView(rools);

        // ضيف الكونتنت جوا الـ Scroll
        scrollView.addView(scrollContent);

        // ضيف الـ ScrollView في الـ Main
        mainLayout.addView(scrollView);

        // اعرض الواجهة
        setContentView(mainLayout);
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
    private void placeRemoteOrder(String kidId, String kidName) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("orders");

        ordersRef.orderByChild("kidId").equalTo(kidId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(offLine.this, "الطلب موجود بالفعل. لا يمكنك تكراره.", Toast.LENGTH_SHORT).show();
                } else {
                    // الطلب مش موجود - ضيفه جديد
                    ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            long lastNumber = 0;
                            for (DataSnapshot child : snapshot.getChildren()) {
                                try {
                                    long keyNumber = Long.parseLong(child.getKey());
                                    if (keyNumber > lastNumber) {
                                        lastNumber = keyNumber;
                                    }
                                } catch (NumberFormatException e) {
                                    // تجاهل
                                }
                            }

                            long newKey = lastNumber + 1;

                            DatabaseReference newOrderRef = ordersRef.child(String.valueOf(newKey));
                            newOrderRef.child("kidId").setValue(kidId);
                            newOrderRef.child("type").setValue("remote");
                            newOrderRef.child("kidName").setValue(kidName)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(offLine.this, "تم إرسال الطلب للمؤسسة بنجاح", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(offLine.this, "فشل في إرسال الطلب للمؤسسة", Toast.LENGTH_SHORT).show();
                                    });

                            DatabaseReference deliveryAlarmRef = FirebaseDatabase.getInstance().getReference(mainCollection).child("deliveryAlarm");
                            deliveryAlarmRef.setValue(true);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(offLine.this, "خطأ في قراءة البيانات من Firebase", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(offLine.this, "خطأ في قراءة البيانات من Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectAndSendKidId() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "شغل الواي فاي الأول", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return;
        }

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

                    handler.post(() -> {
//                        statusTextView.setText("تم الاتصال بالـ Hotspot. جاري الإرسال...");
                        sendUdpMessage();
                    });
                }

                @Override
                public void onUnavailable() {

                }
            });
        } else {
            Toast.makeText(this, "إصدار الأندرويد لا يدعم الاتصال التلقائي", Toast.LENGTH_SHORT).show();
        }
    }
    private void sendUdpMessage() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                String message = kidId + "|" + kidName;
                byte[] data = message.getBytes();

                InetAddress address = InetAddress.getByName(broadcastAddress);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, Port);
                socket.send(packet);
                socket.close();

                handler.post(() ->
                        Toast.makeText(this, "تم إرسال الطلب لمسؤول تسليم المؤسسة يرجي انتظار خروج الطالب", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                handler.post(() ->
                        Toast.makeText(this, "خطأ في الإرسال", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }


}

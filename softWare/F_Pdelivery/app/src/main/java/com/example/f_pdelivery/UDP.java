package com.example.f_pdelivery;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class UDP extends AppCompatActivity {

    private TextView logView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean serverRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // واجهة بسيطة ديناميكية
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        logView = new TextView(this);
        logView.setTextSize(16);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(logView);
        layout.addView(scroll);

        setContentView(layout);

        // شغل السيرفر
        startUdpServer();
    }

    private void startUdpServer() {
        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(8888);
                appendLog("UDP Server started on port 8888");

                byte[] buffer = new byte[1024];

                while (serverRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    String sender = packet.getAddress().toString();

                    appendLog("Received: " + message + " from " + sender);
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
                        appendLog("Sent IP: " + myIp + " to " + sender);
                    }
                }

            } catch (IOException e) {
                appendLog("Error: " + e.getMessage());
                showToast("خطأ في السيرفر: " + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }).start();
    }

    private void appendLog(String text) {
        handler.post(() -> {
            logView.append(text + "\n\n");
        });
    }

    private void showToast(String text) {
        handler.post(() -> {
            Toast.makeText(UDP.this, text, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serverRunning = false;
    }
}
